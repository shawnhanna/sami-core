package sami.uilanguage.toui;

import java.util.List;

/**
 * Rename from Decision to Selection?
 *
 * @author nbb
 */
public abstract class SelectionMessage extends ToUiMessage {

    protected boolean allowMultiple;
    protected boolean allowRejection;
    protected boolean showOptionsIndividually;
    protected List<?> optionsList;

    public boolean getAllowMultiple() {
        return allowMultiple;
    }

    public boolean getAllowRejection() {
        return allowRejection;
    }

    public boolean getShowEachIndividually() {
        return showOptionsIndividually;
    }

    public List<?> getOptionsList() {
        return optionsList;
    }
}
