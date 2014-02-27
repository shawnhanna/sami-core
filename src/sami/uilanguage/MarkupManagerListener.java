package sami.uilanguage;

import sami.uilanguage.fromui.FromUiMessage;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author nbb
 */
public interface MarkupManagerListener {

    public void autonomyTriggered(ToUiMessage toMessage, FromUiMessage fromMessage);

}
