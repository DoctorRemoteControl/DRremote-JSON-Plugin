/* === JsonTreeNode.java === */
package de.drremote.jsonplugin.editor.model;

import java.util.ArrayList;
import java.util.List;

public class JsonTreeNode {

    private final String name;
    private final String value;
    private final List<JsonTreeNode> children = new ArrayList<>();
    private JsonTreeNode parent;

    // raw JSON representation for this node (value or subtree)
    private final String rawJson;

    public JsonTreeNode(String name, String value) {
        this(name, value, null);
    }

    public JsonTreeNode(String name, String value, String rawJson) {
        this.name = name;
        this.value = value;
        this.rawJson = rawJson;
    }

    public void addChild(JsonTreeNode child) {
        child.parent = this;
        children.add(child);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public List<JsonTreeNode> getChildren() {
        return children;
    }

    public JsonTreeNode getParent() {
        return parent;
    }

    public String getRawJson() {
        return rawJson;
    }

    @Override
    public String toString() {
        return value == null ? name : name + ": " + value;
    }
}
