package com.cdot.squirrel.hoard;

import org.json.JSONException;
import org.json.JSONObject;

public interface JSONable {

    /**
     * Construct a JSON template for this objecy
     *
     * @return a new JSON template
     */
    JSONObject toJSON();

    /**
     * Extract this object from a JSON template
     *
     * @param job template
     * @throws JSONException if there's a problem
     */
    void fromJSON(JSONObject job) throws JSONException;
}
