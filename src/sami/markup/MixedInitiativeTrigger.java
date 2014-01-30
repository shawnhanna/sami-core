package sami.markup;

/**
 *
 * @author nbb
 */
public class MixedInitiativeTrigger extends Markup {

    public enum Trigger {

        NEVER, TIMEOUT, PERCENTILE, POSITION, IMMEDIATELY
    };
    private double percentileThreshold = -1;
    private int positionThreshold = -1;
    private int timeout = -1;
    private Trigger trigger;

    public MixedInitiativeTrigger() {
    }
}
