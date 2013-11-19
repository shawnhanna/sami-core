package sami.mission;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.event.Event;
import sami.event.InputEvent;
import sami.event.OutputEvent;
import sami.event.ReflectedEventSpecification;
import sami.mission.TokenSpecification.TokenType;

/**
 *
 * @author pscerri
 */
public class MissionPlanSpecification implements java.io.Serializable {

    private static final Logger LOGGER = Logger.getLogger(MissionPlanSpecification.class.getName());
    static final long serialVersionUID = 2L;
    private AffineTransform layoutTransform = null;
    private AffineTransform viewTransform = null;
    private ArrayList<TokenSpecification> customTaskTokenSpecList = new ArrayList<TokenSpecification>();    // Task tokens created in the spec by the developer
    private Graph<Vertex, Edge> graph = null;
    private Map<Edge, ArrayList<TokenSpecification>> edgeToTokenSpecListMap = new HashMap<Edge, ArrayList<TokenSpecification>>();
    private Map<Vertex, ArrayList<ReflectedEventSpecification>> vertexToEventSpecListMap = new HashMap<Vertex, ArrayList<ReflectedEventSpecification>>();
    private Map<Vertex, Point2D> locations = null;
    private String name = "Anonymous";
    transient private boolean isInstantiated = false;
    transient private ArrayList<TokenSpecification> inTokenSpecList, outTokenSpecList, outRecovTokenSpecList;

    public MissionPlanSpecification(String name) {
        this.name = name;
        createTokenSpecLists();
    }

    /**
     * Produces a copy of the passed in mission plan specification, probably for
     * creating a sub-mission
     *
     * @param missionSpec The spec to be copied
     */
    public MissionPlanSpecification(MissionPlanSpecification missionSpec) {
        // Immutables
        name = missionSpec.name;
        // Mutables
        layoutTransform = (AffineTransform) missionSpec.layoutTransform.clone();
        viewTransform = (AffineTransform) missionSpec.viewTransform.clone();
        graph = new SparseMultigraph<Vertex, Edge>();

        // Create copies of places and transitions - at this point they won't know their edges or what vertices they are connected to
        //  Copies of any events or submissions on the vertices will be created for the vertex copy
        HashMap<Vertex, Vertex> vertexMap = new HashMap<Vertex, Vertex>();
        HashMap<Edge, Edge> edgeMap = new HashMap<Edge, Edge>();
        for (Vertex vertex : missionSpec.graph.getVertices()) {
            if (vertex instanceof Place) {
                Place place = (Place) vertex;
                Place placeCopy = place.copyWithoutConnections();
                vertexMap.put(vertex, placeCopy);
                graph.addVertex(placeCopy);
            } else if (vertex instanceof Transition) {
                Transition transition = (Transition) vertex;
                Transition transitionCopy = transition.copyWithoutConnections();
                vertexMap.put(vertex, transitionCopy);
                graph.addVertex(transitionCopy);
            } else {
                LOGGER.severe("Vertex is not an instance of Place OR Transition! " + vertex);
            }
        }
        // Create copies of the edges connecting our copies of the vertices
        for (Edge edge : missionSpec.graph.getEdges()) {
            Edge edgeCopy = new Edge(vertexMap.get(edge.getStart()), vertexMap.get(edge.getEnd()), edge.getFunctionMode());
            edgeMap.put(edge, edgeCopy);
            graph.addEdge(edgeCopy, vertexMap.get(edge.getStart()), vertexMap.get(edge.getEnd()));
        }
        // Copy the token specifications used on each the edge copy
        edgeToTokenSpecListMap = new HashMap<Edge, ArrayList<TokenSpecification>>();
        for (Edge edge : missionSpec.edgeToTokenSpecListMap.keySet()) {
            Edge edgeCopy = edgeMap.get(edge);
            ArrayList<TokenSpecification> mapCopy = new ArrayList<TokenSpecification>();
            for (TokenSpecification ts : missionSpec.edgeToTokenSpecListMap.get(edge)) {
                mapCopy.add(ts);
                edgeCopy.addTokenName(ts.toString());
            }
            edgeToTokenSpecListMap.put(edgeCopy, mapCopy);
        }
        vertexToEventSpecListMap = new HashMap<Vertex, ArrayList<ReflectedEventSpecification>>();
        for (Vertex vertex : missionSpec.vertexToEventSpecListMap.keySet()) {
            Vertex vertexCopy = vertexMap.get(vertex);
            ArrayList<ReflectedEventSpecification> listCopy = new ArrayList<ReflectedEventSpecification>();
            for (ReflectedEventSpecification spec : missionSpec.vertexToEventSpecListMap.get(vertex)) {
                listCopy.add(spec.copySpecial());
            }
            vertexToEventSpecListMap.put(vertexCopy, listCopy);
        }
        locations = new HashMap<Vertex, Point2D>();
        for (Vertex vertex : missionSpec.locations.keySet()) {
            Vertex vertexCopy = vertexMap.get(vertex);
            locations.put(vertexCopy, (Point2D) (missionSpec.locations.get(vertex).clone()));
        }
        for (Vertex vertex : missionSpec.graph.getVertices()) {
            Vertex vertexCopy = vertexMap.get(vertex);
            if (vertex instanceof Place) {
                Place placeCopy = (Place) vertexCopy;
                for (Transition t : ((Place) vertex).getInTransitions()) {
                    Transition transitionCopy = (Transition) vertexMap.get(t);
                    placeCopy.addInTransition(transitionCopy);
                }
                for (Transition t : ((Place) vertex).getOutTransitions()) {
                    Transition transitionCopy = (Transition) vertexMap.get(t);
                    placeCopy.addOutTransition(transitionCopy);
                }
            } else if (vertex instanceof Transition) {
                Transition transitionCopy = (Transition) vertexCopy;
                for (Place p : ((Transition) vertex).getInPlaces()) {
                    Place placeCopy = (Place) vertexMap.get(p);
                    transitionCopy.addInPlace(placeCopy);
                }
                for (Place p : ((Transition) vertex).getOutPlaces()) {
                    Place placeCopy = (Place) vertexMap.get(p);
                    transitionCopy.addOutPlace(placeCopy);
                }
            }
            for (Edge e : vertex.getInEdges()) {
                Edge edgeCopy = edgeMap.get(e);
                vertexCopy.addInEdge(edgeCopy);
            }
            for (Edge e : vertex.getOutEdges()) {
                Edge edgeCopy = edgeMap.get(e);
                vertexCopy.addOutEdge(edgeCopy);
            }
        }
        createTokenSpecLists();
        // Update tags
        updateAllTags();
    }

