package sami.engine;

import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import com.perc.mitpas.adi.mission.planning.task.Task;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.event.AbortMission;
import sami.event.AbortMissionReceived;
import sami.event.BlockingInputEvent;
import sami.event.GeneratedEventListenerInt;
import sami.event.InputEvent;
import sami.event.MissingParamsReceived;
import sami.event.MissingParamsRequest;
import sami.event.OutputEvent;
import sami.event.ReflectedEventSpecification;
import sami.gui.GuiConfig;
import sami.handler.EventHandlerInt;
import sami.mission.Edge;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Token;
import sami.mission.TokenSpecification;
import sami.mission.TokenSpecification.TokenType;
import sami.mission.Transition;
import sami.mission.Vertex;
import sami.mission.Vertex.FunctionMode;
import sami.proxy.ProxyInt;

/**
 *
 * A PlanManager looks after a single MissionModel Basically the code here is
 * responsible for executing a PetriNet Mostly it is now only capable of
 * executing a State Machine
 *
 * @todo Do something when PlanManager ends, including informing super-mission
 *
 * @author pscerri
 */
public class PlanManager implements GeneratedEventListenerInt, PlanManagerListenerInt {

    private static final Logger LOGGER = Logger.getLogger(PlanManager.class.getName());
    // Variable for counting how many things the operator still needs to fill in before execution
    int repliesExpected = 0;
    // Blocking queue of generated input events waiting to be matched to a parameter input event
    private ArrayBlockingQueue<InputEvent> generatorEventQueue = new ArrayBlockingQueue<InputEvent>(20);
    ArrayList<InputEvent> activeInputEvents = new ArrayList<InputEvent>();
    ArrayList<Place> placesBeingEntered = new ArrayList<Place>();
    // List of tokens on the mission spec's edges to be used as the default list of tokens to put in the starting place
    //  Null PROXY and ALL tokens are not added to this list
    private ArrayList<Token> defaultStartTokens = new ArrayList<Token>();
    // Keeps track of variables coming in from InputEvents, to be used in OutputEvents
    private HashMap<String, Object> variableNameToValue = new HashMap<String, Object>();
    // Lookup table used during processing of generated and updated parameter input events
    private HashMap<InputEvent, Transition> inputEventToTransitionMap = new HashMap<InputEvent, Transition>();
    // Lookup table used when submissions are completed
    private HashMap<PlanManager, Place> planManagerToPlace = new HashMap<PlanManager, Place>();
    private HashMap<InputEvent, HashMap<ProxyInt, InputEvent>> clonedIeTable = new HashMap<InputEvent, HashMap<ProxyInt, InputEvent>>();
    final MissionPlanSpecification mSpec;
    // The model being managed by this PlanManager
    private Place startPlace;
    private String planName;
    public final UUID missionId;

    public PlanManager(final MissionPlanSpecification mSpec, UUID missionId, String planName) {
        LOGGER.log(Level.INFO, "Creating PlanManager for mSpec " + mSpec + " with mission ID " + missionId + " and planName " + planName);
        this.mSpec = mSpec;
        this.missionId = missionId;
        this.planName = planName;

//        mSpec.printGraph();
        startPlace = mSpec.getUninstantiatedStart();

        // If there are any parameters on the events that need to be filled in, request from the operator
        ArrayList<ReflectedEventSpecification> editableEventSpecs = mSpec.getEventSpecsRequestingParams();
        if (editableEventSpecs.size() > 0) {
            LOGGER.fine("Missing/editable parameters in eventSpecs: " + editableEventSpecs);

            // Create vertices to get missing parameters
            Place missingParamsPlace = new Place("Get Params", FunctionMode.Nominal);
            Transition missingParamsTransition = new Transition("Got Params", FunctionMode.Nominal);
            Edge edge1 = new Edge(missingParamsPlace, missingParamsTransition, FunctionMode.Nominal);
            Edge edge2 = new Edge(missingParamsTransition, startPlace, FunctionMode.Nominal);
            // Add vetices to plan
            missingParamsPlace.addOutTransition(missingParamsTransition);
            missingParamsTransition.addInPlace(missingParamsPlace);
            missingParamsTransition.addOutPlace(startPlace);
            edge1.addTokenRequirement(Engine.getInstance().getNoReqToken());
            edge2.addTokenRequirement(Engine.getInstance().getTakeAllToken());
            missingParamsPlace.addOutEdge(edge1);
            missingParamsTransition.addInEdge(edge1);
            missingParamsTransition.addOutEdge(edge2);
            startPlace.addInEdge(edge2);
            startPlace = missingParamsPlace;

            // Make list of missing/editable parameter fields
            Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> eventSpecToFieldDescriptions = new Hashtable<ReflectedEventSpecification, Hashtable<Field, String>>();
            Hashtable<ReflectedEventSpecification, ArrayList<Field>> eventSpecToFields = new Hashtable<ReflectedEventSpecification, ArrayList<Field>>();
            for (ReflectedEventSpecification eventSpec : editableEventSpecs) {
                LOGGER.fine("Event spec for " + eventSpec.getClassName() + " has missing/editable fields");
                // Hashtable entries for this eventSpec
                Hashtable<Field, String> fieldDescriptions = new Hashtable<Field, String>();
                ArrayList<Field> fields = new ArrayList<Field>();
                eventSpecToFieldDescriptions.put(eventSpec, fieldDescriptions);
                eventSpecToFields.put(eventSpec, fields);
                // Defined but editable values
                HashMap<String, Object> instanceParams = eventSpec.getFieldDefinitions();
                HashMap<String, Boolean> fieldNameToEditable = eventSpec.getEditableFields();

                int missingCount = 0, editableCount = 0;
                for (String fieldName : instanceParams.keySet()) {
                    LOGGER.fine("\tField: " + fieldName + " = " + instanceParams.get(fieldName));
                    if (instanceParams.get(fieldName) == null) {
                        LOGGER.finer("\t\t Missing");
                        missingCount++;
                        try {
                            Field missingField = Class.forName(eventSpec.getClassName()).getField(fieldName);
                            fieldDescriptions.put(missingField, "");
                            fields.add(missingField);
                        } catch (ClassNotFoundException cnfe) {
                            cnfe.printStackTrace();
                        } catch (NoSuchFieldException nsfe) {
                            nsfe.printStackTrace();
                        }
                    } else if (fieldNameToEditable.get(fieldName)) {
                        LOGGER.finer("\t\t Editable");
                        editableCount++;
                        try {
                            Field editableField = Class.forName(eventSpec.getClassName()).getField(fieldName);
                            fieldDescriptions.put(editableField, "");
                            fields.add(editableField);
                        } catch (ClassNotFoundException cnfe) {
                            cnfe.printStackTrace();
                        } catch (NoSuchFieldException nsfe) {
                            nsfe.printStackTrace();
                        }
                    } else {
                        LOGGER.finer("\t\t Locked");
                    }

                    if (instanceParams.get(fieldName) == null
                            && !fieldNameToEditable.get(fieldName)) {
                        LOGGER.severe("Have a non-editable field: " + fieldName + " with no value!");
                    }
                }
                LOGGER.fine("\t Have " + missingCount + " missing fields");
                LOGGER.fine("\t Have " + editableCount + " editable fields");
            }

            // Create events to get missing parameters
            //@todo Modify constructors?
            MissingParamsRequest request = new MissingParamsRequest(missionId, eventSpecToFieldDescriptions);
            MissingParamsReceived response = new MissingParamsReceived();
            request.setMissionId(missionId);
            response.setMissionId(missionId);
            response.setRelevantOutputEventId(request.getId());
            missingParamsPlace.addOutputEvent(request);
            missingParamsTransition.addInputEvent(response);

            // Add abort mission handling
            // Add transition
            Transition abortTransition = new Transition("AbortMission", FunctionMode.Recovery);
            AbortMissionReceived abortReceived = new AbortMissionReceived(missionId);
            abortTransition.addInputEvent(abortReceived);
            // Add end place with AbortMission
            Place abortPlace = new Place("AbortMission", FunctionMode.Recovery);
            abortPlace.setIsEnd(true);
            AbortMission abortMission = new AbortMission(missionId);
            abortPlace.addOutputEvent(abortMission);
            // Add edges
            Edge abortInEdge = new Edge(missingParamsPlace, abortTransition, FunctionMode.Recovery);
            abortInEdge.addTokenRequirement(Engine.getInstance().getNoReqToken());
            abortTransition.addInEdge(abortInEdge);
            missingParamsPlace.addOutEdge(abortInEdge);
            abortTransition.addInPlace(missingParamsPlace);
            missingParamsPlace.addOutTransition(abortTransition);
            Edge abortOutEdge = new Edge(abortTransition, abortPlace, FunctionMode.Recovery);
            abortOutEdge.addTokenRequirement(Engine.getInstance().getTakeAllToken());
            abortTransition.addOutEdge(abortOutEdge);
            abortPlace.addInEdge(abortOutEdge);
            abortTransition.addOutPlace(abortPlace);
            abortPlace.addInTransition(abortTransition);
        } else {
            LOGGER.log(Level.INFO, "No missing params, instantiating plan");
            if (!mSpec.isInstantiated()) {
                mSpec.instantiate(missionId);
            }
        }

        // Load TokenSpecifications from the spec and put the corresponding Token in the appropriate edges
        //@todo Should this be in plan.getInstantiatedStart() instead ?
        LOGGER.log(Level.INFO, "Loading tokens from edge token specifications");
        Token token;
        Collection<Edge> edges = mSpec.getGraph().getEdges();
        Map<Edge, ArrayList<TokenSpecification>> edgeToTokenSpecs = mSpec.getEdgeToTokenSpecListMap();
        for (Edge edge : edges) {
            LOGGER.log(Level.FINE, "\tFor edge " + edge);
            ArrayList<TokenSpecification> tokenSpecs = edgeToTokenSpecs.get(edge);
            if (tokenSpecs == null) {
                LOGGER.log(Level.WARNING, "\t\tNo labeled token specs for edge " + edge);
            } else {
                for (TokenSpecification tokenSpec : tokenSpecs) {
                    LOGGER.log(Level.FINE, "\t\tFor tokenSpec " + tokenSpec);

                    // Retreive or create token for the token specification
                    token = Engine.getInstance().getToken(tokenSpec);
                    if (token != null) {
                        LOGGER.log(Level.FINE, "\t\t\tRetrieved token " + token);
                        if (!defaultStartTokens.contains(token) && (token.getType() == TokenType.MatchGeneric || token.getType() == TokenType.Task)) {
                            // Update default list of tokens to add to the starting place
                            defaultStartTokens.add(token);
                        }
                    } else {
                        token = Engine.getInstance().createToken(tokenSpec);
                        LOGGER.log(Level.FINE, "\t\t\tCreated token " + token);
                    }
                    // Add the token to the edge requirements
                    if (token != null) {
                        edge.addTokenRequirement(token);
                    } else {
                        LOGGER.log(Level.SEVERE, "\t\t\tTried to add a null token for edge " + edge + " token specification requirement " + tokenSpec);
                    }
                }
            }
        }
        generatedEventThread.start();
    }

