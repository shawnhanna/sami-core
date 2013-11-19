
package sami.event;

import sami.proxy.ProxyInt;
import java.util.ArrayList;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class SendProxyAbortFutureMissions extends OutputEvent {

    public SendProxyAbortFutureMissions() {
        id = UUID.randomUUID();
    }

    public SendProxyAbortFutureMissions(UUID missionId) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }
}
