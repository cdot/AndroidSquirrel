package com.cdot.squirrel.hoard;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Leaf extends HoardNode {
    // String data associated with this node
    private String mData;
    private Constraints mConstraints;

    /**
     * Construct from a JSON object
     *
     * @param job the template object
     * @throws JSONException if there's a problem
     */
    Leaf(String name, JSONObject job) throws JSONException {
        super(name);
        fromJSON(job);
    }

    /**
     * Construct from a name and data string
     *
     * @param name the name of the new node
     * @param data data to associate with the node
     */
    Leaf(String name, String data) {
        super(name);
        mData = data;
    }

    /**
     * Get the data associated with this node
     * @return null if there is no data
     */
    public String getData() {
        return mData;
    }

    public Constraints getConstraints() {
        return mConstraints;
    }

    public void setConstraints(Constraints cons) {
        mConstraints = cons;
    }

    /**
     * Set or clear the data
     * @param data null to erase data
     */
    public void setData(String data) {
        this.mData = data;
    }

    public boolean meetsConstraints() {
        if (mConstraints == null)
            return true;
        return mConstraints.isAcceptable(mData);
    }

    @Override
    protected List<Action> actionsToCreate(HPath path) {
        List<Action> actions = new ArrayList<>();
        actions.add(new Action(Action.NEW, path.with(name), time, mData));
        if (mConstraints != null)
            actions.addAll(mConstraints.actionsToCreate(path));
        actions.addAll(super.actionsToCreate(path));
        return actions;
    }

    @Override
    public String toString(int tab) {
        String tabs = (tab == 0) ? "" : String.format("%1$" + tab + "s", "");
        return tabs + name + ": '" + mData + "' " + time + (tab > 0 ? "\n" : "");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject job = super.toJSON();
        try {
            job.put("data", mData);
            if (mConstraints != null) {
                job.put("constraints", mConstraints.toJSON());
            }
        } catch (JSONException ignore) {
        }
        return job;
    }

    @Override
    public void fromJSON(JSONObject job) throws JSONException {
        super.fromJSON(job);
        mData = job.getString("data");
        if (job.has("constraints"))
            mConstraints = new Constraints(job.getJSONObject("constraints"));
    }

    @Override
    void diff(HPath path, HoardNode b, DiffReporter differ) {
        if (!(b instanceof Leaf)) {
            // TODO
            return;
        }
        Leaf lb = (Leaf) b;
        if (!lb.mData.equals(mData))
            differ.difference(new Action(Action.EDIT, path, lb.mData), this, b);
        super.diff(path, b, differ);
    }
}