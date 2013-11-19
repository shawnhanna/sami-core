package sami.uilanguage.fromui;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public abstract class CreationDoneMessage extends FromUiMessage {

    public Hashtable<Field, Object> fieldToValue;

    public CreationDoneMessage(UUID uuid, UUID missionUuid, Hashtable<Field, Object> fieldToValue) {
        this.relevantOutputEventId = uuid;
        this.missionId = missionUuid;
        this.fieldToValue = fieldToValue;
    }

    public Hashtable<Field, Object> getFieldToValue() {
        return fieldToValue;
    }
}