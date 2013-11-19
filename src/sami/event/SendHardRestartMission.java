package sami.event;

import java.util.UUID;

/**
 *
 * @author nbb
 */
public class SendHardRestartMission extends OutputEvent {

    public SendHardRestartMission() {
        id = UUID.randomUUID();
    }

    public SendHardRestartMission(UUID missionId) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }
}