    private void createTokenSpecLists() {
        inTokenSpecList = new ArrayList<TokenSpecification>();    // All Tokens in the spec for edges ending on a transition
        outTokenSpecList = new ArrayList<TokenSpecification>();    // All Tokens in the spec for edges starting on a transition
        outRecovTokenSpecList = new ArrayList<TokenSpecification>();    // All Tokens in the spec for edges starting on a transition

        // Incoming
        TokenSpecification matchGenericTokenSpec = new TokenSpecification("Match G", TokenType.MatchGeneric, null);
        inTokenSpecList.add(matchGenericTokenSpec);
        TokenSpecification matchNoneTokenSpec = new TokenSpecification("No Req", TokenType.MatchNoReq, null);
        inTokenSpecList.add(matchNoneTokenSpec);
        TokenSpecification matchRelProxyTokenSpec = new TokenSpecification("Match RP", TokenType.MatchRelevantProxy, null);
        inTokenSpecList.add(matchRelProxyTokenSpec);
        // Outgoing
        TokenSpecification takeNoneTokenSpec = new TokenSpecification("Take None", TokenType.TakeNone, null);
        outTokenSpecList.add(takeNoneTokenSpec);
        outRecovTokenSpecList.add(takeNoneTokenSpec);
        TokenSpecification takeAllTokenSpec = new TokenSpecification("Take All", TokenType.TakeAll, null);
        outTokenSpecList.add(takeAllTokenSpec);
        outRecovTokenSpecList.add(takeAllTokenSpec);
        TokenSpecification takeGenericTokenSpec = new TokenSpecification("Take G", TokenType.TakeGeneric, null);
        outTokenSpecList.add(takeGenericTokenSpec);
        outRecovTokenSpecList.add(takeGenericTokenSpec);
        TokenSpecification takeRelProxyTokenSpec = new TokenSpecification("Take RP", TokenType.TakeRelevantProxy, null);
        outTokenSpecList.add(takeRelProxyTokenSpec);
        outRecovTokenSpecList.add(takeRelProxyTokenSpec);
        TokenSpecification takeRelTaskTokenSpec = new TokenSpecification("Take RT", TokenType.TakeRelevantTask, null);
        outTokenSpecList.add(takeRelTaskTokenSpec);
        outRecovTokenSpecList.add(takeRelTaskTokenSpec);
        TokenSpecification addGenericTokenSpec = new TokenSpecification("Add G", TokenType.AddGeneric, null);
        outTokenSpecList.add(addGenericTokenSpec);
        outRecovTokenSpecList.add(addGenericTokenSpec);
        // Outgoing for recovery mode edges
        TokenSpecification copyRelProxyTokenSpec = new TokenSpecification("Copy RP", TokenType.CopyRelevantProxy, null);
        outRecovTokenSpecList.add(copyRelProxyTokenSpec);
        TokenSpecification copyRelTaskTokenSpec = new TokenSpecification("Copy RT", TokenType.CopyRelevantTask, null);
        outRecovTokenSpecList.add(copyRelTaskTokenSpec);
    }

