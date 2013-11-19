package sami.event;

import java.util.UUID;

/**
 *
 * @author nbb
 */
public class SendSoftRestartMission extends OutputEvent {

    public SendSoftRestartMission() {
        id = UUID.randomUUID();
    }
    
    public SendSoftRestartMission(UUID missionId) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }
}
