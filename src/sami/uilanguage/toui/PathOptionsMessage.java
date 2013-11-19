package sami.uilanguage.toui;

import sami.path.Path2D;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class PathOptionsMessage extends ToUiMessage {

    List<Path2D> options;
    Path2D variableToWrite = null;

    public PathOptionsMessage(List<Path2D> options, UUID uuid) {
        this.options = options;
        this.relevantOutputEventId = uuid;
    }

    public List<Path2D> getOptions() {
        return options;
    }

    public void setOptions(List<Path2D> options) {
        this.options = options;
    }
}
