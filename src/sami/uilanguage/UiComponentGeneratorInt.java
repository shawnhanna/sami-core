package sami.uilanguage;

import java.lang.reflect.Field;
import javax.swing.JComponent;

/**
 *
 * @author nbb
 */
public interface UiComponentGeneratorInt {

    public enum Type {

        CREATE, SELECT
    };

    public JComponent getCreationComponent(Class objectClass);

    public JComponent getSelectionComponent(Object object);

    public Object getComponentValue(JComponent component, Field field);

    public boolean setComponentValue(Object value, JComponent component);
}
