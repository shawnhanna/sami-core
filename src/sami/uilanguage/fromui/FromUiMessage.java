package sami.uilanguage.fromui;

import java.util.UUID;

/**
 * Super class of all messages to UI
 *
 * @author pscerri
 */
public abstract class FromUiMessage {

    protected UUID relevantOutputEventId = null;
    protected UUID missionId = null;

    public UUID getMissionId() {
        return missionId;
    }

    public void setMissionId(UUID missionId) {
        this.missionId = missionId;
    }

    public UUID getRelevantOutputEventId() {
        return relevantOutputEventId;
    }

    public void setRelevantOutputEventId(UUID relevantOutputEventId) {
        this.relevantOutputEventId = relevantOutputEventId;
    }
}
