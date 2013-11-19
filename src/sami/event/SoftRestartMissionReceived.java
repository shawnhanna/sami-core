package sami.event;

import java.util.UUID;

/**
 *
 * @author nbb
 */
public class SoftRestartMissionReceived extends InputEvent {

    public SoftRestartMissionReceived() {
        id = UUID.randomUUID();
    }

    public SoftRestartMissionReceived(UUID missionId) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }
}
