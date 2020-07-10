package com.cdot.squirrel.hoard;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class Constraints {
    int size;
    String chars;

    /**
     * Construct from given minimum length and character set
     *
     * @param len minimum length
     * @param chs character set
     */
    Constraints(int len, String chs) {
        size = len;
        chars = chs;
    }

    /**
     * Construct from JSON ovject that has fields "length" and "chars"
     *
     * @param job
     * @throws JSONException
     */
    Constraints(JSONObject job) throws JSONException {
        fromJSON(job);
    }

    boolean isAcceptable(String s) {
        if (s == null)
            return false;
        if (s.length() < size)
            return false;
        if (!s.matches("^[" + chars + "]*$"))
            return false;
        return true;
    }

    List<Action> actionsToCreate(HPath path) {
        List<Action> actions = new ArrayList<>();
        actions.add(new Action(Action.CONSTRAIN, path, toJSON().toString()));
        return actions;
    }

    /**
     * Capture as JSON object
     *
     * @return a JSON object that can be used to recreate the object
     */
    public JSONObject toJSON() {
        JSONObject job = new JSONObject();
        try {
            job.put("size", size);
            job.put("chars", chars);
        } catch (JSONException ignore) {
        }
        return job;
    }

    /**
     * Populate from a JSON object
     *
     * @param job a JSON object that can be used to recreate the object
     */
    public void fromJSON(JSONObject job) throws JSONException {
        size = job.getInt("size");
        chars = job.getString("chars");
    }
}
