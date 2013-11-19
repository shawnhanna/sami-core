package sami.event;

import java.util.UUID;

/**
 *
 * @author nbb
 */
public class SoftRestartMission extends OutputEvent {

    public SoftRestartMission() {
        id = UUID.randomUUID();
    }

    public SoftRestartMission(UUID missionId) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }
}
