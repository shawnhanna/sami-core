package sami.markup;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author nbb
 */
public class Markup implements java.io.Serializable {

    static final long serialVersionUID = 1L;
    private ArrayList<Field> requiredFields = new ArrayList<Field>();
    private HashMap<Field, Object> fieldValues = new HashMap<Field, Object>();

    public Markup() {
    }

    public HashMap<Field, Object> getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(HashMap<Field, Object> fieldValues) {
        this.fieldValues = fieldValues;
    }

    public ArrayList<Field> getRequiredFields() {
        return requiredFields;
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
