package sami.event;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.markup.Markup;

/**
 * Stores a reflected InputEvent or OutputEvent classname and any parameters for
 * the event
 *
 * This is one of the key classes for mapping between DREAMM and SAMI
 *
 * The complexity is due to variables being passed back and forth and
 * instantiated at runtime
 *
 * @author pscerri
 */
public class ReflectedEventSpecification implements java.io.Serializable {

    private final static Logger LOGGER = Logger.getLogger(ReflectedEventSpecification.class.getName());
    public static final String NONE = "@None";
    static final long serialVersionUID = 0L;
    private static OutputStream testSink;
    private static ObjectOutputStream testStream;
    ArrayList<Markup> markups = new ArrayList<Markup>();
    // Serializable version of fieldNameToObjectInst using HashMaps and Strings to represent object
    HashMap<String, Object> fieldNameToSerialDefinition = new HashMap<String, Object>();
    String className = null;
    //@todo need to decide if we should have to handle non-serializable classes or not...making the below unneeded - right now we assume everything is serializable
    // Non-serializable lookup from field name to object representing its defined/undefined variable name or value
    transient HashMap<String, Object> fieldNameToTransDefinition = new HashMap<String, Object>();

    public ReflectedEventSpecification(String className) {
        this.className = className;
    }

    public void addVariablePrefix(String prefix) {
        //@todo what about sub-field hashmaps?
        HashMap<String, Object> newFieldNameToObject = new HashMap<String, Object>();
        for (String fieldName : fieldNameToTransDefinition.keySet()) {
            Object object = fieldNameToTransDefinition.get(fieldName);
            LOGGER.log(Level.FINEST, "Have <field, object>: " + fieldName + ", " + object);
            if (object != null && object instanceof String && ((String) object).startsWith("@") && !((String) object).equalsIgnoreCase(NONE)) {
                String newVariable = "@" + prefix + "." + ((String) object).substring(1);
                newFieldNameToObject.put(fieldName, newVariable);
            } else {
                newFieldNameToObject.put(fieldName, object);
            }
        }
        fieldNameToTransDefinition = newFieldNameToObject;
    }

