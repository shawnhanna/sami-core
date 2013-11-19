package sami.event;

import java.util.UUID;

/**
 *
 * @author nbb
 */
public class HardRestartMissionReceived extends InputEvent {

    public HardRestartMissionReceived(UUID missionId) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }
}
