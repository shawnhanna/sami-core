package sami.mission;

import sami.event.InputEvent;
import sami.event.ReflectedEventSpecification;
import sami.gui.GuiConfig;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import sami.markup.ReflectedMarkupSpecification;

/**
 *
 * @author pscerri
 */
public class Transition extends Vertex {

    static final long serialVersionUID = 5L;
    private ArrayList<Place> inPlaces = new ArrayList<Place>();
    private ArrayList<Place> outPlaces = new ArrayList<Place>();
    transient private ArrayList<InputEvent> inputEvents = new ArrayList<InputEvent>();
    transient private Hashtable<InputEvent, Boolean> inputEventStatus = new Hashtable<InputEvent, Boolean>();

    public Transition(String name, FunctionMode functionMode) {
        super(name, functionMode);
    }

    public void addInPlace(Place p) {
        inPlaces.add(p);
    }

    public void removeInPlace(Place p) {
        inPlaces.remove(p);
    }

    public ArrayList<Place> getInPlaces() {
        return inPlaces;
    }

    public void addOutPlace(Place p) {
        outPlaces.add(p);
    }

    public void removeOutPlace(Place p) {
        outPlaces.remove(p);
    }

    public ArrayList<Place> getOutPlaces() {
        return outPlaces;
    }

    /**
     * Called when reading in a spec to run a mission, not when creating the
     * mission in the GUI
     *
     * @param e
     */
    public void addInputEvent(InputEvent e) {
        inputEvents.add(e);
        inputEventStatus.put(e, new Boolean(false));
    }

    public boolean removeInputEvent(InputEvent e) {
        boolean t = inputEvents.remove(e);
        t = t && inputEventStatus.remove(e);
        return t;
    }

    public ArrayList<InputEvent> getInputEvents() {
        return inputEvents;
    }

    public boolean getInputEventStatus(InputEvent e) {
        if (inputEventStatus.containsKey(e)) {
            return inputEventStatus.get(e).booleanValue();
        }
        return false;
    }

    public void clearInputEventStatus() {
        inputEventStatus.clear();
    }

    public void setInputEventStatus(InputEvent e, boolean received) {
        inputEventStatus.put(e, new Boolean(received));
    }

    public void prepareForRemoval() {
        Vertex connection;
        // Remove edges ending here
        for (Edge edge : inEdges) {
            connection = edge.getStart();
            connection.removeOutEdge(edge);
        }
        // Remove edges starting here
        for (Edge edge : outEdges) {
            connection = edge.getEnd();
            connection.removeInEdge(edge);
        }
        // Remove connection to preceding places
        for (Place place : inPlaces) {
            place.removeOutTransition(this);
        }
        // Remove connection to following places
        for (Place place : outPlaces) {
            place.removeInTransition(this);
        }
    }

    public Shape getShape() {
        return new Rectangle(-10, -10, 20, 20);
    }

    @Override
    public void updateTag() {
        tag = "<html>";
        shortTag = "<html>";
        if (GuiConfig.DRAW_LABELS && name != null && !name.equals("")) {
            tag += "<font color=" + GuiConfig.LABEL_TEXT_COLOR + ">" + name + "</font><br>";
            shortTag += "<font color=" + GuiConfig.LABEL_TEXT_COLOR + ">" + shorten(name, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
        }
        if (GuiConfig.DRAW_EVENTS) {
            for (ReflectedEventSpecification eventSpec : eventSpecs) {
                try {
                    String className = eventSpec.getClassName();
                    Object eventInstance = Class.forName(className).newInstance();
                    String simpleName = eventInstance.getClass().getSimpleName();
                    if (eventInstance instanceof InputEvent) {
                        tag += "<font color=" + GuiConfig.INPUT_EVENT_TEXT_COLOR + ">I: " + simpleName + "</font><br>";
                        shortTag += "<font color=" + GuiConfig.INPUT_EVENT_TEXT_COLOR + ">I: " + shorten(simpleName, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
                    } else {
                        continue;
                    }
                } catch (ClassNotFoundException cnfe) {
                    cnfe.printStackTrace();
                } catch (InstantiationException ie) {
                    ie.printStackTrace();
                } catch (IllegalAccessException iae) {
                    iae.printStackTrace();
                }
                if (GuiConfig.DRAW_MARKUPS) {
                    for (ReflectedMarkupSpecification markupSpec : eventSpec.getMarkupSpecs()) {
                        tag += "<font color=" + GuiConfig.MARKUP_TEXT_COLOR + ">\tM: " + markupSpec.getClass().getSimpleName() + "</font><br>";
                        shortTag += "<font color=" + GuiConfig.MARKUP_TEXT_COLOR + ">\tM:  " + shorten(markupSpec.getClass().getSimpleName(), GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
                    }
                }
            }
        }
        tag += "</html>";
        shortTag += "</html>";
    }

    public String toString() {
        return "Transition:" + name;
    }

    public Transition copyWithoutConnections() {
        Transition copy = new Transition(name, functionMode);
        copy.visibilityMode = visibilityMode;
        for (ReflectedEventSpecification eventSpec : eventSpecs) {
            copy.eventSpecs.add(eventSpec.copySpecial());
        }
        copy.updateTag();
        return copy;
    }

    private void readObject(ObjectInputStream ois) {
        try {
            ois.defaultReadObject();
            inputEvents = new ArrayList<InputEvent>();
            inputEventStatus = new Hashtable<InputEvent, Boolean>();
            updateTag();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
