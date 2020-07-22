package com.cdot.squirrel.hoard;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Constraints {
    public static final int DEFAULT_LENGTH = 30;
    public static final String DEFAULT_CHARS = "-A-Za-z0-9!\"#$%&'()*+,./:;<=>?@[\\]^_`~";

    public int length;
    public String characters;

    /**
     * Construct with defaults
     */
    public Constraints() {
        this(DEFAULT_LENGTH, DEFAULT_CHARS);
    }

    /**
     * Construct from given minimum length and character set
     *
     * @param len minimum length
     * @param chs character set
     */
    public Constraints(int len, String chs) {
        length = len;
        characters = chs;
    }

    /**
     * Construct from JSON ovject that has fields "length" and "chars"
     *
     * @param job object to build from
     * @throws JSONException if the object scan fails
     */
    Constraints(JSONObject job) throws JSONException {
        fromJSON(job);
    }

    /**
     * Return true if this constraint set is the defaults that apply if no constraints are set
     * @return true if this is the default constraints
     */
    public boolean isDefault() {
        return (length == DEFAULT_LENGTH && DEFAULT_CHARS.equals(characters));
    }

    /**
     * Build a string of all the acceptable characters
     * @return a (potentially quite big) string
     */
    private String getCharSet() {
        StringBuilder legal = new StringBuilder();
        for (int i = 0; i < characters.length(); i++) {
            char sor = characters.charAt(i);
            if (i < characters.length() - 2 && characters.charAt(i + 1) == '-') {
                char eor = characters.charAt(i + 2);
                if (sor > eor) {
                    char t = eor; eor = sor; sor = t;
                }
                while (sor <= eor) {
                    legal.append(sor++);
                }
                i += 2;
            } else
                legal.append(sor);
        }
        return legal.toString();
    }

    /**
     * Test if a string meets the constraints
     * @param s string to test
     * @return true if it meets constraints
     */
    boolean isAcceptable(String s) {
        if (s == null)
            return false;
        if (s.length() < length)
            return false;
        String chs = getCharSet();
        for (int i = 0; i < s.length(); i++)
            if (chs.indexOf(s.charAt(i)) < 0)
                return false;
        return true;
    }

    /**
     * Generate a random string that meets the constraints
     * @return a random string
     */
    public String random() {
        String chs = getCharSet();
        StringBuilder rand = new StringBuilder();
        for (int i = 0; i < length; i++)
            rand.append(chs.charAt((int)(Math.random() * (length - 1))));
        return rand.toString();
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
            job.put("size", length);
            job.put("chars", characters);
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
        length = job.getInt("size");
        characters = job.getString("chars");
    }
}
