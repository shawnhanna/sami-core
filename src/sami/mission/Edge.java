package sami.mission;

import java.io.IOException;
import sami.gui.GuiConfig;
import sami.mission.Vertex.FunctionMode;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author pscerri
 */
public class Edge implements java.io.Serializable {

    static final long serialVersionUID = 4L;
    protected String name = "";
    protected FunctionMode functionMode = null;
    protected GuiConfig.VisibilityMode visibilityMode = GuiConfig.VisibilityMode.Full;
    private Vertex startVertex, endVertex;
    // Store class names of Tokens used in this Edge for display purposes
    //  Originally an instance of the Token was stored, but this caused 
    //  serialization issues
    private ArrayList<String> tokenNames = new ArrayList<String>();

    transient String tag = "", shortTag = "";
    transient private ArrayList<Token> tokenRequirements = new ArrayList<Token>();

    public Edge(Vertex inVertex, Vertex outVertex, FunctionMode functionMode) {
        this.startVertex = inVertex;
        this.endVertex = outVertex;
        this.functionMode = functionMode;
    }

    public Vertex getEnd() {
        return endVertex;
    }

    public void setEnd(Vertex endVertex) {
        this.endVertex = endVertex;
    }

    public Vertex getStart() {
        return startVertex;
    }

    public void setStart(Vertex startVertex) {
        this.startVertex = startVertex;
    }

    /**
     * Called when reading in a spec to run a mission, not when creating the
     * mission in the GUI
     *
     * @param e
     */
    public void addTokenRequirement(Token token) {
        tokenRequirements.add(token);
    }

    public ArrayList<Token> getTokenRequirements() {
        return tokenRequirements;
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

    public String getTag() {
        return getTag(false);
    }

    public String getTag(boolean update) {
        if (update) {
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

    public void addTokenName(String className) {
        tokenNames.add(className);
        updateTag();
    }

    public void clearTokenNames() {
        tokenNames.clear();
        updateTag();
    }

    public void prepareForRemoval() {
        startVertex.removeOutEdge(this);
        endVertex.removeInEdge(this);
    }

    public void updateTag() {
        tag = "";
        shortTag = "";
        if (GuiConfig.DRAW_TOKEN_REQS && tokenNames.size() > 0) {
            tag += "<html>";
            shortTag += "<html>";
            for (String tokenName : tokenNames) {
                tag += "<font color=" + GuiConfig.TOKEN_REQ_TEXT_COLOR + ">" + tokenName + "</font><br>";
                shortTag += "<font color=" + GuiConfig.TOKEN_REQ_TEXT_COLOR + ">" + shorten(tokenName, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
            }
            tag += "</html>";
            shortTag += "</html>";
        }
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
        String ret = "Edge";
        if (startVertex.getName() != null && !startVertex.getName().equals("") && endVertex.getName() != null && !endVertex.getName().equals("")) {
            ret += ":" + startVertex.getName() + "\u21e8" + endVertex.getName();
        }
        return ret;
    }

    public Edge copy(HashMap<Vertex, Vertex> vertexMap) {
        Edge edge = new Edge(vertexMap.get(startVertex), vertexMap.get(endVertex), functionMode);
        return edge;
    }

    public Edge copyWithoutConnections() {
        Edge copy = new Edge(null, null, functionMode);
        copy.name = name;
        copy.visibilityMode = visibilityMode;
        copy.tokenNames = (ArrayList<String>) tokenNames.clone();
        copy.updateTag();
        return copy;
    }

    private void readObject(ObjectInputStream ois) {
        try {
            ois.defaultReadObject();
            tokenRequirements = new ArrayList<Token>();
            updateTag();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
