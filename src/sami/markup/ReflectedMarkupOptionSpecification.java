package sami.markup;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author nbb
 */
public class ReflectedMarkupOptionSpecification implements java.io.Serializable {

    private final static Logger LOGGER = Logger.getLogger(ReflectedMarkupOptionSpecification.class.getName());
    static final long serialVersionUID = 0L;
    public static final String NONE = "@None";
    // Markup option's variables to serialize
    protected HashMap<String, Object> fieldNameToDefinition = new HashMap<String, Object>();
    // Markup option's class name
    protected final String className;

    public ReflectedMarkupOptionSpecification(String className) {
        this.className = className;
    }

    public HashMap<String, Object> getFieldDefinitions() {
        return fieldNameToDefinition;
    }

    public void setFieldDefinitions(HashMap<String, Object> fieldNameToObject) {
        fieldNameToDefinition = fieldNameToObject;
    }

    public void addFieldDefinition(String fieldName, Object fieldValue) {
        fieldNameToDefinition.put(fieldName, fieldValue);
    }

    public String getClassName() {
        return className;
    }

    public String toString() {
        return className.substring(className.lastIndexOf(".") + 1);
    }
}
