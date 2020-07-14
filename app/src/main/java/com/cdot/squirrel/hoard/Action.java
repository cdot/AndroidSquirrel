package com.cdot.squirrel.hoard;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class Action implements JSONable {
    public static final char NEW = 'N';
    public static final char DELETE = 'D';
    public static final char INSERT = 'I';
    public static final char EDIT = 'E';
    public static final char MOVE = 'M';
    public static final char RENAME = 'R';
    public static final char SET_ALARM = 'A';
    public static final char CANCEL_ALARM = 'C';
    public static final char CONSTRAIN = 'X';

    public HPath path;
    public char type;
    public long time;
    public static final long NO_TIME = 0;
    public String data; // Data is always stringified

    /**
     * Copy constructor
     */
    public Action(Action proto) {
        this.type = proto.type;
        if (proto.path != null)
            path = new HPath(proto.path);
        time = proto.time;
        if (proto.data != null)
            data = proto.data;
    }

    /**
     * No data constructor
     * @param type action type
     * @param path node path
     */
    public Action(char type, HPath path) {
        this.type = type;
        this.path = new HPath(path);
        time = NO_TIME;
    }

    /**
     * No data constructor
     * @param type action type
     * @param path node path
     * @param time time of action
     */
    public Action(char type, HPath path, long time) {
        this(type, path);
        this.time = time;
    }

    /**
     * Constructor
     * @param type action type
     * @param path node path
     * @param data associated data
     */    public Action(char type, HPath path, String data) {
        this(type, path);
        this.data = data;
    }

    /**
     * Full parameter constructor
     * @param type action type
     * @param path node path
     * @param time time of action
     * @param data associated data
     */
    public Action(char type, HPath path, long time, String data) {
        this(type, path, time);
        this.data = data;
    }

    /**
     * Constructor
     * @param job JSON template
     * @throws JSONException if anything goes wrong
     */
    public Action(JSONObject job) throws JSONException {
        fromJSON(job);
    }

    /**
     * Comparator for use in sorting action lists by time order
     */
    static Comparator cmp = new Comparator<Action>() {
        @Override
        public int compare(Action item, Action item2) {
            // Sorts NO_TIME first
            return Long.compare(item.time, item2.time);
        }
    };

    /**
     * Compare two actions for equality.
     * @param b action to compare
     * @return equality
     */
    public boolean equals(Object b) {
        if (!(b instanceof Action))
            return false;
        Action ab = (Action) b;
        if (type != ab.type || !path.equals(ab.path))
            return false;
        if (data == null) {
            if (ab.data != null)
                return false;
        } else if (!data.equals(ab.data))
            return false;
        // If either is marked NO_TIME then don't compare times
        if (time != NO_TIME && ab.time != NO_TIME && time != ab.time)
            return false;
        return true;
    }

    /**
     * Generate a florid description of the action for using in dialogs
     *
     * @return {string} human readable description of action
     */
    String verbose() {
        switch (type) {
            case Action.DELETE:
                return String.format("Delete '%s'", path);
            case Action.EDIT:
                return String.format("Change value of %s to '%s'", path, data);
            case Action.INSERT:
                return String.format("Insert %s = %s", path, data);
            case Action.MOVE:
                return String.format("Move %s to %s", path, data);
            case Action.NEW:
                return String.format("Create %s", path);
            case Action.RENAME:
                return String.format("Rename %s to '%s'", path, data);
        }
        return null;
    }

    /**
     * Generate a terse string version of the action for reporting
     *
     * @return {string} human readable description of action
     */
    public String toString() {
        DateFormat f = DateFormat.getDateTimeInstance();
        return this.type + ": " + path + " @" + f.format(new Date(time)) + ((data != null) ? " " + data : "");
    }

    /**
     * Merge two action streams in time order. Duplicate actions
     * are merged.
     *
     * @param as the first action stream to merge
     * @param bs the second action stream to merge
     * @return the merged action stream, sorted in time order. Note that
     * the action objects in a and b are preserved for use here
     */
    static List<Action> mergeActions(List<Action> as, List<Action> bs) {

        List<Action> a = new ArrayList<>(as);
        List<Action> b = new ArrayList<>(bs);
        if (a.size() == 0)
            return b;

        if (b.size() == 0)
            return a;

        // Sort streams into time order
        Collections.sort(a, Action.cmp);
        Collections.sort(b, Action.cmp);

        Action aact = a.remove(0), bact = b.remove(0);
        List<Action> c = new ArrayList<>();
        while (aact != null || bact != null) {
            if (aact != null && bact != null) {
                if (aact.equals(bact)) {
                    // Duplicate, ignore one of them
                    aact = (a.size() > 0) ? a.remove(0) : null;
                } else if (aact.time < bact.time) {
                    c.add(aact);
                    aact = (a.size() > 0) ? a.remove(0) : null;
                } else {
                    c.add(bact);
                    bact = (b.size() > 0) ? b.remove(0) : null;
                }
            } else if (aact != null) {
                c.add(aact);
                aact = (a.size() > 0) ? a.remove(0) : null;
            } else {
                c.add(bact);
                bact = (b.size() > 0) ? b.remove(0) : null;
            }
        }
        return c;
    }

    @Override
    public void fromJSON(JSONObject job) throws JSONException {
        path = new HPath(job.getJSONArray("path"));
        type = job.getString("type").charAt(0);
        time = job.getLong("time");
        data = null;
        if (job.has("data")) {
            Object jo = job.get("data");
            if (jo instanceof String)
                data = job.getString("data");
            else
                data = jo.toString();
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject job = new JSONObject();
        try {
            job.put("path", path.toJSON());
            job.put("type", Character.toString(type));
            job.put("time", time);
            if (data != null)
                job.put("data", data);
        } catch (JSONException ignore) {
        }
        return job;
    }
}
