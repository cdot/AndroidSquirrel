package com.cdot.squirrel.hoard;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

    protected long mTime = System.currentTimeMillis();
    protected String mName;
    private Alarm mAlarm;
    // Use a weak reference to the hoard so we can garbage collect
    private WeakReference<Hoard> mHoard;

    /**
     * Construct
     *
     * @param name  the name of the new node
     * @param hoard hoard the node is in
     */
    HoardNode(String name, Hoard hoard) {
        mName = name;
        mHoard = new WeakReference<>(hoard);
    }

    public Hoard getHoard() {
        return mHoard.get();
    }

    public HPath makePath() {
        return getHoard().getPathOf(this);
    }

    /**
     * Get the parent of this node
     *
     * @return the parent node (which must be a fork) or null
     */
    public HoardNode getParent() {
        return getHoard().getParentOf(this);
    }

    /**
     * Get the parent of the given node in this subtree
     *
     * @return the parent node (which must be a fork) or null
     */
    public Fork getParentOf(HoardNode n) {
        return null;
    }

    public String getName() {
        return mName;
    }

    public void setName(String n) {
        mName = n;
    }

    public Alarm getAlarm() {
        return mAlarm;
    }

    public void setAlarm(Alarm a) {
        mAlarm = a;
    }

    public void setTime(long t) {
        mTime = t;
    }

    public long getTime() {
        return mTime;
    }

    /**
     * Designed to be overloaded in subclasses
     *
     * @param path path to the node being processed
     * @return a list of actions
     */
    protected List<Action> actionsToCreate(HPath path) {
        List<Action> actions = new ArrayList<>();
        if (mAlarm != null)
            actions.addAll(mAlarm.actionsToCreate(path));
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
     * Return the path to the given node.
     *
     * @param find     the node to find
     * @param pathHere the path to this node
     * @return the path, or null if the node is not found in the tree.
     */
    HPath makePath(HoardNode find, HPath pathHere) {
        if (this == find)
            return pathHere;
        return null;
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
        return (Objects.equals(mName, on.mName) && mTime == on.mTime);
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
    @NonNull
    @Override
    public String toString() {
        return toString(0);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject job = new JSONObject();
        try {
            job.put("time", mTime);
        } catch (JSONException ignore) {
        }
        return job;
    }

    @Override
    public void fromJSON(JSONObject job) throws JSONException {
        mTime = job.getLong("time");
        if (job.has("alarm"))
            try {
                mAlarm = new Alarm(job.getJSONObject("alarm"));
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
        if (mAlarm != null && b.mAlarm == null)
            differ.difference(new Action(Action.SET_ALARM, path), this, b);
        else if (b.mAlarm != null)
            differ.difference(new Action(Action.SET_ALARM, path, b.mAlarm.toJSON().toString()), this, b);
        if (!Objects.equals(mName, b.mName))
            // Can this ever happen?
            differ.difference(new Action(Action.RENAME, path, b.mName), this, b);
    }

    /**
     * Check all alarms in this subtree.
     *
     * @param now  the "current" time
     * @param path tree path to reach this node
     * @param ring ringer to invoke when an alarm is triggered
     */
    void checkAlarms(HPath path, long now, Alarm.Ringer ring) {
        if (mAlarm == null)
            return;

        if (mAlarm.due > 0 && now >= mAlarm.due) {
            Date ding = new Date(mAlarm.due);
            ring.ring(path, ding);
            if (mAlarm.repeat > 0)
                // Update the alarm for the next ring
                mAlarm.due = now + mAlarm.repeat;
            else
                // Disable the alarm (no repeat)
                mAlarm = null;
        }
    }
}