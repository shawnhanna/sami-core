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

    protected Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues;

    public ParamsSelectedMessage(UUID uuid, UUID missionUuid, Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues) {
        super(uuid, missionUuid, eventSpecToFieldValues);
    }
}
