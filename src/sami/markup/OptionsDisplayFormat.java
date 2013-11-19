package sami.markup;

/**
 *
 * @author nbb
 */
public class OptionsDisplayFormat extends Markup {

    public enum Format {

        SEQUENTIAL, TABBED, STACKED
    };
    private Format format;

    public OptionsDisplayFormat() {
    }
}
