package sami.event;

import sami.proxy.ProxyInt;
import java.util.ArrayList;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class ProxyAbortMissionReceived extends InputEvent {

    public ProxyAbortMissionReceived() {
        id = UUID.randomUUID();
    }

    public ProxyAbortMissionReceived(UUID missionId, ArrayList<ProxyInt> relevantProxyList) {
        this.missionId = missionId;
        this.relevantProxyList = relevantProxyList;
        id = UUID.randomUUID();
    }
}
