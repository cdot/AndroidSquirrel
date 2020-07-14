package com.cdot.squirrel.hoard;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class HPath extends ArrayList<String> {
    public static final String PATH_SEPARATOR = "↘";

    /**
     * Parse the string on the PATH_SEPARATOR and add the components to this path
     *
     * @param s a path string
     */
    private void addBits(String s) {
        int i;
        while ((i = s.indexOf(PATH_SEPARATOR)) >= 0) {
            add(s.substring(0, i));
            s = s.substring(i + 1);
        }
        add(s);
    }

    /**
     * Construct an empty path
     */
    public HPath() {
    }

    /**
     * Construct a path from a path string
     *
     * @param s a path string
     */
    public HPath(String s) {
        addBits(s);
    }

    /**
     * Make a copy of another path
     *
     * @param p path to copy
     */
    public HPath(HPath p) {
        addAll(p);
    }

    /**
     * Load from a JSON array
     *
     * @param arr array to load
     */
    public HPath(JSONArray arr) throws JSONException {
        for (int i = 0; i < arr.length(); i++)
            add(arr.getString(i));
    }

    /**
     * Serialise to a JSON array
     *
     * @return array
     */
    public JSONArray toJSON() {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < size(); i++)
            arr.put(get(i));
        return arr;
    }

    /**
     * Create a new path by appending a path component to this path
     *
     * @param s
     * @return
     */
    public HPath with(String s) {
        HPath p = new HPath(this);
        p.addBits(s);
        return p;
    }

    /**
     * Get the parent path of this path
     *
     * @return
     */
    public HPath parent() {
        HPath p = new HPath(this);
        p.remove(p.size() - 1);
        return p;
    }

    /**
     * Test path equality
     *
     * @param oth other path
     * @return true if they are equal
     */
    boolean equals(HPath oth) {
        if (size() != oth.size())
            return false;
        for (int i = 0; i < size(); i++)
            if (!get(i).equals(oth.get(i)))
                return false;
        return true;
    }

    /**
     * Make a visual representation of the path
     *
     * @return a string version of the path that can be used to reconstruct the path
     */
    public String toString() {
        StringBuilder s = new StringBuilder();
        boolean sep = false;
        for (String p : this) {
            if (sep) s.append(PATH_SEPARATOR);
            s.append(p);
            sep = true;
        }
        return s.toString();
    }
}