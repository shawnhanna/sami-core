package sami.uilanguage;

import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author pscerri
 */
public interface UiClientListenerInt {

    public void ToUiMessage(ToUiMessage m);

    public UiClientInt getUiClient();

    public void setUiClient(UiClientInt uiClient);

    public UiServerInt getUiServer();

    public void setUiServer(UiServerInt uiServer);
}
