package sami.event;

import java.util.UUID;

/**
 *
 * @author nbb
 */
public class AbortMission extends OutputEvent {

    public AbortMission() {
        id = UUID.randomUUID();
    }

    public AbortMission(UUID missionId) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }
}
