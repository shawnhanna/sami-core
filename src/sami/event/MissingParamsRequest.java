package sami.event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;

public class MissingParamsRequest extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    protected final Hashtable<Field, String> FIELD_DESCRIPTIONS;
    protected final Hashtable<Field, ReflectedEventSpecification> FIELD_TO_EVENT_SPEC;

    public MissingParamsRequest(UUID missionId, Hashtable<Field, String> fieldDescriptions, Hashtable<Field, ReflectedEventSpecification> fieldToEventSpec) {
        this.missionId = missionId;
        this.FIELD_DESCRIPTIONS = fieldDescriptions;
        this.FIELD_TO_EVENT_SPEC = fieldToEventSpec;
        id = UUID.randomUUID();
    }

    public Hashtable<Field, String> getFieldDescriptions() {
        return FIELD_DESCRIPTIONS;
    }

    public Hashtable<Field, ReflectedEventSpecification> getFieldToEventSpec() {
        return FIELD_TO_EVENT_SPEC;
    }
}
