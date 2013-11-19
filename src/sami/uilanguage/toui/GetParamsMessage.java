package sami.uilanguage.toui;

import sami.event.ReflectedEventSpecification;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.UUID;

/**
 *
 * @author pscerri
 */
public class GetParamsMessage extends CreationMessage {

    protected final Hashtable<Field, ReflectedEventSpecification> fieldToEventSpec;

    public GetParamsMessage(UUID relevantOutputEventId, UUID missionId, int priority, Hashtable<Field, String> fieldDescriptions, Hashtable<Field, ReflectedEventSpecification> fieldToEventSpec) {
        super(relevantOutputEventId, missionId, priority, fieldDescriptions);
        this.fieldToEventSpec = fieldToEventSpec;
    }

    public Hashtable<Field, ReflectedEventSpecification> getFieldToEventSpec() {
        return fieldToEventSpec;
    }
}
