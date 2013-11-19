package sami.sensor;

import sami.event.InputEvent;

/**
 *
 * @author nbb
 */
public interface ObservationListenerInt {

    public void eventOccurred(InputEvent ie);

    public void newObservation(Observation o);
}
