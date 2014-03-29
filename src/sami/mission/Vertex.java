package sami.mission;

import java.util.ArrayList;
import sami.event.ReflectedEventSpecification;
import sami.gui.GuiConfig;

/**
 *
 * @author pscerri
 */
public class Vertex implements java.io.Serializable {

    public enum FunctionMode {

        Nominal, Recovery, HiddenRecovery
    };
    static final long serialVersionUID = 4L;
    protected String name = "";
    protected FunctionMode functionMode = null;
    protected GuiConfig.VisibilityMode visibilityMode = GuiConfig.VisibilityMode.Full;
    protected ArrayList<Edge> inEdges = new ArrayList<Edge>();
    protected ArrayList<Edge> outEdges = new ArrayList<Edge>();
    protected ArrayList<ReflectedEventSpecification> eventSpecs = new ArrayList<ReflectedEventSpecification>();
    transient protected String tag = "", shortTag = "";
    transient protected boolean beingModified = false;

    public Vertex(String name, FunctionMode functionMode) {
        this.name = name;
        this.functionMode = functionMode;
        tag = name;
        shortTag = shorten(name, GuiConfig.MAX_STRING_LENGTH);
    }

    public void addInEdge(Edge edge) {
        inEdges.add(edge);
    }

    public boolean removeInEdge(Edge edge) {
        return inEdges.remove(edge);
    }

    public ArrayList<Edge> getInEdges() {
        return inEdges;
    }

    public void addOutEdge(Edge edge) {
        outEdges.add(edge);
    }

    public boolean removeOutEdge(Edge edge) {
        return outEdges.remove(edge);
    }

    public ArrayList<Edge> getOutEdges() {
        return outEdges;
    }

    public FunctionMode getFunctionMode() {
        return functionMode;
    }

    public void setFunctionMode(FunctionMode functionMode) {
        this.functionMode = functionMode;
    }

    public GuiConfig.VisibilityMode getVisibilityMode() {
        return visibilityMode;
    }

    public void setVisibilityMode(GuiConfig.VisibilityMode visibilityMode) {
        this.visibilityMode = visibilityMode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        updateTag();
    }

    public String getTag() {
        return getTag(false);
    }

    public String getTag(boolean updateTag) {
        if (updateTag) {
            updateTag();
        }
        return tag;
    }

    public String getShortTag() {
        return getShortTag(false);
    }

    public String getShortTag(boolean update) {
        if (update) {
            updateTag();
        }
        return shortTag;
    }

    public void addEventSpec(ReflectedEventSpecification eventSpec) {
        eventSpecs.add(eventSpec);
        updateTag();
    }

    public ArrayList<ReflectedEventSpecification> getEventSpecs() {
        return eventSpecs;
    }

    public void setEventSpecs(ArrayList<ReflectedEventSpecification> eventSpecs) {
        this.eventSpecs = eventSpecs;
        updateTag();
    }

    public void prepareForRemoval() {
        // Remove each edge
        ArrayList<Edge> edgeList = (ArrayList<Edge>)inEdges.clone();
        for(Edge inEdge : edgeList) {
            inEdge.prepareForRemoval();
        }
        edgeList = (ArrayList<Edge>)outEdges.clone();
        for(Edge outEdge : edgeList) {
            outEdge.prepareForRemoval();
        }
    }

    public void updateTag() {
        if (this instanceof Place) {
            ((Place) this).updateTag();
        } else if (this instanceof Transition) {
            ((Transition) this).updateTag();
        }
    }

    public boolean getBeingModified() {
        return beingModified;
    }

    public void setBeingModified(boolean beingModified) {
        this.beingModified = beingModified;
    }

    public String shorten(String full, int maxLength) {
        String reduced = "";
        int upperCount = 0;
        for (char c : full.toCharArray()) {
            if (Character.isUpperCase(c) || c == '.') {
                upperCount++;
            }
        }
        int charPerUpper = maxLength / Math.max(1, upperCount); // prevent divide by 0
        int lowerCaseAfterUpperCount = 0;
        for (int i = 0; i < full.length(); i++) {
            if (Character.isUpperCase(full.charAt(i)) || full.charAt(i) == '.') {
                reduced += full.charAt(i);
                lowerCaseAfterUpperCount = 0;
            } else if (lowerCaseAfterUpperCount < charPerUpper) {
                reduced += full.charAt(i);
                lowerCaseAfterUpperCount++;
            }
        }
        return reduced;
    }

    public String toString() {
        return "Vertex: " + name;
    }
}