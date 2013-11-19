package sami.uilanguage.toui;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.UUID;

/**
 * @author nbb
 */
public abstract class CreationMessage extends ToUiMessage {

    protected final Hashtable<Field, String> fieldDescriptions;

    public CreationMessage(UUID relevantOutputEventId, UUID missionId, int priority, Hashtable<Field, String> fieldDescriptions) {
        this.relevantOutputEventId = relevantOutputEventId;
        this.missionId = missionId;
        this.priority = priority;
        this.fieldDescriptions = fieldDescriptions;
    }

    public Hashtable<Field, String> getFieldDescriptions() {
        return fieldDescriptions;
    }
}
