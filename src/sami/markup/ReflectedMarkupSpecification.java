package sami.markup;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author nbb
 */
public class ReflectedMarkupSpecification implements java.io.Serializable {

    private final static Logger LOGGER = Logger.getLogger(ReflectedMarkupSpecification.class.getName());
    static final long serialVersionUID = 0L;
    // Markup's variables to serialize
    protected HashMap<String, Object> fieldNameToDefinition = new HashMap<String, Object>();
    // Markup's class name
    protected final String className;

    public ReflectedMarkupSpecification(String className) {
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
