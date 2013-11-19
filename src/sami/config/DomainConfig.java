package sami.config;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 *
 * @author nbb
 */
public class DomainConfig implements java.io.Serializable {

    private static final Logger LOGGER = Logger.getLogger(DomainConfig.class.getName());
    static final long serialVersionUID = 1L;
    /*
     * Reads in two types of files: Lists and mappings
     *   - Listings: Format is 
     *       "<java class path>" "<tool text>"
     *       carots (^) may be used to create folder hierarchies for lists that are presented to the user (ex: events and markups)
     *   - Mappings:
     *       "<java class path> <java class path>"
     *       (ex: "events.output.proxy.ProxyExecutePath handlers.ProxyEventHandler" in handlers has 
     *       ProxyExecutePath handled by ProxyEventHandler)
     * 
     * A complete domain configuration file needs to read in the following:
     *   - Agents Listing: All DREAAM agents used for helping or checking mission plans
     *   - Assets Listing: All AbstractAsset implementations
     *   - Component generator Listing: Takes the first class that implements UiComponentGeneratorInterface
     *   - Events Listing: All input and output event class paths
     *   - Event handlers Mapping: Mapping from output event class path to handler class path
     *   - Markup Listing: All SA and MI operator markup class paths
     *   - Server Listing: ProxyServerInterface and ObserverServerInterface implementations
     *   - Task Listing: All domain tasks that can be used to create a task token in DREAAM
     *   - UI component Listing: All UiClientListener, UiServerListener, and UiFrame implementations
     *       Used to set the UiClient and UiServer for UiClientListeners and UiServerListers,
     *       and JFrame for UiFrames
     */
    public boolean complete;
    public String domainName;
    public String domainDescription;
    public DefaultMutableTreeNode agentTree;
    public String agentTreeFilePath;
    public DefaultMutableTreeNode assetTree;
    public String assetTreeFilePath;
    public ArrayList<String> componentGeneratorList;
    public String componentGeneratorListFilePath;
    public DefaultMutableTreeNode eventTree;
    public String eventTreeFilePath;
    public Hashtable<String, String> eventHandlerMapping;
    public String eventHandlerMappingFilePath;
    public DefaultMutableTreeNode markupTree;
    public String markupTreeFilePath;
    public ArrayList<String> serverList;
    public String serverListFilePath;
    public DefaultMutableTreeNode taskTree;
    public String taskTreeFilePath;
    public ArrayList<String> uiList;
    public String uiListFilePath;

    public DomainConfig() {
        complete = false;
    }

    public DomainConfig(String domainName,
            String domainDescription,
            String agentListFilePath,
            String asssetListFilePath,
            String componentGeneratorListFilePath,
            String eventListFilePath,
            String eventHandlerMappingFilePath,
            String markupListFilePath,
            String serverListFilePath,
            String taskListFilePath,
            String uiListFilePath) {
        this.domainName = domainName;
        this.domainDescription = domainDescription;
        this.agentTreeFilePath = agentListFilePath;
        this.assetTreeFilePath = asssetListFilePath;
        this.componentGeneratorListFilePath = componentGeneratorListFilePath;
        this.eventTreeFilePath = eventListFilePath;
        this.eventHandlerMappingFilePath = eventHandlerMappingFilePath;
        this.markupTreeFilePath = markupListFilePath;
        this.serverListFilePath = serverListFilePath;
        this.taskTreeFilePath = taskListFilePath;
        this.uiListFilePath = uiListFilePath;
        complete = false;
        reload();
    }

