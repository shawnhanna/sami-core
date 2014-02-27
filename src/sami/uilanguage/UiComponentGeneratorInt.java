package sami.uilanguage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import sami.markup.Markup;

/**
 *
 * @author nbb
 */
public interface UiComponentGeneratorInt {

    public enum Type {

        CREATE, SELECT
    };

    public MarkupComponent getCreationComponent(Class objectClass, ArrayList<Markup> markupList);

    public MarkupComponent getSelectionComponent(Object object, ArrayList<Markup> markupList);

    public Object getComponentValue(MarkupComponent component, Field field);

    public boolean setComponentValue(MarkupComponent component, Object value);
}
