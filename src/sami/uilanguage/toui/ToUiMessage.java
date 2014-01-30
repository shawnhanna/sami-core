package sami.uilanguage.toui;

import java.util.ArrayList;
import java.util.UUID;
import sami.markup.Markup;

/**
 * Super class of all messages to UI
 *
 * @author pscerri
 */
public abstract class ToUiMessage {
    
    protected int priority;
    protected ArrayList<Markup> markups = new ArrayList<Markup>();
    protected UUID relevantOutputEventId;
    protected UUID missionId;

    public UUID getMissionId() {
        return missionId;
    }

    public void setMissionId(UUID missionId) {
        this.missionId = missionId;
    }

    public ArrayList<Markup> getMarkups() {
        return markups;
    }

    public void setMarkups(ArrayList<Markup> markups) {
        this.markups = markups;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public UUID getRelevantOutputEventId() {
        return relevantOutputEventId;
    }

    public void setRelevantOutputEventId(UUID relevantOutputEventId) {
        this.relevantOutputEventId = relevantOutputEventId;
    }
}
