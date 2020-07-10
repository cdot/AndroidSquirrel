package com.cdot.squirrel.hoard;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Base class of node types in a hoard tree
 */
public abstract class HoardNode implements JSONable {

    /**
     * Interface defining a runnable that can report a difference between two nodes in terms
     * of an action required to transform from one to the other.
     */
    interface DiffReporter {
        void difference(Action act, HoardNode a, HoardNode b);
    }

    long time = System.currentTimeMillis();
    String name = null;
    Alarm alarm;

    HoardNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Designed to be overloaded in subclasses
     *
     * @param path path to the node being processed
     * @return a list of actions
     */
    protected List<Action> actionsToCreate(HPath path) {
        List<Action> actions = new ArrayList<>();
        if (alarm != null)
            actions.addAll(alarm.actionsToCreate(path));
        return actions;
    }

    /**
     * Construct the list of actions required to reproduce this entire subtree
     *
     * @return a list of actions
     */
    public List<Action> actionsToCreate() {
        return actionsToCreate(new HPath());
    }

    /**
     * Get the node at the given path
     *
     * @param path path to look up
     * @return the node found, or null
     */
    HoardNode getByPath(HPath path) {
        return null;
    }

    @Override
    public boolean equals(Object other) {
        HoardNode on = (HoardNode) other;
        // Could check constraints and alarms, but shouldn't be needed if we compare time
        return (name.equals(on.name) && time == on.time);
    }

    /**
     * Generate a string representation of the node, tabbing out with spaces and newlines
     * if tab > 0
     *
     * @param tab number of spaces to tab
     * @return string
     */
    public abstract String toString(int tab);

    /**
     * Generate a string representation of the tree under this node
     *
     * @return a string
     */
    @Override
    public String toString() {
        return toString(0);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject job = new JSONObject();
        try {
            job.put("time", time);
        } catch (JSONException ignore) {
        }
        return job;
    }

    @Override
    public void fromJSON(JSONObject job) throws JSONException {
        time = job.getLong("time");
        if (job.has("alarm"))
            try {
                alarm = new Alarm(job.getJSONObject("alarm"));
            } catch (JSONException ignore) {
                // Alarm formats are somewhat confused. Only handle correctly formatted alarms.
            }
    }

    /**
     * Simple search for differences between two hoards.  No
     * attempt is made to resolve complex changes, such as nodes
     * being moved. Each difference detected is reported using:
     * difference(action, a, b) where 'action' is the action required
     * to transform from the tree containing a to the tree with b, and
     * a and b are the tree nodes being compared. Actions used are
     * 'A', 'D', 'E', 'I' and 'X'. Not smart enough to 'M'.
     *
     * @param path   path to this node
     * @param b      node to compare against
     * @param differ difference reporter
     */
    void diff(HPath path, HoardNode b, DiffReporter differ) {
        if (alarm != null && b.alarm == null)
            differ.difference(new Action(Action.SET_ALARM, path), this, b);
        else if (b.alarm != null)
            differ.difference(new Action(Action.SET_ALARM, path, b.alarm.toJSON().toString()), this, b);
        if (!name.equals(b.name))
            // Can this ever happen?
            differ.difference(new Action(Action.RENAME, path, b.name), this, b);
    }

    /**
     * Check all alarms in this subtree.
     *
     * @param now the "current" time
     * @param path tree path to reach this node
     * @param ring ringer to invoke when an alarm is triggered
     */
    void checkAlarms(HPath path, long now, Alarm.Ringer ring) {
        if (alarm == null)
            return;

        if (alarm.due > 0 && now >= alarm.due) {
            Date ding = new Date(alarm.due);
            ring.ring(path, ding);
            if (alarm.repeat > 0)
                // Update the alarm for the next ring
                alarm.due = now + alarm.repeat;
            else
                // Disable the alarm (no repeat)
                alarm = null;
        }
    }
}