    public void reload() {
        if (agentTreeFilePath != null) {
            File file = new File(agentTreeFilePath);
            if (file != null) {
                agentTree = parseTreeFile(file);
            } else {
                LOGGER.warning("Could not open agent list file path: \"" + agentTreeFilePath + "\"");
            }
        } else {
            LOGGER.warning("Agent list file path is not set");
        }
        if (assetTreeFilePath != null) {
            File file = new File(assetTreeFilePath);
            if (file != null) {
                assetTree = parseTreeFile(file);
            } else {
                LOGGER.warning("Could not open asset list file path: \"" + assetTreeFilePath + "\"");
            }
        } else {
            LOGGER.warning("Asset list file path is not set");
        }
        if (componentGeneratorListFilePath != null) {
            File file = new File(componentGeneratorListFilePath);
            if (file != null) {
                componentGeneratorList = parseListFile(file);
            } else {
                LOGGER.warning("Could not open component generator list file path: \"" + componentGeneratorListFilePath + "\"");
            }
        } else {
            LOGGER.warning("Component generator file path is not set");
        }
        if (eventTreeFilePath != null) {
            File file = new File(eventTreeFilePath);
            if (file != null) {
                eventTree = parseTreeFile(file);
            } else {
                LOGGER.warning("Could not open event list file path: \"" + eventTreeFilePath + "\"");
            }
        } else {
            LOGGER.warning("Event list file path is not set");
        }
        if (eventHandlerMappingFilePath != null) {
            File file = new File(eventHandlerMappingFilePath);
            if (file != null) {
                eventHandlerMapping = parseMappingFile(file);
            } else {
                LOGGER.warning("Could not open event handler mapping file path: \"" + eventHandlerMappingFilePath + "\"");
            }
        } else {
            LOGGER.warning("Event handler mapping file path is not set");
        }
        if (markupTreeFilePath != null) {
            File file = new File(markupTreeFilePath);
            if (file != null) {
                markupTree = parseTreeFile(file);
            } else {
                LOGGER.warning("Could not open markup list file path: \"" + markupTreeFilePath + "\"");
            }
        } else {
            LOGGER.warning("Markup list file path is not set");
        }
        if (serverListFilePath != null) {
            File file = new File(serverListFilePath);
            if (file != null) {
                serverList = parseListFile(file);
            } else {
                LOGGER.warning("Could not open server list file path: \"" + serverListFilePath + "\"");
            }
        } else {
            LOGGER.warning("Server list file path is not set");
        }
        if (taskTreeFilePath != null) {
            File file = new File(taskTreeFilePath);
            if (file != null) {
                taskTree = parseTreeFile(file);
            } else {
                LOGGER.warning("Could not open task list file path: \"" + taskTreeFilePath + "\"");
            }
        } else {
            LOGGER.warning("Task list file path is not set");
        }
        if (uiListFilePath != null) {
            File file = new File(uiListFilePath);
            if (file != null) {
                uiList = parseListFile(file);
            } else {
                LOGGER.warning("Could not open ui list file path: \"" + uiListFilePath + "\"");
            }
        } else {
            LOGGER.warning("Ui list file path is not set");
        }

        // Do we have values for all trees/lists/mappings?
        if (agentTree != null
                && assetTree != null
                && componentGeneratorList != null
                && eventHandlerMapping != null
                && eventTree != null
                && markupTree != null
                && serverList != null
                && taskTree != null
                && uiList != null) {
            complete = true;
        } else {
            complete = false;
        }
    }

