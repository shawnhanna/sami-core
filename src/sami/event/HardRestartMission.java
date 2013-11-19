package sami.event;

import java.util.UUID;

/**
 *
 * @author nbb
 */
public class HardRestartMission extends OutputEvent {

    public HardRestartMission() {
        id = UUID.randomUUID();
    }
}
