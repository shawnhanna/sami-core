package sami.uilanguage.fromui;

import sami.event.ReflectedEventSpecification;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.UUID;

/**
 *
 * @author pscerri
 */
public class ParamsSelectedMessage extends CreationDoneMessage {

    public Hashtable<Field, ReflectedEventSpecification> fieldToEventSpec;

    public ParamsSelectedMessage(UUID uuid, UUID missionUuid, Hashtable<Field, Object> fieldToValue, Hashtable<Field, ReflectedEventSpecification> fieldToEventSpec) {
        super(uuid, missionUuid, fieldToValue);
        this.fieldToEventSpec = fieldToEventSpec;
    }

    public Hashtable<Field, ReflectedEventSpecification> getFieldToEventSpec() {
        return fieldToEventSpec;
    }
}
