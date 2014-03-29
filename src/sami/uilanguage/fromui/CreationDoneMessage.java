package sami.uilanguage.fromui;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.ReflectedEventSpecification;

/**
 *
 * @author nbb
 */
public abstract class CreationDoneMessage extends FromUiMessage {

    protected Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues;

    public CreationDoneMessage(UUID uuid, UUID missionUuid, Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues) {
        this.relevantOutputEventId = uuid;
        this.missionId = missionUuid;
        this.eventSpecToFieldValues = eventSpecToFieldValues;
    }

    public Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> getEventSpecToFieldValues() {
        return eventSpecToFieldValues;
    }
}
