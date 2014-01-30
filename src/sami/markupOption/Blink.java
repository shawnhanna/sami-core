package sami.markupOption;

import sami.markup.MarkupOption;

/**
 *
 * @author nbb
 */
public class Blink extends MarkupOption {

    // Fields
    public int cycleLength = 2000; // ms

    static {
        fieldNames.add("cycleLength");

        fieldNameToDescription.put("cycleLength", "Length of cycle (ms)");
    }

    public Blink() {
    }
}