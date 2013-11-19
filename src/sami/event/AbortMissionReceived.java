package sami.event;

import java.util.UUID;

/**
 *
 * @author nbb
 */
public class AbortMissionReceived extends InputEvent {

    public AbortMissionReceived() {
        id = UUID.randomUUID();
    }

    public AbortMissionReceived(UUID missionId) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }
}
