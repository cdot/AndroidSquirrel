package com.cdot.squirrel.hoard;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * An intermediate tree node
 */
public class Fork extends HoardNode {

    // Child nodes, may be Fork or Leaf
    private SortedMap<String, HoardNode> branches;

    /**
     * Construct from a name
     *
     * @param name
     */
    Fork(String name) {
        super(name);
        branches = new TreeMap<>();
    }

    /**
     * Construct from a JSON object
     *
     * @param job the template object
     * @throws JSONException if there's a problem
     */
    Fork(String name, JSONObject job) throws JSONException {
        this(name);
        fromJSON(job);
    }

    /**
     * Get a list of child nodes
     * @return an empty list if there are no children
     */
    public SortedMap<String, HoardNode> getChildren() {
        return branches;
    }

    /**
     * Get an immediate child of this node by name
     *
     * @param name simple name of child
     * @return the child node, or null if not found
     */
    public HoardNode getChildByName(String name) {
        return branches.get(name);
    }

    /**
     * Get a node by path relative to this node
     *
     * @param path relative path to the required node
     * @return the node, or null if not found
     */
    public HoardNode getByPath(HPath path) {
        HoardNode node = this;
        for (String pel : path) {
            if (node == null || node instanceof Leaf)
                break;
            node = ((Fork) node).getChildByName(pel);
        }
        return node;
    }

    /**
     * Add a new child to the end of the branches of this node
     *
     * @param child new child to add
     */
    void addChild(HoardNode child) {
        branches.put(child.name, child);
    }

    /**
     * Remove a child node from this node
     *
     * @param child node to remove
     */
    void removeChild(HoardNode child) {
        branches.remove(child.name);
    }

    @Override
    protected List<Action> actionsToCreate(HPath path) {
        List<Action> actions = new ArrayList<>();
        if (name != null) {
            path = path.append(name);
            actions.add(new Action(Action.NEW, path, time));
        }
        for (HoardNode child : branches.values()) {
            actions.addAll(child.actionsToCreate(path));
        }
        actions.addAll(super.actionsToCreate(path));
        return actions;
    }

    @Override
    public String toString(int tab) {
        String tabs = (tab > 0) ? String.format("%1$" + tab + "s", "") : "";
        StringBuilder sub = new StringBuilder(tabs);
        sub.append(name).append(" { ").append(time);
        if (tab > 0) sub.append("\n");
        for (HoardNode child : branches.values()) {
            sub.append(child.toString(tab + 1));
        }
        sub.append(tabs).append("}");
        if (tab > 0) sub.append("\n");
        return sub.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject job = super.toJSON();
        try {
            JSONObject data = new JSONObject();
            for (String name : branches.keySet())
                data.put(name, branches.get(name).toJSON());
            job.put("data", data);
        } catch (JSONException ignore) {
        }
        return job;
    }

    @Override
    public void fromJSON(JSONObject job) throws JSONException {
        super.fromJSON(job);
        JSONObject data = job.getJSONObject("data");
        Iterator it = data.keys();
        while (it.hasNext()) {
            String name = (String)it.next();
            JSONObject child = (JSONObject)data.get(name);
            HoardNode kid;
            if (child.get("data") instanceof String)
                kid = new Leaf(name, child);
            else
                kid = new Fork(name, child);
            branches.put(name, kid);
        }
    }

    @Override
    void diff(HPath path, HoardNode b, DiffReporter differ) {
        if (!(b instanceof Fork)) {
            // TODO
            return;
        }
        List<HPath> matched = new ArrayList<>();
        Fork fb = (Fork) b;
        for (HoardNode subnode : branches.values()) {
            HPath subpath = path.append(subnode.name);
            HoardNode fbSubnode =fb.getChildByName(subnode.name);
            if (fbSubnode != null) {
                matched.add(subpath);
                subnode.diff(subpath, fbSubnode, differ);
            } else {
                // HoardNode is not present in b, delete it
                differ.difference(new Action(Action.DELETE, subpath), this, b);
            }
        }
        for (HoardNode subnode : fb.branches.values()) {
            HPath subpath = path.append(subnode.name);
            if (matched.indexOf(subpath) < 0) {
                // HoardNode in b is new
                if (subnode instanceof Fork)
                    // INSERT entire subtree
                    differ.difference(new Action(Action.INSERT, subpath, subnode.toJSON().toString()), this, b);
                else
                    // Construct using NEW
                    differ.difference(new Action(Action.NEW, subpath, ((Leaf)subnode).getData()), this, b);
            }
        }
        super.diff(path, b, differ);
    }

    public boolean equals(Object other) {
        if (!super.equals(other) || !(other instanceof Fork))
            return false;
        int i = 0;
        Fork on = (Fork)other;
        for (HoardNode c : branches.values()) {
            if (!c.equals(on.branches.get(c.name)))
                return false;
        }
        return true;
    }

    @Override
    void checkAlarms(HPath path, long now, Alarm.Ringer ring) {
        super.checkAlarms(path, now, ring);
        for (HoardNode c : branches.values()) {
            c.checkAlarms(path.append(c.name), now, ring);
        }
    }
}

