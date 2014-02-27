package sami.uilanguage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import sami.markup.Markup;

/**
 *
 * @author nbb
 */
public interface MarkupComponentWidget {

    public abstract int getCreationWidgetScore(Class creationClass, ArrayList<Markup> markups);

    public abstract int getSelectionWidgetScore(Object selectionObject, ArrayList<Markup> markups);

    public abstract int getMarkupScore(ArrayList<Markup> markups);

    public abstract MarkupComponentWidget addCreationWidget(MarkupComponent component, Class creationClass, ArrayList<Markup> markups);

    public abstract MarkupComponentWidget addSelectionWidget(MarkupComponent component, Object selectionObject, ArrayList<Markup> markups);

    public Object getComponentValue(Field field);

    public boolean setComponentValue(Object value);

    public abstract void handleMarkups(ArrayList<Markup> markups, MarkupManager manager);

    public abstract void disableMarkup(Markup markup);
}