    /**
     * Override the default serialization This is required because Fields are
     * not serializable
     *
     * @param os
     */
    private void writeObject(ObjectOutputStream os) {
        try {
            fieldNameToSerialDefinition = transientToSerial(fieldNameToTransDefinition);
            os.defaultWriteObject();
        } catch (IOException ex) {
            Logger.getLogger(ReflectedEventSpecification.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream ois) {
        try {
            ois.defaultReadObject();
            fieldNameToTransDefinition = serialToTransient(className, fieldNameToSerialDefinition);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Read failed: " + e, this);
            e.printStackTrace();
        }
    }

    private HashMap<String, Object> serialToTransient(String className, HashMap<String, Object> serialLookup) {
        HashMap<String, Object> transientLookup = new HashMap<String, Object>();
        for (String fieldName : serialLookup.keySet()) {
            transientLookup.put(fieldName, serialLookup.get(fieldName));
        }
        return transientLookup;
    }

    private HashMap<String, Object> transientToSerial(HashMap<String, Object> transientLookup) {
        //@todo need to decide if we should have to handle non-serializable classes or not - 
        //  this will handle some cases of non-serializable classes, but for the most part I 
        //  am just making the datamodel classes serializable
        HashMap<String, Object> serialLookup = new HashMap<String, Object>();
        for (String fieldName : transientLookup.keySet()) {
            Object object = transientLookup.get(fieldName);
            if (ReflectedEventSpecification.isSerializable(object)) {
                // If it is serializable, just copy the entry
                serialLookup.put(fieldName, object);
            } else {
                LOGGER.severe("Field named \"" + fieldName + "\" is not serializable!");
//                // If it is not serializable, make a hashmap containing each of the field's fields
//                HashMap<String, Object> subTransientLookup = new HashMap<String, Object>();
//                for (Field subField : object.getClass().getDeclaredFields()) {
//                    if (!subField.isAccessible()) {
//                        subField.setAccessible(true);
//                    }
//                    String subFieldName = subField.getName();
//                    try {
//                        subTransientLookup.put(subFieldName, subField.get(object));
//                    } catch (IllegalAccessException iae) {
//                        iae.printStackTrace();
//                    }
//                }
//                HashMap<String, Object> subSerialLookup = transientToSerial(subTransientLookup);
//                serialLookup.put(fieldName, subSerialLookup);
            }
        }
        return serialLookup;
    }

    public HashMap<String, Object> getFieldDefinitions() {
        return fieldNameToTransDefinition;
    }

    public void setFieldDefinitions(HashMap<String, Object> fieldNameToObject) {
        fieldNameToTransDefinition = fieldNameToObject;
    }

    public void addFieldDefinition(String fieldName, Object fieldValue) {
        fieldNameToTransDefinition.put(fieldName, fieldValue);
    }

    public ArrayList<Markup> getMarkups() {
        return markups;
    }

    public void setMarkups(ArrayList<Markup> markups) {
        this.markups = markups;
    }

    public String getClassName() {
        return className;
    }

    public boolean hasMissingParams(boolean atPlace) {
        //@todo this is ugly
        try {
            Class c = Class.forName(className);
            Event event = (Event) c.newInstance();
            LOGGER.log(Level.FINE, "Event " + event.getClass().getSimpleName() + ", event.getFillAtPlace() = " + event.getFillAtPlace());
            if (event.getFillAtPlace() && !atPlace) {
                LOGGER.log(Level.INFO, "Tried to check for missing params for event " + event.getClass().getSimpleName() + " at plan loading, but params are to be filled at place, returning false");
                return false;
            } else {
                LOGGER.log(Level.FINE, "Checking for missing params for event " + event.getClass().getSimpleName());
                for (String fieldName : fieldNameToTransDefinition.keySet()) {
                    if (fieldNameToTransDefinition.get(fieldName) == null) {
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to instantiate event: " + className + " due to " + ex, this);
            ex.printStackTrace();
        }
        return false;
    }

    public String toString() {
        return className.substring(className.lastIndexOf(".") + 1);
    }

    public ArrayList<Field> getRequiredFields() {
        ArrayList<Field> fields = new ArrayList<Field>();
        try {
            Class c = Class.forName(className);
            LOGGER.log(Level.FINE, "Looking for fields of " + c + " " + c.getDeclaredFields());
            for (Field field : c.getDeclaredFields()) {
                fields.add(field);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Problem getting fields for " + className + ", Exception: " + e);
        }
        return fields;
    }

    /**
     * Return an instance of the stored Event class with values set to those
     * stored in fieldNameToTransObject
     *
     * @return event
     */
    public Event instantiate() {
        LOGGER.log(Level.FINE, "Instantiate event called for " + className, this);
        Event event = null;
        try {
            Class c = Class.forName(className);
            event = (Event) c.newInstance();

            if (event != null) {
                instantiate(event, fieldNameToTransDefinition);
            } else {
                LOGGER.log(Level.SEVERE, "Creation of instance failed for " + className, this);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to instantiate event: " + className + " due to " + ex, this);
            ex.printStackTrace();
        }

        return event;
    }

    private void instantiate(Event event, HashMap<String, Object> fieldNameToObject) {
        for (Field field : event.getClass().getDeclaredFields()) {
            Object definition = fieldNameToObject.get(field.getName());
            if (definition != null) {
                try {
                    // Earlier we had to replace primitive fields with their wrapper object, undo that here
                    if (definition.getClass().equals(String.class) && ((String) definition).startsWith("@")) {
                        // This is a variable name
                        if (event instanceof InputEvent) {
                            ((InputEvent) event).addVariable(field.getName(), (String) definition);
                        }
                        if (event instanceof OutputEvent) {
                            ((OutputEvent) event).addVariable((String) definition, field);
                        }
                    } else if (field.getType().equals(double.class) && definition.getClass().equals(Double.class)) {
                        field.setDouble(event, ((Double) definition).doubleValue());
                    } else if (field.getType().equals(float.class) && definition.getClass().equals(Float.class)) {
                        field.setFloat(event, ((Float) definition).floatValue());
                    } else if (field.getType().equals(int.class) && definition.getClass().equals(Integer.class)) {
                        field.setInt(event, ((Integer) definition).intValue());
                    } else if (field.getType().equals(long.class) && definition.getClass().equals(Long.class)) {
                        field.setLong(event, ((Long) definition).longValue());
                    } else {
                        field.set(event, definition);
//                        field.set(event, new Area2D(new ArrayList<Location>()));
//                        field.set(event, new Area2D(((Area2D)fieldNameToObject.get(definition)).getPoints()));
                        if (definition != null && field.get(event) == null) {
                            LOGGER.log(Level.SEVERE, "Instantiation of field " + field.getName() + " on Event " + event.toString() + " failed!");
                        }
                    }
                } catch (IllegalAccessException iae) {
                    iae.printStackTrace();
                }
            }
        }
    }

    public ReflectedEventSpecification copySpecial() {
        ReflectedEventSpecification copy = new ReflectedEventSpecification(className);
        // Need to set variableHolderField or instanceParams?
        if (fieldNameToTransDefinition != null) {
            copy.fieldNameToTransDefinition = new HashMap<String, Object>();
            for (String key : fieldNameToTransDefinition.keySet()) {
                copy.fieldNameToTransDefinition.put(key, fieldNameToTransDefinition.get(key));
            }
        }
        return copy;
    }

    public static boolean isSerializable(final Object o) {
        boolean ret = false;
        try {
            if (o == null) {
                ret = true;
            } else if (o.getClass().isPrimitive()) {
                ret = true;
            } else if (o instanceof Serializable || o instanceof Externalizable) {
                if (testSink == null) {
                    testSink = new ByteArrayOutputStream();
                }
                testStream = new ObjectOutputStream(testSink);
                testStream.writeObject(o);
                // If we are here, there were no exceptions and this is serializable
                ret = true;
            }
        } catch (IOException io) {
            //io.printStackTrace();
        }
        // Shut down stream
        if (testStream != null) {
            try {
                testStream.close();
            } catch (IOException ex) {
            }
        }
        return ret;
    }
}
