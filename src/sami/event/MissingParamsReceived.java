package sami.event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class MissingParamsReceived extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    protected Hashtable<Field, Object> fieldToValue;
    protected Hashtable<Field, ReflectedEventSpecification> fieldToEventSpec;

    public MissingParamsReceived() {
        id = UUID.randomUUID();
    }

    public MissingParamsReceived(UUID relevantOutputEventId, UUID missionId, Hashtable<Field, Object> fieldToValue, Hashtable<Field, ReflectedEventSpecification> fieldToEventSpec) {
        this.relevantOutputEventId = relevantOutputEventId;
        this.missionId = missionId;
        this.fieldToValue = fieldToValue;
        this.fieldToEventSpec = fieldToEventSpec;
        id = UUID.randomUUID();
    }

    public Hashtable<Field, Object> getFieldValues() {
        return fieldToValue;
    }

    public Hashtable<Field, ReflectedEventSpecification> getFieldToEventSpec() {
        return fieldToEventSpec;
    }

    public String toString() {
        return "MissingParamsReceived: " + (fieldToValue != null ? fieldToValue.toString() : "null");
    }
}
