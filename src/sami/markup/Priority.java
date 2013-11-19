package sami.markup;

import java.util.Hashtable;
import java.util.Map;

/**
 *
 * @author nbb
 */
public class Priority extends Markup {

    public enum Ranking {

        LOW, MEDIUM, HIGH, CRITICAL
    };
    public static final Map<Ranking, Integer> priorityToInt;
    public static final Map<Integer, Ranking> intToPriority;

    static {
        priorityToInt = new Hashtable<Ranking, Integer>();
        priorityToInt.put(Ranking.LOW, new Integer(1));
        priorityToInt.put(Ranking.MEDIUM, new Integer(2));
        priorityToInt.put(Ranking.HIGH, new Integer(3));
        priorityToInt.put(Ranking.CRITICAL, new Integer(4));
        intToPriority = new Hashtable<Integer, Ranking>();
        intToPriority.put(priorityToInt.get(Ranking.LOW), Ranking.LOW);
        intToPriority.put(priorityToInt.get(Ranking.MEDIUM), Ranking.MEDIUM);
        intToPriority.put(priorityToInt.get(Ranking.HIGH), Ranking.HIGH);
        intToPriority.put(priorityToInt.get(Ranking.CRITICAL), Ranking.CRITICAL);
    }

    public Priority() {
    }

    public static int getPriority(Ranking priority) {
        return priorityToInt.get(priority).intValue();
    }

    public static Ranking getPriority(int priority) {
        return intToPriority.get(new Integer(priority));
    }
}
