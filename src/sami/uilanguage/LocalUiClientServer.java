package sami.uilanguage;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.uilanguage.fromui.FromUiMessage;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author pscerri
 */
public class LocalUiClientServer implements UiClientInt, UiServerInt {

    // Implement client
    ArrayList<UiClientListenerInt> cls = new ArrayList<UiClientListenerInt>();
    ArrayList<UiServerListenerInt> sls = new ArrayList<UiServerListenerInt>();

    @Override
    public void addClientListener(UiClientListenerInt l) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Client listener added: " + l.getClass());
        cls.add(l);
    }

    @Override
    public void removeClientListener(UiClientListenerInt l) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Client listener removed: " + l.getClass());
        cls.remove(l);
    }

    @Override
    public void UIMessage(ToUiMessage m) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Message to UI: " + m);
        for (UiClientListenerInt clientListener : cls) {
            clientListener.ToUiMessage(m);
        }
    }

    @Override
    public void addServerListener(UiServerListenerInt l) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Server listener added: " + l.getClass());
        sls.add(l);
    }

    @Override
    public void removeServerListener(UiServerListenerInt l) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Server listener removed: " + l.getClass());
        sls.remove(l);
    }

    // Implement server
    @Override
    public void UIMessage(FromUiMessage m) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Message from UI: " + m);
        for (UiServerListenerInt serverListener : sls) {
            serverListener.FromUiMessage(m);
        }
    }
}
