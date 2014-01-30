package sami.markup;

import sami.markupOption.Blink;

/**
 *
 * @author nbb
 */
public class Attention extends Markup {

    public enum AttentionType {

        HIGHLIGHT, BLINK
    };

    public enum AttentionEnd {

        ON_CLICK
    };

    public enum AttentionTarget {

        PANEL, FRAME, ALL_PROXIES, RELEVANT_PROXIES, SELECTED_PROXIES
    };
    // Fields
    public AttentionType attentionType;
    public AttentionEnd attentionEnd;
    public AttentionTarget attentionTarget;
    public Blink blink;

    static {
        enumFieldNames.add("attentionType");
        enumFieldNames.add("attentionEnd");
        enumFieldNames.add("attentionTarget");

        enumNameToDescription.put("attentionType", "How to draw attention?");
        enumNameToDescription.put("attentionEnd", "When to stop drawing attention?");
        enumNameToDescription.put("attentionTarget", "What to draw attention to?");

        enumValueToFieldName.put(AttentionType.BLINK, "blink");
    }

    public Attention() {
    }
}
