package sami.engine;

import com.perc.mitpas.adi.mission.planning.task.ITask;
import com.perc.mitpas.adi.mission.planning.task.Task;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.config.DomainConfigManager;
import sami.handler.EventHandlerInt;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Token;
import sami.mission.TokenSpecification;
import sami.mission.TokenSpecification.TokenType;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyServerInt;
import sami.proxy.ProxyServerListenerInt;
import sami.sensor.ObserverInt;
import sami.sensor.ObserverServerInt;
import sami.sensor.ObserverServerListenerInt;
import sami.service.ServiceServer;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiServerInt;

/**
 *
 * @todo The Engine is keeping track of PlanManagers
 *
 * @author pscerri
 */
public class Engine implements ProxyServerListenerInt, ObserverServerListenerInt {

    private static final Logger LOGGER = Logger.getLogger(Engine.class.getName());
    private ArrayList<PlanManager> plans = new ArrayList<PlanManager>();
    private ArrayList<ProxyInt> proxies = new ArrayList<ProxyInt>();
    private ArrayList<ObserverInt> observers = new ArrayList<ObserverInt>();
    private ArrayList<Token> tokens = new ArrayList<Token>();
    private ArrayList<Token> proxyTokens = new ArrayList<Token>();
    private ArrayList<Token> taskTokens = new ArrayList<Token>();
    private ProxyServerInt proxyServer;
    private ObserverServerInt observerServer;
    private ServiceServer serviceServer;
    private sami.uilanguage.UiClientInt uiClient = null;
    private sami.uilanguage.UiServerInt uiServer = null;
    private ArrayList<PlanManagerListenerInt> planManagerListeners = new ArrayList<PlanManagerListenerInt>();
    private static final Object lock = new Object();
    private final Token genericToken = new Token("G", TokenType.MatchGeneric, null, null);
    private final Token noReqToken = new Token("No Req", TokenType.MatchNoReq, null, null);
    private final Token relProxyToken = new Token("RP", TokenType.MatchRelevantProxy, null, null);
    private final Token copyRelProxyToken = new Token("Copy RP", TokenType.CopyRelevantProxy, null, null);
    private final Token copyRelTaskToken = new Token("Copy RT", TokenType.CopyRelevantTask, null, null);
    private final Token noneToken = new Token("None", TokenType.TakeNone, null, null);
    private final Token takeAllToken = new Token("Take All", TokenType.TakeAll, null, null);
    private final Token takeGenericToken = new Token("Take G", TokenType.TakeGeneric, null, null);
    private final Token takeRelProxyToken = new Token("Take RP", TokenType.TakeRelevantProxy, null, null);
    private final Token takeRelTaskToken = new Token("Take RT", TokenType.TakeRelevantTask, null, null);
    private final Token addGenericToken = new Token("Add G", TokenType.AddGeneric, null, null);
    private final Token consumeGenericToken = new Token("Consume G", TokenType.ConsumeGeneric, null, null);
    private final Token consumeRelProxyToken = new Token("Consume RP", TokenType.ConsumeRelevantProxy, null, null);
    private final Token consumeRelTaskToken = new Token("Consume RT", TokenType.ConsumeRelevantTask, null, null);
    private final Token takeTaskToken = new Token("Take Task", TokenType.TakeTask, null, null);
    private final Token takeProxyToken = new Token("Take Proxy", TokenType.TakeProxy, null, null);
    // Lookup table used for retrieving proxy based tokens 
    private HashMap<ProxyInt, Token> proxyToToken = new HashMap<ProxyInt, Token>();
    // Lookup table used for retrieving task based tokens (ie for updating a token after a resource allocation is received)
    private HashMap<ITask, Token> taskToToken = new HashMap<ITask, Token>();
    private HashMap<TokenSpecification, Token> tokenSpecToToken = new HashMap<TokenSpecification, Token>();
    private HashMap<UUID, PlanManager> missionIdToPlanManager = new HashMap<UUID, PlanManager>();
    // Configuration of output events and the handler classes that will execute them
    private Hashtable<Class, EventHandlerInt> handlers = new Hashtable<Class, EventHandlerInt>();

    ;

    private static class EngineHolder {

        public static final Engine INSTANCE = new Engine();
    }

