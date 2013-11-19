package sami.uilanguage.toui;

import sami.allocation.ResourceAllocation;
import java.util.ArrayList;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class AllocationOptionsMessage extends ToUiMessage {

    ArrayList<ResourceAllocation> options;

    public AllocationOptionsMessage(ArrayList<ResourceAllocation> options, UUID uuid) {
        this.options = options;
        this.relevantOutputEventId = uuid;
    }

    public ArrayList<ResourceAllocation> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<ResourceAllocation> options) {
        this.options = options;
    }
}
