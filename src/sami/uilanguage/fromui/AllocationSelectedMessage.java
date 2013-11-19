package sami.uilanguage.fromui;

import sami.allocation.ResourceAllocation;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class AllocationSelectedMessage extends FromUiMessage {

    private ResourceAllocation allocation;

    public AllocationSelectedMessage(UUID relevantOutputEventId, UUID missionId, ResourceAllocation allocation) {
        this.relevantOutputEventId = relevantOutputEventId;
        this.missionId = missionId;
        this.allocation = allocation;
    }

    public ResourceAllocation getAllocation() {
        return allocation;
    }

    public void setAllocation(ResourceAllocation allocation) {
        this.allocation = allocation;
    }
}
