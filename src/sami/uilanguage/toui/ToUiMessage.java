package sami.uilanguage.toui;

import java.util.UUID;

/**
 * Super class of all messages to UI
 *
 * @author pscerri
 */
public abstract class ToUiMessage {

    protected int priority;
    protected UUID relevantOutputEventId;
    protected UUID missionId;

    public UUID getMissionId() {
        return missionId;
    }

    public void setMissionId(UUID missionId) {
        this.missionId = missionId;
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
