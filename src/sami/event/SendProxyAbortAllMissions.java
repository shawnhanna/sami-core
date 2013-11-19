package sami.event;

import java.util.UUID;

/**
 *
 * @author nbb
 */
public class SendProxyAbortAllMissions extends OutputEvent {

    public SendProxyAbortAllMissions() {
        id = UUID.randomUUID();
    }

    public SendProxyAbortAllMissions(UUID missionId) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }
}
