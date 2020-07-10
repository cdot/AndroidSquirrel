package com.cdot.squirrel.hoard;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class Alarm {
    long due;
    long repeat;

    interface Ringer {
        void ring(HPath path, Date ring);
    }

    Alarm(long d, long r) {
        due = d;
        repeat = r;
    }

    Alarm(Alarm a) {
        due = a.due;
        repeat = a.repeat;
    }

    Alarm(JSONObject job) throws JSONException {
        fromJSON(job);
    }

    List<Action> actionsToCreate(HPath path) {
        List<Action> actions = new ArrayList<>();
        actions.add(new Action(Action.SET_ALARM, path, toJSON().toString()));
        return actions;
    }

    public JSONObject toJSON() {
        JSONObject job = new JSONObject();
        try {
            job.put("due", due);
            job.put("repeat", repeat);
        } catch (JSONException ignore) {
        }
        return job;
    }

    public void fromJSON(JSONObject job) throws JSONException {
        due = job.getLong("due");
        repeat = job.getLong("repeat");
    }

    public String toString() {
        return "due " + due + (repeat > 0 ? " repeat " + repeat : "");
    }
}