    private Engine() {
        for (String className : DomainConfigManager.getInstance().domainConfiguration.serverList) {
            try {
                Class serverClass = Class.forName(className);
                Object serverElement = serverClass.getConstructor(new Class[]{}).newInstance();
                if (serverElement instanceof ProxyServerInt) {
                    proxyServer = (ProxyServerInt) serverElement;
                    proxyServer.addListener(this);
                }
                if (serverElement instanceof ObserverServerInt) {
                    observerServer = (ObserverServerInt) serverElement;
                    observerServer.addListener(this);
                }
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            } catch (InstantiationException ie) {
                ie.printStackTrace();
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
            } catch (NoSuchMethodException nsme) {
                nsme.printStackTrace();
            } catch (InvocationTargetException ite) {
                ite.printStackTrace();
            }
        }
        if (proxyServer == null) {
            LOGGER.log(Level.SEVERE, "Failed to find Proxy Server in domain configuration!");
        }
        if (observerServer == null) {
            LOGGER.log(Level.SEVERE, "Failed to find Observer Server in domain configuration!");
        }

        Hashtable<String, String> handlerMapping = DomainConfigManager.getInstance().domainConfiguration.eventHandlerMapping;
        Class eventClass, handlerClass;
        EventHandlerInt handlerObject;
        HashMap<String, EventHandlerInt> handlerObjects = new HashMap<String, EventHandlerInt>();
        String handlerClassName;
        for (String ieClassName : handlerMapping.keySet()) {
            handlerClassName = handlerMapping.get(ieClassName);
            try {
                eventClass = Class.forName(ieClassName);
                handlerClass = Class.forName(handlerClassName);
                if (!handlerObjects.containsKey(handlerClassName)) {
                    // First use of this handler class, create an instance and add it to our hashmap
                    EventHandlerInt newHandlerObject = (EventHandlerInt) handlerClass.newInstance();
                    if (ProxyServerListenerInt.class.isInstance(newHandlerObject)) {
                        proxyServer.addListener((ProxyServerListenerInt) newHandlerObject);
                    }
                    handlerObjects.put(handlerClassName, newHandlerObject);
                }
                handlerObject = handlerObjects.get(handlerClassName);
                handlers.put(eventClass, handlerObject);
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        tokens.add(genericToken);
        tokens.add(noReqToken);
        tokens.add(relProxyToken);
        tokens.add(copyRelProxyToken);
        tokens.add(copyRelTaskToken);
        tokens.add(noneToken);
        tokens.add(takeAllToken);
        tokens.add(takeGenericToken);
        tokens.add(takeRelProxyToken);
        tokens.add(takeRelTaskToken);
        tokens.add(addGenericToken);
        tokens.add(consumeGenericToken);
        tokens.add(consumeRelProxyToken);
        tokens.add(consumeRelTaskToken);
        tokens.add(takeTaskToken);
        tokens.add(takeProxyToken);
    }

    public static Engine getInstance() {
        return EngineHolder.INSTANCE;
    }

    public void addListener(PlanManagerListenerInt planManagerListener) {
        synchronized (lock) {
            planManagerListeners.add(planManagerListener);
        }
    }

    public ProxyServerInt getProxyServer() {
        return proxyServer;
    }

    public ObserverServerInt getObserverServer() {
        return observerServer;
    }

    public PlanManager spawnMission(MissionPlanSpecification mSpec, final ArrayList<Token> parentMissionTokens) {
        if (mSpec != null) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Spawning mission from spec " + mSpec);

            UUID missionId = UUID.randomUUID();
            String planInstanceName = getUniquePlanName(mSpec.getName());
            MissionPlanSpecification mSpecInstance = new MissionPlanSpecification(mSpec);
            final PlanManager pm = new PlanManager(mSpecInstance, missionId, planInstanceName);
            // Add in already existing proxy tokens
            for (Token proxyToken : proxyTokens) {
                pm.addProxyToken(proxyToken);
            }
            plans.add(pm);
            missionIdToPlanManager.put(missionId, pm);

            ArrayList<PlanManagerListenerInt> listenersCopy;
            synchronized (lock) {
                listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
            }
            for (PlanManagerListenerInt listener : listenersCopy) {
                listener.planCreated(pm, mSpecInstance);
            }

            (new Thread() {
                public void run() {
                    pm.start(parentMissionTokens);
                }
            }).start();
            return pm;
        }
        return null;
    }

    public ServiceServer getServiceServer() {
        return serviceServer;
    }

    public void setServiceServer(ServiceServer serviceServer) {
        this.serviceServer = serviceServer;
        InputEventMapper.getInstance().setServiceServer(serviceServer);
    }

    public UiClientInt getUiClient() {
        return uiClient;
    }

    public void setUiClient(UiClientInt uiClient) {
        this.uiClient = uiClient;
    }

    public UiServerInt getUiServer() {
        return uiServer;
    }

    public void setUiServer(sami.uilanguage.UiServerInt uiServer) {
        this.uiServer = uiServer;
    }

    public void created(PlanManager planManager, MissionPlanSpecification spec) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planCreated(planManager, spec);
        }
    }

    public void started(PlanManager planManager) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planStarted(planManager);
        }
    }

    public void enterPlace(PlanManager planManager, Place p) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planEnteredPlace(planManager, p);
        }
    }

    public void leavePlace(PlanManager planManager, Place p) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planLeftPlace(planManager, p);
        }
    }

    public void done(PlanManager planManager) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planFinished(planManager);
        }
        plans.remove(planManager);
    }

    public void abort(PlanManager planManager) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planAborted(planManager);
        }
        plans.remove(planManager);
    }

    @Override
    public void proxyAdded(ProxyInt p) {
        proxies.add(p);
        Token proxyToken = createToken(p);
        for (PlanManager planManager : plans) {
            planManager.addProxyToken(proxyToken);
        }

//        Location waypoint = new Location(40.44515205369163, -80.01877404355538, 0);
//        ArrayList<Location> waypoints = new ArrayList<Location>();
//        waypoints.add(waypoint);
//        Path path = new PathUtm(waypoints);
//        p.setPath(path);
    }

    @Override
    public void proxyRemoved(ProxyInt p) {
        proxies.remove(p);
    }

    @Override
    public void observerAdded(ObserverInt p) {
        observers.add(p);
    }

    @Override
    public void observerRemoved(ObserverInt p) {
        observers.remove(p);
    }

    public EventHandlerInt getHandler(Class outputEventClass) {
        return handlers.get(outputEventClass);
    }

    public Token getGenericToken() {
        return genericToken;
    }

    public Token getNoReqToken() {
        return noReqToken;
    }

    public Token getRelProxyToken() {
        return relProxyToken;
    }

    public Token getCopyRelProxyToken() {
        return copyRelProxyToken;
    }

    public Token getCopyRelTaskToken() {
        return copyRelTaskToken;
    }

    public Token getNoneToken() {
        return noneToken;
    }

    public Token getTakeAllToken() {
        return takeAllToken;
    }

    public Token getTakeGenericToken() {
        return takeGenericToken;
    }

    public Token getTakeRelProxyToken() {
        return takeRelProxyToken;
    }

    public Token getTakeRelTaskToken() {
        return takeRelTaskToken;
    }

    public Token getAddGenericToken() {
        return addGenericToken;
    }

    public Token getConsumeGenericToken() {
        return consumeGenericToken;
    }

    public Token getConsumeRelProxyToken() {
        return consumeRelProxyToken;
    }

    public Token getConsumeRelTaskToken() {
        return consumeRelTaskToken;
    }

    public Token getTakeTaskToken() {
        return takeTaskToken;
    }

    public Token getTakeProxyToken() {
        return takeProxyToken;
    }

    public Token getToken(ProxyInt proxy) {
        return proxyToToken.get(proxy);
    }

    public Token getToken(Task task) {
        return taskToToken.get(task);
    }

    public Token getToken(TokenSpecification tSpec) {
        if (tSpec.getType() == TokenSpecification.TokenType.CopyRelevantProxy) {
            return copyRelProxyToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.CopyRelevantTask) {
            return copyRelTaskToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.MatchGeneric) {
            return genericToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.MatchNoReq) {
            return noReqToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.TakeNone) {
            return noneToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.MatchRelevantProxy) {
            return relProxyToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.TakeAll) {
            return takeAllToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.TakeGeneric) {
            return takeGenericToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.TakeRelevantProxy) {
            return takeRelProxyToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.TakeRelevantTask) {
            return takeRelTaskToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.AddGeneric) {
            return addGenericToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.ConsumeGeneric) {
            return consumeGenericToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.ConsumeRelevantProxy) {
            return consumeRelProxyToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.ConsumeRelevantTask) {
            return consumeRelTaskToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.TakeTask) {
            return takeTaskToken;
        } else if (tSpec.getType() == TokenSpecification.TokenType.TakeProxy) {
            return takeProxyToken;
        } else {
            return tokenSpecToToken.get(tSpec);
        }
    }

    public ArrayList<Token> getAllTokens() {
        return tokens;
    }

    public ArrayList<Token> getTaskTokens() {
        return taskTokens;
    }

    public Token createToken(ProxyInt proxy) {
        Token token = new Token(proxy.getProxyName(), TokenType.Proxy, proxy, null);
        proxyToToken.put(proxy, token);
        proxyTokens.add(token);
        tokens.add(token);
        return token;
    }

    public Token createToken(TokenSpecification tokenSpec) {
        Token token = null;
        try {
            if (tokenSpec.getType() == TokenSpecification.TokenType.Task) {
                Object task = Class.forName(tokenSpec.getTaskClassName()).newInstance();
                ((Task) task).setName(tokenSpec.getName());
                token = new Token(tokenSpec.getName(), tokenSpec.getType(), null, (Task) task);
                taskToToken.put((Task) task, token);
                tokenSpecToToken.put(tokenSpec, token);
                taskTokens.add(token);
                tokens.add(token);
                Logger.getLogger(this.getClass().getName()).log(Level.FINER, "\t\t\tCreated token " + token);
            }
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } catch (InstantiationException ie) {
            ie.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }
        return token;
    }

    public String getUniquePlanName(String planName) {
        boolean validName = false;
        while (!validName) {
            validName = true;
            for (PlanManager pm : plans) {
                if (pm.getPlanName().equals(planName)) {
                    int index = planName.length() - 1;
                    while (index >= 0 && (int) planName.charAt(index) >= (int) '0' && (int) planName.charAt(index) <= (int) '9') {
                        index--;
                    }
                    index++;
                    if (index == planName.length()) {
                        planName += '2';
                    } else {
                        int number = Integer.parseInt(planName.substring(index));
                        planName = planName.substring(0, index) + (number + 1);
                    }
                    validName = false;
                    break;
                }
            }
        }
        return planName;
    }
}
