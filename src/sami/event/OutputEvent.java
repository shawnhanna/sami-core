package sami.event;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 *
 * @author pscerri
 */
public class OutputEvent extends Event {

    private HashMap<String, Field> variableNameToField = null;

    public HashMap<String, Field> getVariables() {
        return variableNameToField;
    }

    public void addVariable(String variableName, Field field) {
        if (variableNameToField == null) {
            variableNameToField = new HashMap<String, Field>();
        }
        variableNameToField.put(variableName, field);
    }

    @Override
    public Object deepCopy() {
        OutputEvent copy = (OutputEvent) super.deepCopy();
        for (String key : variableNameToField.keySet()) {
            copy.variableNameToField.put(key, variableNameToField.get(key));
        }
        return copy;
    }
}
