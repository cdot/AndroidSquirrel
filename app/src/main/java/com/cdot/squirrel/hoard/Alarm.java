package com.cdot.squirrel.hoard;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Alarm {
    public long due; // abs time (ms) when the alarm will ring
    public long repeat; // repeat the alarm every ms

    interface Ringer {
        void ring(HPath path, Date ring);
    }

    public Alarm() {
        due = 0;
        repeat = 0;
    }

    public Alarm(long due, long repeat) {
        this.due = due;
        this.repeat = repeat;
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

    @NonNull
    public String toString() {
        return "due " + due + (repeat > 0 ? " repeat " + repeat : "");
    }
}
