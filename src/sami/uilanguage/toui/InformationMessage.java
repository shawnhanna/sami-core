package sami.uilanguage.toui;

import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.proxy.ProxyInt;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import sami.markup.Priority;

/**
 *
 * @author pscerri
 */
public class InformationMessage extends ToUiMessage implements java.io.Serializable {

    private String message = null;
    // Context 
    private MissionPlanSpecification missionContext = null;
    private Place placeContext = null;
    private Importance importance = null;

    /**
     * Tells the UI what type of message this is
     */
    public enum InformationMessageType {

        Change, Update, Misc
    };
    private InformationMessageType type = InformationMessageType.Misc;
    /**
     * The time that this message is relevant
     */
    private Date relevantTime = null;
    /**
     * Tells the UI what proxies this relates to
     */
    private ArrayList<ProxyInt> relevantProxies = null;
    private ArrayList<Importance> importances = null;

    public InformationMessage(UUID relevantOutputEventId, UUID missionId, int priority, String message) {
        this.relevantOutputEventId = relevantOutputEventId;
        this.missionId = missionId;
        this.priority = priority;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String toString() {
        return "<html>"
                + "<font color=rgb(188,6,6)>" + Priority.getPriority(priority).toString() + "</font>&nbsp;&nbsp;&nbsp;"
                + "<html><font color=rgb(0,0,0)>" + message + "</font>"
                + "</html>";
    }
}
