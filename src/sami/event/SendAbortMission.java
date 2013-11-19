package sami.event;

import java.util.UUID;

/**
 *
 * @author nbb
 */
public class SendAbortMission extends OutputEvent {

    public SendAbortMission() {
        id = UUID.randomUUID();
    }

    public SendAbortMission(UUID missionId) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }
}