    private ArrayList<String> parseListFile(File listFile) {
        ArrayList<String> list = new ArrayList<String>();
        boolean success;
        Pattern pattern = Pattern.compile("\\s*\"[^\\r\\n\"]*\"\\s*");
        try {
            FileInputStream fstream = new FileInputStream(listFile);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                success = false;
                line = line.trim();
                if (line.matches(pattern.toString())) {
                    int quote1 = line.indexOf("\"");
                    int quote2 = line.indexOf("\"", quote1 + 1);
                    list.add(line.substring(quote1 + 1, quote2));
                    success = true;
                } else if (line.length() == 0) {
                    success = true;
                }
                if (!success) {
                    LOGGER.warning("Could not parse line from list file: " + listFile.getAbsolutePath() + ": " + line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return list;
    }

    private Hashtable<String, String> parseMappingFile(File mappingFile) {
        Hashtable<String, String> mapping = new Hashtable<String, String>();
        boolean success;
        Pattern pattern = Pattern.compile("\\s*\"[^\\r\\n\"]*\"\\s*\"[^\\r\\n\"]*\"\\s*");
        try {
            FileInputStream fstream = new FileInputStream(mappingFile);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                success = false;
                line = line.trim();
                if (line.matches(pattern.toString())) {
                    int quote1 = line.indexOf("\"");
                    int quote2 = line.indexOf("\"", quote1 + 1);
                    int quote3 = line.indexOf("\"", quote2 + 1);
                    int quote4 = line.indexOf("\"", quote3 + 1);
                    mapping.put(line.substring(quote1 + 1, quote2), line.substring(quote3 + 1, quote4));
                    success = true;
                } else if (line.length() == 0) {
                    success = true;
                }
                if (!success) {
                    LOGGER.warning("Could not parse line from mapping file: " + mappingFile.getAbsolutePath() + ": " + line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return mapping;
    }

    public DefaultMutableTreeNode parseTreeFile(File treeFile) {
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode();
        boolean success;
        int currentLevel = -1, tabCount;
        DefaultMutableTreeNode parentNode = treeRoot;
        DefaultMutableTreeNode currentNode = treeRoot;
        Pattern categoryPattern = Pattern.compile(">*\\s*\"[^\\r\\n\"]*\"\\s*");
        Pattern classPattern = Pattern.compile(">*\\s*\"[^\\r\\n\"]*\"\\s*\"[^\\r\\n\"]*\"\\s*\"[^\\r\\n\"]*\"\\s*");
        try {
            FileInputStream fstream = new FileInputStream(treeFile);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                success = false;
                line = line.trim();
                if (line.matches(categoryPattern.toString())) {
                    tabCount = 0;
                    while (line.charAt(tabCount) == '>' && tabCount < line.length()) {
                        tabCount++;
                    }
                    int quote1 = line.indexOf("\"");
                    int quote2 = line.indexOf("\"", quote1 + 1);
                    String categoryName = line.substring(quote1 + 1, quote2);
                    if (tabCount == currentLevel && parentNode != null) {
                        // Sibling category
                        // This is a sibling of the current "category" node
                        // First check that our current "category" node has children "events"
                        if (currentNode != null && currentNode.getChildCount() == 0) {
                            // Remove it or the selection dialog won't work
                            currentNode.removeFromParent();
                        }
                        currentNode = new DefaultMutableTreeNode(categoryName);
                        parentNode.add(currentNode);
                        currentLevel = tabCount;
                        success = true;
                    } else if (tabCount < currentLevel && parentNode != null) {
                        // Parent category
                        // This is a n-generation parent of the current "category" node
                        while (tabCount < currentLevel && parentNode != null) {
                            // First check that our current "category" node has children "events"
                            if (currentNode.getChildCount() == 0) {
                                // Remove it or the selection dialog won't work
                                currentNode.removeFromParent();
                            }
                            currentNode = parentNode;
                            parentNode = (DefaultMutableTreeNode) parentNode.getParent();
                            currentLevel--;
                            if (tabCount == currentLevel) {
                                if (currentNode.getChildCount() == 0) {
                                    currentNode.removeFromParent();
                                }
                                currentNode = new DefaultMutableTreeNode(categoryName);
                                parentNode.add(currentNode);
                                success = true;
                            }
                        }
                        currentLevel = tabCount;
                    } else if (tabCount == currentLevel + 1 && currentNode != null) {
                        // Child category
                        parentNode = currentNode;
                        currentNode = new DefaultMutableTreeNode(categoryName);
                        parentNode.add(currentNode);
                        currentLevel = tabCount;
                        success = true;
                    }
                } else if (line.matches(classPattern.toString()) && currentNode != null) {
                    int quote1 = line.indexOf("\"");
                    int quote2 = line.indexOf("\"", quote1 + 1);
                    int quote3 = line.indexOf("\"", quote2 + 1);
                    int quote4 = line.indexOf("\"", quote3 + 1);
                    int quote5 = line.indexOf("\"", quote4 + 1);
                    int quote6 = line.indexOf("\"", quote5 + 1);
                    LeafNode node = new LeafNode(line.substring(quote1 + 1, quote2), line.substring(quote3 + 1, quote4), line.substring(quote5 + 1, quote6));
                    currentNode.add(node);
                    success = true;
                } else if (line.length() == 0) {
                    success = true;
                }
                if (!success) {
                    LOGGER.warning("Could not parse line from tree file: " + treeFile.getAbsolutePath() + ": " + line + ": " + line.length());
                }
            }
            // Done loading, finish cleaning up empty category nodes
            while (currentLevel >= 0 && currentNode != null) {
                // First check that our current "category" node has children "events"
                if (currentNode.getChildCount() == 0) {
                    // Remove it or the selection dialog won't work
                    currentNode.removeFromParent();
                }
                currentNode = parentNode;
                parentNode = (DefaultMutableTreeNode) parentNode.getParent();
                currentLevel--;
            }
        } catch (IOException ioe) {
            LOGGER.warning("Reading file failed: " + treeFile.getAbsolutePath());
        }

        return treeRoot;
    }

    public static String printTree(DefaultMutableTreeNode treeRoot) {
        String ret = "";
        for (int i = 0; i < treeRoot.getChildCount(); i++) {
            ret += printNode(treeRoot.getChildAt(i), treeRoot, 1);
        }
        return ret;
    }

    public static String printNode(TreeNode node, DefaultMutableTreeNode parent, int generation) {
        String ret = "";
        if (node instanceof LeafNode) {
            // At a event, add as CheckBox and return
            LeafNode leafNode = (LeafNode) node;
            for (int i = 0; i < generation; i++) {
                ret += "\t";
            }
            ret += leafNode.displayName + "\n";
        } else if (node instanceof DefaultMutableTreeNode) {
            // At a category, add and recurse
            for (int i = 0; i < generation; i++) {
                ret += "\t";
            }
            ret += node.toString().toUpperCase() + " (" + node.getChildCount() + ")\n";
            for (int i = 0; i < node.getChildCount(); i++) {
                ret += printNode(node.getChildAt(i), (DefaultMutableTreeNode) node, generation + 1);
            }
        } else {
            ret += "Can't handle node of type " + node.getClass() + "\n";
        }
        return ret;
    }

    public class LeafNode extends DefaultMutableTreeNode implements Serializable {

        static final long serialVersionUID = 1L;
        public String className;
        public String displayName;
        public String detailedDescription;

        public LeafNode(String className, String text, String description) {
            this.className = className;
            this.displayName = text;
            this.detailedDescription = description;
        }

        public String toString() {
            return displayName;
        }
    }
}