    public MissionPlanSpecification getSubmissionInstance(MissionPlanSpecification parentSpec, String namePrefix, String variablePrefix) {
        MissionPlanSpecification copy = new MissionPlanSpecification(this);

        // Apply prefixes
        copy.setName(namePrefix + "." + name);
        copy.addVariablePrefix(variablePrefix);

        return copy;
    }

    public void addVariablePrefix(String prefix) {
        // Add prefix to all variables in the plan, recursing into sub-missions
        for (Vertex key : vertexToEventSpecListMap.keySet()) {
            ArrayList<ReflectedEventSpecification> value = vertexToEventSpecListMap.get(key);
            for (ReflectedEventSpecification eventSpec : value) {
                eventSpec.addVariablePrefix(prefix);
            }
            if (key instanceof Place && ((Place) key).getSubMission() != null) {
                ((Place) key).getSubMission().addVariablePrefix(prefix);
            }
        }
    }

    public Graph<Vertex, Edge> getGraph() {
        return graph;
    }

    public ArrayList<TokenSpecification> getIncomingTokenSpecList() {
        return inTokenSpecList;
    }

    public ArrayList<TokenSpecification> getOutgoingTokenSpecList() {
        return outTokenSpecList;
    }

    public ArrayList<TokenSpecification> getOutgoingRecoveryTokenSpecList() {
        return outRecovTokenSpecList;
    }

    public ArrayList<TokenSpecification> getCustomTaskTokenSpecList() {
        return customTaskTokenSpecList;
    }

    public ArrayList<ReflectedEventSpecification> getEventSpecsRequiringParams() {
        ArrayList<ReflectedEventSpecification> ret = new ArrayList<ReflectedEventSpecification>();
        for (Vertex vertex : vertexToEventSpecListMap.keySet()) {
            ArrayList<ReflectedEventSpecification> events = vertexToEventSpecListMap.get(vertex);
            for (ReflectedEventSpecification reflectedEventSpecification : events) {
                if (reflectedEventSpecification.hasMissingParams(false)) {
                    ret.add(reflectedEventSpecification);
                }
            }
        }
        return ret;
    }

    public Place getUninstantiatedStart() {
        for (Vertex v : graph.getVertices()) {
            if (v instanceof Place && ((Place) v).isStart()) {
                return (Place) v;
            }
        }
        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "MissionPlanSpecification has no start state!");
        System.exit(0);
        return null;
    }

