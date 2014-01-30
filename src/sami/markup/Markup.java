package sami.markup;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author nbb
 */
public class Markup {

    // List of enum fields for which an enum option should be selected
    public static final ArrayList<String> enumFieldNames = new ArrayList<String>();
    // Description for each enum field
    public static final HashMap<String, String> enumNameToDescription = new HashMap<String, String>();
    // Mapping from enum value to the MarkupOption field it requires
    public static final HashMap<Enum, String> enumValueToFieldName = new HashMap<Enum, String>();

    public Markup() {
    }

    public Markup copy() {
        return new Markup();
    }

    public boolean equals(Object o) {
        if (o instanceof Markup) {
            boolean ret = o.getClass().getName().equalsIgnoreCase(this.getClass().getName());

            return ret;
        }
        return super.equals(o);
    }

    public String toString() {
        return this.getClass().getName();
    }
}