    public PlanManager(final MissionPlanSpecification plan, UUID missionId) {
        this(plan, missionId, plan.getName());
    }
    Thread generatedEventThread = new Thread() {
        public void run() {
            while (true) {
                try {
                    InputEvent generatedEvent = generatorEventQueue.take();
                    processGeneratedEvent(generatedEvent);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * Begin the execution of the plan
     *
     * Either wait for a proxy token to be added, or use a "mission token",
     * i.e., one that is not associated with any proxy
     *
     * @param plan
     */
    public void start(ArrayList<Token> startingTokens) {
        LOGGER.log(Level.INFO, "Begin plan start");
        while (!generatedEventThread.isAlive()) {
            LOGGER.log(Level.WARNING, "generatedEventThread is not alive, sleeping for 1s");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(PlanManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        boolean checkedForTransition = false;
        // Add in any starting tokens to the start place
        if (startingTokens != null) {
            LOGGER.log(Level.INFO, "\tAdding received token list " + startingTokens + " to start place " + startPlace);
            enterPlace(startPlace, startingTokens, true);
            checkedForTransition = true;
        } else {
            LOGGER.log(Level.INFO, "\tNo token list received for start place " + startPlace + ", using default list " + defaultStartTokens);
            enterPlace(startPlace, defaultStartTokens, true);
            checkedForTransition = true;
        }

        if (!checkedForTransition) {
            for (Transition transition : startPlace.getOutTransitions()) {
                HashMap<Place, ArrayList<Token>> tokensToRemove = checkTransition(transition);
                if (tokensToRemove != null) {
                    executeTransition(transition, tokensToRemove);
                };
            }
        }

        LOGGER.log(Level.INFO, "Plan has finished starting");
        Engine.getInstance().started(this);
    }

    private synchronized HashMap<Place, ArrayList<Token>> checkTransition(Transition transition) {
        LOGGER.log(Level.INFO, "Checking " + transition);
        String debug = "input event fulfillment is currently:";
        for (InputEvent ie : transition.getInputEvents()) {
            debug += "\n\t" + ie + ": " + transition.getInputEventStatus(ie);
        }

        boolean failure = false;
        HashMap<Place, ArrayList<Token>> placeToMatchedTokens = new HashMap<Place, ArrayList<Token>>();
        HashMap<Place, ArrayList<Token>> placeToRemovedTokens = new HashMap<Place, ArrayList<Token>>();

        ////
        // Check that each InputEvent has occurred
        ////
        ArrayList<InputEvent> inputEvents = transition.getInputEvents();
        LOGGER.log(Level.FINE, "\tChecking for input events: " + inputEvents);
        for (InputEvent ie : inputEvents) {
            if (!transition.getInputEventStatus(ie)) {
                LOGGER.log(Level.INFO, "\t\tInput event " + ie + " is not ready");
                failure = true;
                break;
            } else {
                LOGGER.log(Level.FINE, "\t\tInput event " + ie + " is ready");
            }
        }

        if (!failure) {
            ////
            // Check the token requirements of each incoming edge
            ////
            check:
            for (Edge edge : transition.getInEdges()) {
                // Get incoming Place
                Vertex vertex = edge.getStart();
                if (!(vertex instanceof Place)) {
                    LOGGER.log(Level.SEVERE, "\tPreceding Vertex " + vertex + " is not a Place!");
                    System.exit(0);
                }
                Place place = (Place) vertex;

                ////
                // Check that if there is a sub-mission that it has completed
                ////
                if (place.getSubMission() != null && !place.getSubMissionComplete()) {
                    LOGGER.log(Level.INFO, "\tSub-mission " + place.getSubMission() + " is not yet complete");
                    failure = true;
                    break check;
                } else if (place.getSubMission() != null && place.getSubMissionComplete()) {
                    LOGGER.log(Level.FINE, "\tSub-mission " + place.getSubMission() + " is complete");
                }

                ////
                // Check edge requirements
                ////
                ArrayList<Token> matchedTokens = new ArrayList<Token>();
                ArrayList<Token> placeTokens = (ArrayList<Token>) place.getTokens();
                ArrayList<Token> removedTokens = new ArrayList<Token>();
                placeToMatchedTokens.put(place, matchedTokens);
                placeToRemovedTokens.put(place, removedTokens);
                LOGGER.log(Level.FINE, "\tChecking incoming " + edge + " with reqs [" + edge.getTokenRequirements() + "] against " + place + " with [" + placeTokens + "]");
                LOGGER.log(Level.FINE, "Engine's tokens are:\n" + Engine.getInstance().getAllTokens().toString());
                for (Token edgeToken : edge.getTokenRequirements()) {
                    // NoRequirement, RelevantProxy, Generic, Task
                    LOGGER.log(Level.FINE, "Handling edge token requirement: " + edgeToken + " and place with tokens: " + placeTokens);
                    switch (edgeToken.getType()) {
                        case MatchNoReq:
                            if (!matchedTokens.isEmpty()) {
                                LOGGER.severe("Edge has \"No Req\" label AND a token requirement!");
                                matchedTokens.clear();
                            }
                            break;
                        case HasProxy:
                            boolean foundProxy = false;
                            for (Token placeToken : placeTokens) {
                                LOGGER.log(Level.FINE, "\t\t\t !!!Checking if token " + placeToken + " matches any unmatched relevant proxies");
                                if (placeToken.getProxy() != null) {
                                    LOGGER.log(Level.FINE, "\t\t\t !!!Token " + placeToken + " with proxy " + placeToken.getProxy() + ", satifies HasProxy label");
                                    foundProxy = true;
                                    break;
                                }
                            }
                            if(!foundProxy) {
                                    LOGGER.log(Level.FINE, "\t\t\t !!!Failed to find proxy for has proxy token");
                            }
                            failure = !foundProxy;
                            break;
                        case MatchRelevantProxy:
                            for (InputEvent ie : transition.getInputEvents()) {
                                if (ie.getGeneratorEvent() == null) {
                                    LOGGER.log(Level.FINE, "\tie: " + ie + "\tgeneratorEvent is null: should be a null RP blocking IE: param IE RP = " + ie.getRelevantProxyList());
                                    continue;
                                }
                                if (ie.getGeneratorEvent().getRelevantProxyList() == null) {
                                    LOGGER.log(Level.FINE, "\tie: " + ie + "\trelevantProxyList is null");
                                    continue;
                                }
                                LOGGER.log(Level.FINE, "\tie: " + ie + "\trelevantProxyList: " + ie.getGeneratorEvent().getRelevantProxyList());
                                for (ProxyInt proxy : ie.getGeneratorEvent().getRelevantProxyList()) {
                                    boolean relevantProxyMatched = false;
                                    for (Token placeToken : placeTokens) {
                                        LOGGER.log(Level.FINE, "\t\t\tChecking if token " + placeToken + " matches any unmatched relevant proxies");
                                        if (placeToken.getProxy() == proxy) {
                                            LOGGER.log(Level.FINE, "\t\t\tUsing place token " + placeToken + " for input event/edge proxy token");
                                            matchedTokens.add(placeToken);
                                            relevantProxyMatched = true;
                                            break;
                                        }
                                    }
                                    if (!relevantProxyMatched) {
                                        LOGGER.log(Level.FINE, "\t\tFailed to find token in place matching IE relevant proxy " + proxy);
                                        failure = true;
                                        break check;
                                    }
                                }
                            }
                            break;
                        case MatchGeneric:
                            failure = !placeTokens.remove(edgeToken);
                            if (!failure) {
                                matchedTokens.add(edgeToken);
                                removedTokens.add(edgeToken);
                            } else {
                                LOGGER.fine("\t Failed G check");
                            }
                            break;
                        case Task:
                            failure = !placeTokens.remove(edgeToken);
                            if (!failure) {
                                matchedTokens.add(edgeToken);
                                removedTokens.add(edgeToken);
                            } else {
                                LOGGER.fine("\t Failed Task check");
                            }
                            break;
                        default:
                            LOGGER.severe("Edge has unexpected token requirements: " + edgeToken);
                            failure = true;
                            break;
                    }
                    if (failure) {
                        LOGGER.log(Level.FINE, "\tFailed check");
                        break check;
                    }
                }
                if (failure) {
                    LOGGER.severe("Logic error - I should not be here");
                }
                LOGGER.log(Level.FINE, "\tEdge requirements have been met");
            }
        }

        // Return all removed tokens
        for (Place place : placeToRemovedTokens.keySet()) {
            place.getTokens().addAll(placeToRemovedTokens.get(place));
        }

        if (!failure) {
            LOGGER.log(Level.FINE, "\tAll event requirements have been met");
            LOGGER.log(Level.FINE, "\tTransition " + transition + " is ready!");
            return placeToMatchedTokens;
        }
        return null;
    }

    private synchronized void executeTransition(Transition transition, HashMap<Place, ArrayList<Token>> matchedTokens) {
        LOGGER.log(Level.INFO, "Executing " + transition + ", have matched tokens " + matchedTokens + ", inPlaces: " + transition.getInPlaces() + ", inEdges: " + transition.getInEdges() + ", outPlaces: " + transition.getOutPlaces() + ", outEdges: " + transition.getOutEdges());

        synchronized (placesBeingEntered) {
            for (Place place : transition.getInPlaces()) {
                if (placesBeingEntered.contains(place)) {
                    LOGGER.log(Level.INFO, "\tAborting executeTransition becaues an incoming place is still being entered: " + place);
                    return;
                }
            }
        }

        try {
            Thread.sleep(GuiConfig.TRANSITION_DELAY);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        // Get lists of various token groups ready ahead of time
        ArrayList<ProxyInt> relevantProxies = new ArrayList<ProxyInt>();
        for (InputEvent ie : transition.getInputEvents()) {
            if (ie.getGeneratorEvent() == null) {
                LOGGER.log(Level.FINE, "\tie: " + ie + "\tgeneratorEvent is null: should be a null RP blocking IE: param IE RP = " + ie.getRelevantProxyList());
            }
            if (ie.getGeneratorEvent() != null && ie.getGeneratorEvent().getRelevantProxyList() != null) {
                LOGGER.log(Level.FINE, "\tie: " + ie + "\tgeneratorEvent is not null and has RP: " + ie.getGeneratorEvent().getRelevantProxyList());
                relevantProxies.addAll(ie.getGeneratorEvent().getRelevantProxyList());
            }
        }

        boolean proxyTokensFound = true;
        ArrayList<Token> relevantProxyTokens = new ArrayList<Token>();
        if (!relevantProxies.isEmpty()) {
            for (Place inPlace : transition.getInPlaces()) {
                boolean tokenMatchFound = false;
                for (Token token : inPlace.getTokens()) {
                    if (relevantProxies.contains(token.getProxy())) {
                        relevantProxyTokens.add(token);
                        tokenMatchFound = true;
                    }
                }
                if (!tokenMatchFound) {
                    proxyTokensFound = false;
                }
            }
        }
        boolean taskTokensFound = true;
        ArrayList<Token> relevantTaskTokens = new ArrayList<Token>();
        if (!relevantProxies.isEmpty()) {
            for (Place inPlace : transition.getInPlaces()) {
                boolean tokenMatchFound = false;
                for (Token token : inPlace.getTokens()) {
                    if (relevantProxies.contains(token.getProxy())
                            && token.getType() == TokenType.Task) {
                        relevantTaskTokens.add(token);
                        tokenMatchFound = true;
                    }
                }
                if (!tokenMatchFound) {
                    taskTokensFound = false;
                }
            }
        }
        ArrayList<Token> allTokens = new ArrayList<Token>();
        for (Place inPlace : transition.getInPlaces()) {
            for (Token token : inPlace.getTokens()) {
                allTokens.add(token);
            }
        }
        ArrayList<Token> allTaskTokens = new ArrayList<Token>();
        for (Place inPlace : transition.getInPlaces()) {
            for (Token token : inPlace.getTokens()) {
                if (token.getType() == TokenType.Task) {
                    allTaskTokens.add(token);
                }
            }
        }
        ArrayList<Token> allProxyTokens = new ArrayList<Token>();
        for (Place inPlace : transition.getInPlaces()) {
            for (Token token : inPlace.getTokens()) {
                if (token.getType() == TokenType.Proxy) {
                    allProxyTokens.add(token);
                }
            }
        }

        // Explicity named task tokens
        HashMap<Place, ArrayList<Token>> inPlaceToTaskTokensToRemove = new HashMap<Place, ArrayList<Token>>();
        HashMap<Place, ArrayList<Token>> outPlaceToTaskTokensToAdd = new HashMap<Place, ArrayList<Token>>();
        // Number of generic tokens to add/remove
        HashMap<Place, Integer> inPlaceToGenericToRemove = new HashMap<Place, Integer>();
        HashMap<Place, Integer> outPlaceToGenericToAdd = new HashMap<Place, Integer>();
        // boolean[5] corrensponds to add/remove [ All, RelevantTasks, RelevantProxies, Tasks, Proxies ]
        HashMap<Place, boolean[]> inPlaceToListsToRemove = new HashMap<Place, boolean[]>();
        HashMap<Place, boolean[]> outPlaceToListsToAdd = new HashMap<Place, boolean[]>();

        for (Place inPlace : transition.getInPlaces()) {
            inPlaceToTaskTokensToRemove.put(inPlace, new ArrayList<Token>());
            inPlaceToGenericToRemove.put(inPlace, 0);
            inPlaceToListsToRemove.put(inPlace, new boolean[]{false, false, false, false, false});
        }
        for (Place outPlace : transition.getOutPlaces()) {
            outPlaceToTaskTokensToAdd.put(outPlace, new ArrayList<Token>());
            outPlaceToGenericToAdd.put(outPlace, 0);
            outPlaceToListsToAdd.put(outPlace, new boolean[]{false, false, false, false, false});
        }

        ////
        // Figure out what to add and remove
        ////
        for (Edge outEdge : transition.getOutEdges()) {
            // Get outgoing Place
            Vertex vertex = outEdge.getEnd();
            if (!(vertex instanceof Place)) {
                LOGGER.log(Level.SEVERE, "\tPreceding Vertex " + vertex + " is not a Place!");
                System.exit(0);
            }
            Place place = (Place) vertex;

            ArrayList<Token> taskTokensToAdd = outPlaceToTaskTokensToAdd.get(place);
            Integer genericToAdd = outPlaceToGenericToAdd.get(place);
            // boolean[3] corrensponds to add/remove [ All, RelevantTasks, RelevantProxies ]
            boolean[] listsToAdd = outPlaceToListsToAdd.get(place);

            token:
            for (Token edgeToken : outEdge.getTokenRequirements()) {
                switch (edgeToken.getType()) {
                    case TakeAll:
                        listsToAdd[0] = true;
                        for (Place inPlace : transition.getInPlaces()) {
                            boolean[] listsToRemove = inPlaceToListsToRemove.get(inPlace);
                            listsToRemove[0] = true;
                        }
                        break;
                    case CopyRelevantTask:
                        listsToAdd[1] = true;
                        break;
                    case CopyRelevantProxy:
                        listsToAdd[2] = true;
                        break;
                    case TakeRelevantTask:
                        listsToAdd[1] = true;
                        for (Place inPlace : transition.getInPlaces()) {
                            boolean[] listsToRemove = inPlaceToListsToRemove.get(inPlace);
                            listsToRemove[1] = true;
                        }
                        break;
                    case TakeRelevantProxy:
                        listsToAdd[2] = true;
                        for (Place inPlace : transition.getInPlaces()) {
                            boolean[] listsToRemove = inPlaceToListsToRemove.get(inPlace);
                            listsToRemove[2] = true;
                        }
                        break;
                    case TakeTask:
                        listsToAdd[3] = true;
                        for (Place inPlace : transition.getInPlaces()) {
                            boolean[] listsToRemove = inPlaceToListsToRemove.get(inPlace);
                            listsToRemove[3] = true;
                        }
                        break;
                    case TakeProxy:
                        listsToAdd[4] = true;
                        for (Place inPlace : transition.getInPlaces()) {
                            boolean[] listsToRemove = inPlaceToListsToRemove.get(inPlace);
                            listsToRemove[4] = true;
                        }
                        break;
                    case TakeGeneric:
                        genericToAdd++;
                        for (Place inPlace : transition.getInPlaces()) {
                            Integer genericCount = inPlaceToGenericToRemove.get(inPlace);
                            inPlaceToGenericToRemove.put(inPlace, genericCount + 1);
                        }
                        break;
                    case AddGeneric:
                        genericToAdd++;
                        break;
                    case ConsumeRelevantTask:
                        for (Place inPlace : transition.getInPlaces()) {
                            boolean[] listsToRemove = inPlaceToListsToRemove.get(inPlace);
                            listsToRemove[1] = true;
                        }
                        break;
                    case ConsumeRelevantProxy:
                        for (Place inPlace : transition.getInPlaces()) {
                            boolean[] listsToRemove = inPlaceToListsToRemove.get(inPlace);
                            listsToRemove[2] = true;
                        }
                        break;
                    case ConsumeGeneric:
                        for (Place inPlace : transition.getInPlaces()) {
                            Integer genericCount = inPlaceToGenericToRemove.get(inPlace);
                            inPlaceToGenericToRemove.put(inPlace, genericCount + 1);
                        }
                        break;
                    case Task:
                        // Add the token to the outgoing place
                        taskTokensToAdd.add(edgeToken);
                        // Look at the incoming edges - for those that also have this task token marked, 
                        //  remove the task token from the connected place
                        for (Edge inEdge : transition.getInEdges()) {
                            ArrayList<Token> tokensToRemove = inPlaceToTaskTokensToRemove.get((Place) inEdge.getStart());
                            if (inEdge.getTokenRequirements().contains(edgeToken)) {
                                tokensToRemove.add(edgeToken);
                            }
                        }
                        break;
                    case TakeNone:
                        break token;
                    default:
                        LOGGER.severe("Edge has unexpected token requirements: " + edgeToken);
                        break;
                }
            }
            // Need to update this hashtable entry
            outPlaceToGenericToAdd.put(place, genericToAdd);
        }

        ////
        // Remove things
        ////
        // If we have a take all, just remove everything and we're done
        //  else we have to check for intersections...no double counting
        for (Place inPlace : transition.getInPlaces()) {
            // boolean[3] corrensponds to add/remove [ All, RelevantTasks, RelevantProxies ]
            boolean[] listsToRemove = inPlaceToListsToRemove.get(inPlace);
            LOGGER.log(Level.FINE, "ListsToRemove for Place: " + inPlace + " leading to Transition: " + transition + "\n" + listsToRemove[0] + "\t" + listsToRemove[1] + "\t" + listsToRemove[2]);
            ArrayList<Token> taskTokensToRemove = inPlaceToTaskTokensToRemove.get(inPlace);
            LOGGER.log(Level.FINE, "TaskTokensToRemove for Place: " + inPlace + " leading to Transition: " + transition + "\n" + taskTokensToRemove);
            Integer genericCount = inPlaceToGenericToRemove.get(inPlace);
            ArrayList<Token> tokensToRemove = new ArrayList<Token>();

            if (listsToRemove[0]) {
                // Remove all and finish
                LOGGER.log(Level.FINER, "\tRemove all");
                tokensToRemove.addAll(inPlace.getTokens());
            } else {
                if (listsToRemove[1]) {
                    // Remove relevant task tokens
                    for (Token token : relevantTaskTokens) {
                        if (!tokensToRemove.contains(token)) {
                            LOGGER.log(Level.FINER, "\tRemove RT token: " + token);
                            tokensToRemove.add(token);
                        }
                    }
                }
                if (listsToRemove[2]) {
                    // Remove relevant proxy tokens
                    for (Token token : relevantProxyTokens) {
                        if (!tokensToRemove.contains(token)) {
                            LOGGER.log(Level.FINER, "\tRemove RP token: " + token);
                            tokensToRemove.add(token);
                        }
                    }
                }
                if (listsToRemove[3]) {
                    // Remove all task tokens
                    for (Token token : inPlace.getTokens()) {
                        if (token.getType() == TokenType.Task && !tokensToRemove.contains(token)) {
                            LOGGER.log(Level.FINER, "\tRemove all task token: " + token);
                            tokensToRemove.add(token);
                        }
                    }
                }
                if (listsToRemove[4]) {
                    // Remove all proxy tokens
                    for (Token token : inPlace.getTokens()) {
                        if (token.getType() == TokenType.Proxy && !tokensToRemove.contains(token)) {
                            LOGGER.log(Level.FINER, "\tRemove all proxy token: " + token);
                            tokensToRemove.add(token);
                        }
                    }
                }
                if (!taskTokensToRemove.isEmpty()) {
                    for (Token token : taskTokensToRemove) {
                        if (!tokensToRemove.contains(token)) {
                            LOGGER.log(Level.FINER, "\tRemove task token to remove: " + token);
                            tokensToRemove.add(token);
                        }
                    }
                }
                if (genericCount > 0) {
                    for (int i = 0; i < genericCount; i++) {
                        LOGGER.log(Level.FINER, "\tRemove generic");
                        tokensToRemove.add(Engine.getInstance().getGenericToken());
                    }
                }
            }
            LOGGER.log(Level.FINE, "TokensToRemove for Place: " + inPlace + " leading to Transition: " + transition + "\n" + tokensToRemove);

            leavePlace(inPlace, tokensToRemove);
        }

        ////
        // Add things
        ////
        for (Place outPlace : transition.getOutPlaces()) {
            // boolean[3] corrensponds to add/remove [ All, RelevantTasks, RelevantProxies ]
            boolean[] listsToAdd = outPlaceToListsToAdd.get(outPlace);
            ArrayList<Token> taskTokensToAdd = outPlaceToTaskTokensToAdd.get(outPlace);
            Integer genericCount = outPlaceToGenericToAdd.get(outPlace);
            ArrayList<Token> tokensToAdd = new ArrayList<Token>();

            if (listsToAdd[0]) {
                // Remove all and finish
                tokensToAdd.addAll(allTokens);
            } else {
                if (listsToAdd[1]) {
                    for (Token token : relevantTaskTokens) {
                        if (!tokensToAdd.contains(token)) {
                            tokensToAdd.add(token);
                        }
                    }
                }
                if (listsToAdd[2]) {
                    for (Token token : relevantProxyTokens) {
                        if (!tokensToAdd.contains(token)) {
                            tokensToAdd.add(token);
                        }
                    }
                }
                if (listsToAdd[3]) {
                    for (Token token : allTaskTokens) {
                        if (!tokensToAdd.contains(token)) {
                            tokensToAdd.add(token);
                        }
                    }
                }
                if (listsToAdd[4]) {
                    for (Token token : allProxyTokens) {
                        if (!tokensToAdd.contains(token)) {
                            tokensToAdd.add(token);
                        }
                    }
                }
                if (!taskTokensToAdd.isEmpty()) {
                    for (Token token : taskTokensToAdd) {
                        if (!tokensToAdd.contains(token)) {
                            tokensToAdd.add(token);
                        }
                    }
                }
                if (genericCount > 0) {
                    for (int i = 0; i < genericCount; i++) {
                        tokensToAdd.add(Engine.getInstance().getGenericToken());
                    }
                }
            }
            enterPlace(outPlace, tokensToAdd, false);
        }

        // If we had a null RP blocking event we cloned for RPs, we should remove the clones
        //  we don't want them to still exist if we re-enter this transition
        // Get a list of blocking IE classes which we cloned
        ArrayList<Class> eventClassesToRemove = new ArrayList<Class>();
        for (InputEvent ie : transition.getInputEvents()) {
            if (ie instanceof BlockingInputEvent && ie.getRelevantProxyList() == null) {
                eventClassesToRemove.add(ie.getClass());

                // Clear lookup
                if (clonedIeTable.containsKey(ie)) {
                    HashMap<ProxyInt, InputEvent> proxyLookup = clonedIeTable.get(ie);
                    proxyLookup.clear();
                }

            }
        }
        // Get the clones of those classes
        ArrayList<InputEvent> clonedIesToRemove = new ArrayList<InputEvent>();
        for (InputEvent ie : transition.getInputEvents()) {
            if (eventClassesToRemove.contains(ie.getClass()) && ie.getRelevantProxyList() != null) {
                clonedIesToRemove.add(ie);
            }
        }
        // Remove the clones
        for (InputEvent clonedIe : clonedIesToRemove) {
            activeInputEvents.remove(clonedIe);
            transition.removeInputEvent(clonedIe);
            inputEventToTransitionMap.remove(clonedIe);
        }

        // Clear our input event status, in case we re-enter a previous place
        transition.clearInputEventStatus();

        ////
        // Now we can check the transitions we added tokens to
        ////
        LOGGER.log(Level.INFO, "\tDone executing " + transition + ", checking for new transitions");
        for (Place outPlace : transition.getOutPlaces()) {
            for (Transition t2 : outPlace.getOutTransitions()) {
                HashMap<Place, ArrayList<Token>> tokensToRemove2 = checkTransition(t2);
                if (tokensToRemove2 != null) {
                    executeTransition(t2, tokensToRemove2);
                }
            }
        }
    }

    private synchronized void leavePlace(Place place, ArrayList<Token> tokens) {
        LOGGER.log(Level.INFO, "Leaving " + place + " with " + place.getTokens() + " and taking " + tokens);

        // Remove Edge specified Tokens from Place
        for (Token token : tokens) {
            if (place.removeToken(token)) {
                LOGGER.log(Level.FINE, "\tRemoved " + token + " from " + place);
            } else {
                LOGGER.log(Level.SEVERE, "\tTrying to leave " + place + ", but am missing " + token);
                System.exit(0);
            }
        }

        // Check to see if any tokens are left in this place, or if it has become inactive
        if (place.getTokens().isEmpty()) {
            place.setIsActive(false);
            // This place is no longer active, unregister all input events from the event mapper
            for (Transition transition : place.getOutTransitions()) {
                boolean stillActive = false;
                for (Place inPlace : transition.getInPlaces()) {
                    if (inPlace != place && inPlace.getIsActive()) {
                        stillActive = true;
                        break;
                    }
                }
                if (!stillActive) {
                    for (InputEvent inputEvent : transition.getInputEvents()) {
                        if (!activeInputEvents.contains(inputEvent)) {
                            LOGGER.warning("Tried to remove IE: " + inputEvent + " from activeInputEvents, but it is not a member");
                        }
                        activeInputEvents.remove(inputEvent);

                        InputEventMapper.getInstance().unregisterEvent(inputEvent);

                        if (inputEventToTransitionMap.containsKey(inputEvent)) {
                            Transition t = inputEventToTransitionMap.remove(inputEvent);
                            LOGGER.log(Level.FINE, "\t Removed <" + inputEvent + ", " + t + "> from inputEventToTransitionMap");
                        } else {
                            LOGGER.warning("\t Tried to remove IE: " + inputEvent + " from inputEventToTransitionMap, but it is not a key");
                        }
                    }
                } else {
                    LOGGER.log(Level.FINE, "\t Not unregistering transition: " + transition + " as other connected Places are still active");
                }
            }

            // Tell watchers thet we have updates
            Engine.getInstance().leavePlace(this, place);
        }
    }

    private synchronized void enterPlace(Place place, Token token, boolean checkForTransition) {
        ArrayList<Token> tokens = new ArrayList<Token>();
        tokens.add(token);
        LOGGER.info("enter place 4");
        enterPlace(place, tokens, checkForTransition);
    }

    private synchronized void enterPlace(Place place, ArrayList<Token> tokens, boolean checkForTransition) {
        LOGGER.log(Level.INFO, "Entering " + place + " with Tokens: " + tokens + " with checkForTransition: " + checkForTransition + ", getInTransitions: " + place.getInTransitions() + ", inEdges: " + place.getInEdges() + ", getOutTransitions: " + place.getOutTransitions() + ", outEdges: " + place.getOutEdges());

        // 1 - Make note that this place should finish being entered before any of its
        //  transitions are actually executed
        synchronized (placesBeingEntered) {
            placesBeingEntered.add(place);
        }

        // 2 - Add the new tokens to the place
        for (Token token : tokens) {
            place.addToken(token);
        }

        // 3 - If this is our first time to enter the place, set up the requirements for all possible transitions attached to this place
        synchronized (activeInputEvents) {
            if (!place.getIsActive()) {
                // Things are not registered, do it now
                for (Transition transition : place.getOutTransitions()) {
                    for (InputEvent inputEvent : transition.getInputEvents()) {
                        if (!inputEventToTransitionMap.containsKey(inputEvent)) {
                            activeInputEvents.add(inputEvent);
                            InputEventMapper.getInstance().registerEvent(inputEvent, this);
                            LOGGER.log(Level.FINE, "\tAdding <" + inputEvent + "," + transition + "> to inputEventToTransitionMap");
                            inputEventToTransitionMap.put(inputEvent, transition);
                        }
                    }
                }
                place.setIsActive(true);
            }
        }

        // 4 - Invoke each OutputEvent with the new tokens
        processOutputEvents(place.getOutputEvents(), tokens);

        // 5 - Check for sub-missions and start them if required
        if (place.getSubMission() != null) {
            LOGGER.log(Level.INFO, "\tStarting submission " + place.getSubMission());
            PlanManager planManager = Engine.getInstance().spawnMission(place.getSubMission(), tokens);
            planManagerToPlace.put(planManager, place);
            Engine.getInstance().addListener(this);
        }

        // 6 - Tell listeners we have entered a place
        Engine.getInstance().enterPlace(this, place);

        // 7 - It is now safe to execute transitions leading out of this place
        synchronized (placesBeingEntered) {
            placesBeingEntered.remove(place);
        }

        // 8 - Check if any transitions out of this place should execute
        if (checkForTransition) {
            for (Transition transition : place.getOutTransitions()) {
                HashMap<Place, ArrayList<Token>> tokensToRemove = checkTransition(transition);
                if (tokensToRemove != null) {
                    executeTransition(transition, tokensToRemove);
                };
            }
        }

        // 9 - Check if we are at an end place
        if (place.isEnd()) {
            LOGGER.log(Level.INFO, "\tReached an end place: " + place);
            boolean end = true;
            for (Vertex v : mSpec.getGraph().getVertices()) {
                if (v instanceof Place) {
                    Place checkPlace = (Place) v;
                    if ((checkPlace.getFunctionMode() == FunctionMode.HiddenRecovery || checkPlace.getFunctionMode() == FunctionMode.Recovery)
                            && checkPlace.getTokens().size() > 0
                            && !checkPlace.isEnd()) {
                        LOGGER.log(Level.INFO, "\t\tRecovery place has tokens in it! " + checkPlace);
                        end = false;
                    }
                }
            }
            if (end) {
                finishMission(place);
            }
        }
    }

    private void processOutputEvents(ArrayList<OutputEvent> outputEvents, ArrayList<Token> tokens) {
        for (OutputEvent oe : outputEvents) {
            LOGGER.log(Level.INFO, "Processing event " + oe + " with input variables " + oe.getVariables());
            // Write in values for any fields that were filled with a variable name
            if (oe.getVariables() != null) {
                for (String variableName : oe.getVariables().keySet()) {
                    Object variableValue = variableNameToValue.get(variableName);
                    LOGGER.log(Level.FINE, "\tLooking for " + variableName + " to set " + oe.getVariables().get(variableName) + " find " + variableValue + " " + variableNameToValue);
                    if (variableValue != null) {
                        Field f = oe.getVariables().get(variableName);
                        try {
                            LOGGER.log(Level.FINE, "\t\tAttempting to set " + f + " on " + oe + " to " + variableValue);
                            setField(f, oe, variableValue);
                        } catch (IllegalArgumentException ex) {
                            Logger.getLogger(PlanManager.class.getName()).log(Level.SEVERE, "Failed to set variable value on OutputEvent: " + ex, ex);
                        }
                    } else {
                        LOGGER.log(Level.SEVERE, "No value for variable: " + variableName);
                    }

                }
            } else {
                LOGGER.log(Level.INFO, "\tNo variables for " + oe);
            }
            // Find and invoke an appropriate handler
            EventHandlerInt eh = Engine.getInstance().getHandler(oe.getClass());
            if (eh != null) {
                LOGGER.log(Level.INFO, "Invoking handler: " + eh + " for OE: " + oe);
                eh.invoke(oe, tokens);
            } else {
                LOGGER.log(Level.SEVERE, "No handler for event of type " + oe.getClass());
            }

            // If we are aborting the mission, need to do some deregistration
            if (oe instanceof AbortMission) {
                abortMission();
            }
        }
    }

    @Override
    public void eventGenerated(InputEvent generatedEvent) {
        generatorEventQueue.offer(generatedEvent);
    }

    public void finishMission(Place endPlace) {
        LOGGER.log(Level.INFO, "We reached an end state: " + endPlace);
        Engine.getInstance().done(this);
        // Unregister any IEs still active
        for (InputEvent ie : activeInputEvents) {
            LOGGER.log(Level.INFO, "\tUnregistering active input event:" + ie);
            InputEventMapper.getInstance().unregisterEvent(ie);
        }
        activeInputEvents.clear();
    }

    public void abortMission() {
        // Get removed from Engine's plan manager listener list
        Engine.getInstance().abort(this);
        // Unregister any IEs still active
        for (InputEvent ie : activeInputEvents) {
            InputEventMapper.getInstance().unregisterEvent(ie);
        }
        activeInputEvents.clear();

        // Put token in end place?
    }

    public void processGeneratedEvent(InputEvent generatedEvent) {
        LOGGER.log(Level.INFO, "Processing generated event " + generatedEvent + " with event UUID " + generatedEvent.getId() + " mission UUID " + generatedEvent.getMissionId() + " and relevant proxy " + generatedEvent.getRelevantProxyList());
        if (generatedEvent.getMissionId() == null) {
            LOGGER.log(Level.FINE, "\tGenerated event has no mission UUID");
        } else {
            if (generatedEvent.getMissionId() != missionId) {
                LOGGER.log(Level.FINE, "\tMatching failed on mission UUID comparison");
                return;
            } else {
                LOGGER.log(Level.FINE, "\tMatching success on mission UUID comparison");
            }
        }
        // Events to add: A list of cloned IEs used to keep track of which proxy's have generated their instance of a Blocking IE
        HashMap<InputEvent, Transition> clonedEventsToAdd = new HashMap<InputEvent, Transition>();
        ArrayList<InputEvent> eventsToRemove = new ArrayList<InputEvent>();
        // One of our active transitions has an input event that resulted in a subscription to an InformationServiceProvider of this class
        boolean match;
        ArrayList<InputEvent> matchingEvents = new ArrayList<InputEvent>();

        // 1 - Go through our list of parameter events waiting to be fulfilled by a generated event
        for (InputEvent paramEvent : activeInputEvents) {
            LOGGER.log(Level.FINE, "\tChecking for match between parameter " + paramEvent + " to generated " + generatedEvent);

            // 2 - Compare variables used by the generated event to find the param event(s) it fulfills
            // 2a - Same class? 
            //  @todo - can remove this step with some additional data structures...
            if (paramEvent.getClass() != generatedEvent.getClass()) {
                LOGGER.log(Level.FINE, "\t\tMatching failed on class comparison: " + paramEvent.getClass() + " != " + generatedEvent.getClass());
                continue;
            } else {
                LOGGER.log(Level.FINE, "\t\tMatching success on class: " + paramEvent.getClass());
            }
            // 2b - If defined, we know the output event that caused this to occur
            //  Share a common OutputEvent uuid in a preceding place?
            if (generatedEvent.getRelevantOutputEventId() != null) {
                Transition transition = inputEventToTransitionMap.get(paramEvent);
                ArrayList<Place> inPlaces = transition.getInPlaces();
                match = false;
                for (Place inPlace : inPlaces) {
                    for (OutputEvent oe : inPlace.getOutputEvents()) {
                        if (oe.getId().equals(generatedEvent.getRelevantOutputEventId())) {
                            LOGGER.log(Level.FINE, "\t\tMatching success on relevant OE UUID: " + oe.getId());
                            match = true;
                        }
                    }
                }
                if (!match) {
                    LOGGER.log(Level.FINE, "\t\tMatching failed on UUID relevant OE UUID comparison - does not share a common OutputEvent uuid from a preceding place?");
                    continue;
                }
            } else {
                LOGGER.log(Level.FINE, "\t\tMatching success on UUID - no UUID to match against");
            }
            // 2c - If defined, this was some sort of proxy triggered event
            //  Have a token (Proxy or Task) for the proxy?
            //  Record the param event that matches this generator event
            if (generatedEvent.getRelevantProxyList() != null) {
                if (paramEvent.getRelevantProxyList() == null) {
                    // This is a proxy type event where no relevant proxies were specified in the OE, but relevant proxies exist in the resulting IE
                    if (paramEvent instanceof BlockingInputEvent) {
                        // For blocking input events, we require that all proxies on incoming edges be accounted for 
                        //  (ie, be contained in the RP list of the IE or a copy of it)
                        // For instance, a ProxyExploreArea would compute paths for a set of proxies to take, but each proxy will
                        //  individually send ProxyPathCompleted IEs to the system
                        // When an gen Blocking IE is received AND it matches the class of a param IE, AND the gen IE has RP, 
                        //  AND the param IE has null RP, check to see if we have created a copy of the param IE with the RP set to this proxy
                        //  If we have, we will/have match it, and if not, we should create it and set it to fulfilled. Repeat the IE copy 
                        //  process for each proxy token in all the transition's incoming places
                        //@todo Should it only be for incoming places with RP on the edge going to the transition?
                        LOGGER.log(Level.FINE, "\t\tHandling occurence of BlockingInputEvent with null RP in param: " + paramEvent + " and defined RP in gen: " + generatedEvent);

                        Transition transition = inputEventToTransitionMap.get(paramEvent);
                        HashMap<ProxyInt, InputEvent> proxyLookup;
                        if (clonedIeTable.containsKey(paramEvent)) {
                            proxyLookup = clonedIeTable.get(paramEvent);
                        } else {
                            proxyLookup = new HashMap<ProxyInt, InputEvent>();
                            clonedIeTable.put(paramEvent, proxyLookup);
                        }
                        ArrayList<ProxyInt> proxiesToCheck = new ArrayList<ProxyInt>();
                        // Get list of proxies that we will check that a cloned IE exists for 
                        // First add each proxy in incoming place with RP on the edge
                        for (Edge inEdge : transition.getInEdges()) {
                            boolean hasRpReq = false;
                            for (Token token : inEdge.getTokenRequirements()) {
                                if (token.getType() == TokenType.MatchRelevantProxy) {
                                    hasRpReq = true;
                                    break;
                                }
                            }
                            if (hasRpReq) {
                                if (!(inEdge.getStart() instanceof Place)) {
                                    LOGGER.severe("Incoming edge to a transition was not a place!");
                                    System.exit(0);
                                }
                                for (Token token : ((Place) inEdge.getStart()).getTokens()) {
                                    if (token.getProxy() != null && !proxiesToCheck.contains(token.getProxy())) {
                                        proxiesToCheck.add(token.getProxy());
                                    }
                                }
                            }
                        }
                        // Next, look for incoming places with a Task token on the edge
                        //  If the Task token has an assigned proxy, add it to the list of proxies to check
                        for (Edge inEdge : transition.getInEdges()) {
                            boolean hasRpReq = false;
                            for (Token token : inEdge.getTokenRequirements()) {
                                if (token.getType() == TokenType.Task) {
                                    if (token.getProxy() != null && !proxiesToCheck.contains(token.getProxy())) {
                                        proxiesToCheck.add(token.getProxy());
                                    }
                                } else if (token.getType() == TokenType.Proxy) {
                                    // This shouldn't happen - you can't label edge with a Proxy token
                                    //  but I'm putting it here just in case this is used in some special case in the future
                                    if (token.getProxy() != null && !proxiesToCheck.contains(token.getProxy())) {
                                        proxiesToCheck.add(token.getProxy());
                                    }
                                }
                            }
                        }
                        // Check that we have proxies to check, otherwise something is probably wrong
                        if (proxiesToCheck.isEmpty()) {
                            LOGGER.severe("Generated event RP != null and parameter event RP == null, but have no incoming edges with RP requirement!");
                        }

                        // Check that we have a cloned IE for each of the proxies in proxiesToCheck
                        //  If we don't, we need to create one
                        ArrayList<InputEvent> createdClones = new ArrayList<InputEvent>();
                        for (ProxyInt proxy : proxiesToCheck) {
                            if (!proxyLookup.containsKey(proxy)) {
                                // Clone the input event and then set the relevant proxy
                                InputEvent clonedEvent = paramEvent.copyForProxyTrigger();
                                ArrayList<ProxyInt> relevantProxyList = new ArrayList<ProxyInt>();
                                relevantProxyList.add(proxy);
                                clonedEvent.setRelevantProxyList(relevantProxyList);
                                // Add the cloned ie to the lookup table
                                proxyLookup.put(proxy, clonedEvent);
                                // Add the cloned ie to the list of input events waiting to be fulfilled
                                clonedEventsToAdd.put(clonedEvent, transition);
                                createdClones.add(clonedEvent);
                            }
                        }
                        // Mark the status of the null RP param event as "complete" so it won't prevent the transition from firing
                        //  It is still an active input event though, so events will continue to try and match against it,
                        //  creating more copies of it with new RP as needed
                        transition.setInputEventStatus(paramEvent, true);

                        // Now go through events we just created and ones matching proxies in the gen IE's RP
                        match = false;

                        for (ProxyInt proxy : generatedEvent.getRelevantProxyList()) {
                            if (proxyLookup.containsKey(proxy)) {
                                InputEvent matchingClonedEvent = proxyLookup.get(proxy);
                                if (!transition.getInputEventStatus(matchingClonedEvent)
                                        && createdClones.contains(matchingClonedEvent)) {
                                    // Hasn't been matched yet and the cloned event for the proxy was just created
                                    //  If it was previously created (ie is in activeInputEvents instead of eventsToAdd),
                                    //  it will be checked and matched - we don't want to add it to matchingEvents a second time here
                                    matchingEvents.add(matchingClonedEvent);
                                    match = true;
                                    LOGGER.log(Level.INFO, "\t\t\tMatching success on relevant proxy: " + proxy);
                                } else if (transition.getInputEventStatus(matchingClonedEvent)) {
                                    match = true;
                                    LOGGER.log(Level.WARNING, "\t\t\tMatching success on relevant proxy: " + proxy + ", but the corresponding cloned IE was already marked as having occurred!");
                                } else if (!createdClones.contains(matchingClonedEvent)) {
                                    match = true;
                                    LOGGER.log(Level.INFO, "\t\t\tMatching success on relevant proxy: " + proxy + ", but the corresponding cloned IE was already created!");
                                }
                            }
                        }
                        if (!match) {
                            LOGGER.log(Level.FINE, "\t\tMatching failed on relevant proxy - param event had no relevant proxy and no preceding place with a token containing the gen event's relevant proxy");
                            continue;
                        }
                    } else {
                        // For non-blocking input events, we don't require that all proxies on incoming edges are accounted for,
                        //  but for each proxy in the relevant proxy list we must have a token containing that proxy in an incoming Place
                        // For instance, an OE event requesting a selection of proxies would have no relevant proxies. The operator
                        //  would choose from the proxies contained in the tokens passed into the Transition.
                        //  The resulting IE would have contain the selected proxies in its relevant proxy list, which would
                        //  be a subset of the proxies contained in the tokens in the incoming places
                        LOGGER.log(Level.FINE, "\t\tHandling non-BlockingInputEvent " + paramEvent);
                        matchingEvents.add(paramEvent);
                    }
                } else {
                    // Check that the param event's proxy list matches the generated event's proxy list
                    match = true;
                    for (ProxyInt proxy : paramEvent.getRelevantProxyList()) {
                        if (!generatedEvent.getRelevantProxyList().contains(proxy)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        LOGGER.log(Level.FINE, "\t\tMatching success on relevant proxy - param event's relevant proxy matched gen event's relevant proxy");
                        // The process above has occurred previously, so we have versions of the ie with the proxy specified
                        matchingEvents.add(paramEvent);
                    } else {
                        LOGGER.log(Level.FINE, "\t\tMatching failed on relevant proxy - param event's relevant proxy did not match gen event's relevant proxy");
                        continue;
                    }
                }
            } else {
                LOGGER.log(Level.FINE, "\t\tMatching success on relevant proxy - gen event had no relevant proxy to match");
                // Generator event had no relevant proxy so no matching is necessary
                matchingEvents.add(paramEvent);
            }
        }

        LOGGER.log(Level.INFO, "\tResult of comparisons: add " + clonedEventsToAdd.size() + ", remove " + eventsToRemove.size() + ", update " + matchingEvents.size());

        for (InputEvent ie : clonedEventsToAdd.keySet()) {
            LOGGER.log(Level.INFO, "\t\tAdding " + ie);
            activeInputEvents.add(ie);
            Transition t = clonedEventsToAdd.get(ie);
            t.addInputEvent(ie);
            inputEventToTransitionMap.put(ie, t);
        }
        for (InputEvent ie : eventsToRemove) {
            // THIS LIST IS NEVER MODIFIED
            LOGGER.log(Level.INFO, "\t\tRemoving " + ie);
            activeInputEvents.remove(ie);
            Transition t = inputEventToTransitionMap.get(ie);
            t.removeInputEvent(ie);
            inputEventToTransitionMap.remove(ie);
        }
        for (InputEvent ie : matchingEvents) {
            LOGGER.log(Level.INFO, "\t\tUpdating " + ie);
            ie.setGeneratorEvent(generatedEvent);
            // Handle updated event
            processUpdatedParamEvent(ie);
        }
        LOGGER.log(Level.INFO, "\tFinished handling generated event: " + generatedEvent);
    }

    public void processUpdatedParamEvent(InputEvent updatedParamEvent) {
        LOGGER.log(Level.INFO, "Processing updated param event " + updatedParamEvent + " with UUID " + updatedParamEvent.getId() + " and relevant proxy " + updatedParamEvent.getRelevantProxyList());

        InputEvent generatorEvent = updatedParamEvent.getGeneratorEvent();
        if (!inputEventToTransitionMap.containsKey(updatedParamEvent)) {
            LOGGER.severe("No mapping from updated param event " + updatedParamEvent + " to transition!");
            return;
        }
        Transition transition = inputEventToTransitionMap.get(updatedParamEvent);

        // 1 - Check if there is an attached allocation to assign
        if (generatorEvent.getAllocation() != null) {
            Map<ITask, AbstractAsset> allocation = generatorEvent.getAllocation().getAllocation();
            LOGGER.log(Level.FINE, "\tInputEvent " + updatedParamEvent + " tied to " + transition + " occurred with an attach allocation: " + generatorEvent.getAllocation().toString());
            for (ITask task : allocation.keySet()) {
                Token token = Engine.getInstance().getToken((Task) task);
                if (token != null) {
                    LOGGER.log(Level.FINE, "\t\tFound token " + token + " for task " + task);
                    AbstractAsset asset = allocation.get(task);
                    ProxyInt proxy = Engine.getInstance().getProxyServer().getProxy(asset);
                    if (proxy != null) {
                        LOGGER.log(Level.FINE, "\t\tFound proxy " + proxy + " for asset " + asset);
                        token.setProxy(proxy);
                    } else {
                        LOGGER.log(Level.SEVERE, "\t\tCould not find proxy for asset " + asset);
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "\t\tCould not find token for task " + task);
                }
            }
        }

        // 2a - Assign any missing instance params that were missing and have now been received
        if (generatorEvent instanceof MissingParamsReceived) {
            MissingParamsReceived paramsReceived = (MissingParamsReceived) generatorEvent;
            LOGGER.log(Level.FINE, "Writing parameters from MissingParamsReceived: " + paramsReceived);
            Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues = paramsReceived.getEventSpecToFieldValues();
            for (ReflectedEventSpecification eventSpec : eventSpecToFieldValues.keySet()) {
                Hashtable<Field, Object> fieldsToValues = eventSpecToFieldValues.get(eventSpec);
                for (Field field : fieldsToValues.keySet()) {
                    eventSpec.addFieldDefinition(field.getName(), fieldsToValues.get(field));
                }
            }

            // Check if we have any required params that are still not defined
            ArrayList<ReflectedEventSpecification> missing = mSpec.getEventSpecsRequiringParams();
            if (missing.size() > 0) {
                for (ReflectedEventSpecification eventSpec : missing) {
                    LOGGER.warning("Event spec for " + eventSpec.getClassName() + " is missing fields");
                }
            }

            //@todo ugly code!
            //@todo what if we still have missing params that the operator decided not to fill out?
            // Should we instantiate the plan now?
            if (!mSpec.isInstantiated()) {
                mSpec.instantiate(missionId);
            }
        }

        // 2b - Assign any variable values returned in the generator event
        // The variables are on the InputEvent, because that has come from the spec, but the values are in the generator event
        HashMap<String, String> variables = updatedParamEvent.getVariables();
        if (variables != null) {
            LOGGER.log(Level.FINE, "\tInputEvent " + updatedParamEvent + " tied to " + transition + " occurred with variables: " + variables);

            for (String fieldName : variables.keySet()) {
                LOGGER.log(Level.FINE, "\toccurred looking at variable " + fieldName);
                // For each variable in the response event
                Field f;
                try {
                    // Get the variable's Field object
                    f = generatorEvent.getClass().getField(fieldName);
                    if (f != null) {
                        f.setAccessible(true);
                        // Retrieve the value of the variable's Field object
                        variableNameToValue.put(variables.get(fieldName), f.get(generatorEvent));
                        LOGGER.log(Level.FINE, "\t\tVariable set " + variables.get(fieldName) + " = " + f.get(generatorEvent));
                    } else {
                        LOGGER.log(Level.WARNING, "\t\tGetting field failed: " + fieldName);
                    }
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(PlanManager.class.getName()).log(Level.SEVERE, "\t\tFinding field for variable failed", ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(PlanManager.class.getName()).log(Level.SEVERE, "\t\tFinding field for variable failed", ex);
                } catch (NoSuchFieldException ex) {
                    Logger.getLogger(PlanManager.class.getName()).log(Level.SEVERE, "\t\tFinding field for variable failed", ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(PlanManager.class.getName()).log(Level.SEVERE, "\t\tFinding field for variable failed", ex);
                }
            }
        } else {
            LOGGER.log(Level.FINE, "\tInputEvent " + updatedParamEvent + " tied to " + transition + " occurred with no variables");
        }

        // 3 - Update the input event's status in the transition, remove it from the "active" input event list, and check if the transition should trigger now
        synchronized (activeInputEvents) {
            transition.setInputEventStatus(updatedParamEvent, true);
            HashMap<Place, ArrayList<Token>> tokensToRemove = checkTransition(transition);
            if (tokensToRemove != null) {
                executeTransition(transition, tokensToRemove);
            }
        }
    }

    public Place getStartPlace() {
        return startPlace;
    }

    /**
     * This has to recurse through the object, creating objects as it goes until
     * it finds the field that needs to be set
     *
     * @param f
     * @param e
     * @param v
     */
    private void setField(Field f, OutputEvent e, Object v) {
        LOGGER.log(Level.FINER, "Setting " + f + " on " + e + " to " + v);
        try {
            ArrayList options = new ArrayList();
            options.add(e);
            Object actualObject = null;
            while (options.size() > 0) {
                Object currObject = options.remove(0);
                try {
                    currObject.getClass().getDeclaredField(f.getName());
                    actualObject = currObject;
                } catch (Exception ex) {
                    for (Field field : currObject.getClass().getDeclaredFields()) {
                        try {
                            // If the field object needs to be created, do it, otherwise just add to options
                            Object o = field.get(currObject);
                            if (o == null) {
                                field.getType().newInstance();
                                field.set(currObject, o);
                            }
                            options.add(o);
                        } catch (Exception exception) {
                            LOGGER.log(Level.WARNING, "Failed to created object for " + field);
                        }
                    }
                }
            }
            if (actualObject != null) {
                f.set(actualObject, v);
                LOGGER.log(Level.FINER, "Variable set successfully: " + f + " on " + actualObject + " to " + v);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed: " + ex, ex);
        }
    }

    public void addProxyToken(Token token) {
        defaultStartTokens.add(token);
    }

    public void removeProxy(ProxyInt p) {
        // @todo Implement remove proxy
        LOGGER.log(Level.WARNING, "Removing proxy from plan unimplemented");
    }

    public String getPlanName() {
        return planName;
    }

    @Override
    public void planCreated(PlanManager planManager, MissionPlanSpecification mSpec) {
    }

    @Override
    public void planStarted(PlanManager planManager) {
    }

    @Override
    public void planEnteredPlace(PlanManager planManager, Place place) {
    }

    @Override
    public void planLeftPlace(PlanManager planManager, Place place) {
    }

    @Override
    public void planFinished(PlanManager planManager) {
        Place place = planManagerToPlace.get(planManager);
        if (place != null) {
            place.setSubMissionComplete(true);
            for (Transition transition : place.getOutTransitions()) {
                HashMap<Place, ArrayList<Token>> tokensToRemove = checkTransition(transition);
                if (tokensToRemove != null) {
                    executeTransition(transition, tokensToRemove);
                };
            }
        }
    }

    @Override
    public void planAborted(PlanManager planManager) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