//    /**
//     * This is called in AIMS to start the mission, so need to instantiate.
//     * Likely the signature will have to be changed to accommodate params
//     *
//     * @return The starting state of the mission.
//     */
//    public Place getInstantiatedStart() {
//
//        // @todo MissionPlanSpecification getInstantiatedStart does not allow parameterization or reuse
//
//        for (Vertex vertex : vertexToEventSpecListMap.keySet()) {
//            ArrayList<ReflectedEventSpecification> events = vertexToEventSpecListMap.get(vertex);
//            for (ReflectedEventSpecification reflectedEventSpecification : events) {
//                Event e = reflectedEventSpecification.instantiate();
//                if (vertex instanceof Transition && e instanceof InputEvent) {
//                    ((Transition) vertex).addInputEvent((InputEvent) e);
//                } else if (vertex instanceof Place && e instanceof OutputEvent) {
//                    ((Place) vertex).addOutputEvent((OutputEvent) e);
//                } else {
//                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Have a mismatch of classes in vertexToEventSpecListMap!");
//                    System.exit(0);
//                }
//            }
//        }
//
//        for (Vertex v : graph.getVertices()) {
//            if (v instanceof Place && ((Place) v).isStart()) {
//                return (Place) v;
//            }
//        }
//        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "MissionPlanSpecification has no start state!");
//        System.exit(0);
//        return null;
//    }
    public boolean isInstantiated() {
        return isInstantiated;
    }

    public void instantiate(UUID missionId) {
        LOGGER.info("Instantiating mission: " + name + " with id: " + missionId);
        // @todo MissionPlanSpecification getInstantiatedStart does not allow parameterization or reuse
        for (Vertex vertex : vertexToEventSpecListMap.keySet()) {
            ArrayList<ReflectedEventSpecification> events = vertexToEventSpecListMap.get(vertex);
            for (ReflectedEventSpecification reflectedEventSpecification : events) {
                Event e = reflectedEventSpecification.instantiate();
                e.setMissionId(missionId);
                if (vertex instanceof Transition && e instanceof InputEvent) {
                    ((Transition) vertex).addInputEvent((InputEvent) e);
                } else if (vertex instanceof Place && e instanceof OutputEvent) {
                    ((Place) vertex).addOutputEvent((OutputEvent) e);
                } else {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Have a mismatch of classes in vertexToEventSpecListMap!");
                    System.exit(0);
                }
            }
        }
        isInstantiated = true;
    }

    /**
     * Updates our Graph and locations Hashtable from a Graph and Layout
     *
     * @param graph The new Graph object
     * @param layout The Layout to update our locations with
     */
    public void setGraph(Graph<Vertex, Edge> graph, AbstractLayout layout) {
        this.graph = graph;

        locations = new Hashtable<Vertex, Point2D>();

        for (Vertex v : graph.getVertices()) {
            locations.put(v, layout.transform(v));
        }
    }

    public void clearEventSpecList(Vertex vertex) {
        vertexToEventSpecListMap.put(vertex, new ArrayList<ReflectedEventSpecification>());
    }

    public void removeEventSpecList(Vertex vertex) {
        vertexToEventSpecListMap.remove(vertex);
    }

    public ArrayList<ReflectedEventSpecification> getEventSpecList(Vertex vertex) {
        return vertexToEventSpecListMap.get(vertex);
    }

    public void updateEventSpecList(Vertex vertex, ReflectedEventSpecification spec) {
        ArrayList<ReflectedEventSpecification> specList = vertexToEventSpecListMap.get(vertex);
        if (specList == null) {
            specList = new ArrayList<ReflectedEventSpecification>();
            vertexToEventSpecListMap.put(vertex, specList);
        }
        specList.add(spec);
    }

    public Map<Vertex, ArrayList<ReflectedEventSpecification>> getVertexToEventSpecListMap() {
        return vertexToEventSpecListMap;
    }

    public void clearTokenSpecList(Edge edge) {
        edgeToTokenSpecListMap.put(edge, new ArrayList<TokenSpecification>());
    }

    public void removeTokenSpecList(Edge edge) {
        edgeToTokenSpecListMap.remove(edge);
    }

    public ArrayList<TokenSpecification> getTokenSpecList(Edge edge) {
        return edgeToTokenSpecListMap.get(edge);
    }

    public void updateEdgeToTokenSpecListMap(Edge edge, TokenSpecification tokenSpec) {
        ArrayList<TokenSpecification> specList = edgeToTokenSpecListMap.get(edge);
        if (specList == null) {
            specList = new ArrayList<TokenSpecification>();
            edgeToTokenSpecListMap.put(edge, specList);
        }
        specList.add(tokenSpec);
    }

    public Map<Edge, ArrayList<TokenSpecification>> getEdgeToTokenSpecListMap() {
        return edgeToTokenSpecListMap;
    }

    public void setLayout(AffineTransform transform) {
        this.layoutTransform = transform;
    }

    public void setView(AffineTransform transform) {
        this.viewTransform = transform;
    }

    public AffineTransform getView() {
        return viewTransform;
    }

    public AffineTransform getLayoutTransform() {
        return layoutTransform;
    }

    /**
     * Updates a passed in Layout based on our Vertex locations object
     *
     * @param layout The Layout object to be updated
     */
    public void updateThisLayout(AbstractLayout<Vertex, Edge> layout) {
        for (Vertex v : locations.keySet()) {
            if (locations.get(v) != null) {
                layout.setLocation(v, locations.get(v));
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public Map<Vertex, Point2D> getLocations() {
        return locations;
    }

    public void updateAllTags() {
        for (Vertex vertex : graph.getVertices()) {
            vertex.updateTag();
        }
        for (Edge edge : graph.getEdges()) {
            edge.updateTag();
        }
    }

    private void readObject(ObjectInputStream ois) {
        try {
            ois.defaultReadObject();
            createTokenSpecLists();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
