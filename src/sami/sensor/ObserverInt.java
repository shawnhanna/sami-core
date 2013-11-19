package sami.sensor;

import sami.event.OutputEvent;

/**
 *
 * @author nbb
 */
public interface ObserverInt {

    public void addListener(ObservationListenerInt l);

    public void removeListener(ObservationListenerInt l);

    public void handleEvent(OutputEvent oe);
}
