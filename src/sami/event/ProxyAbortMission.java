package sami.event;

import java.util.UUID;

/**
 *
 * @author nbb
 */
public class ProxyAbortMission extends OutputEvent {

    public ProxyAbortMission() {
        id = UUID.randomUUID();
    }

    public ProxyAbortMission(UUID missionId) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }
}
