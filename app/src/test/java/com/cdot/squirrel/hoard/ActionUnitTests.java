package com.cdot.squirrel.hoard;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ActionUnitTests {
    final static long HOUR = 60 * 60 * 1000;

    private String loadTestResource(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        // load from src/test/resources
        InputStream in = classLoader.getResourceAsStream(name);
        assertNotNull(in);
        ByteArrayOutputStream ouch = new ByteArrayOutputStream();
        int ch;
        try {
            while ((ch = in.read()) != -1)
                ouch.write(ch);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        return ouch.toString();
    }

    @Test
    public void should_merge_action_streams() {
        Action[] odda = new Action[]{
                new Action(Action.NEW, new HPath("A"), 1 * HOUR),
                new Action(Action.NEW, new HPath("A↘B"), 3 * HOUR),
                new Action(Action.NEW, new HPath("A↘B↘C"), 5 * HOUR, "ABC5"),
                new Action(Action.NEW, new HPath("A↘C"), 7 * HOUR, "AC7"),
        };
        List<Action> odds = Arrays.asList(odda);

        // Client hoard, with populated cache and action stream
        Action[] evena = new Action[]{
                // Duplicate an action that is already in the cloud, different date, should update the date
                new Action(Action.NEW, new HPath("A"), 2 * HOUR),
                // Add an action that isn't in the cloud yet (no implicit intermediate node creation)
                new Action(Action.NEW, new HPath("A↘C"), 4 * HOUR, "AC4"),
                new Action(Action.NEW, new HPath("A↘B"), 6 * HOUR),
                new Action(Action.NEW, new HPath("A↘B↘E"), 8 * HOUR, "ABE8")
        };
        List<Action> evens = Arrays.asList(evena);

        List<Action> m = Action.mergeActions(new ArrayList<Action>(), odds);
        assertEquals(m, odds);

        // Merging should skip duplicates
        m = Action.mergeActions(m, odds);
        assertEquals(m, odds);

        m = Action.mergeActions(odds, evens);

        Action[] expecta = new Action[]{
                odda[0],
                evena[0],
                odda[1],
                evena[1],
                odda[2],
                evena[2],
                odda[3],
                evena[3]
        };
        List<Action> expects = Arrays.asList(expecta);
        assertEquals(expects, m);

        // A merge the other way should work the same
        m = Action.mergeActions(evens, odds);
        assertEquals(expects, m);
    }

    @Test
    public void loader() {
        try {
            JSONObject job = new JSONObject(loadTestResource("actions.json"));
            JSONArray j = job.getJSONArray("actions");
            for (int i = 0; i < j.length(); i++) {
                new Action(j.getJSONObject(i));
            }
        } catch (JSONException je) {
            je.printStackTrace();
            fail(je.getMessage());
        }
    }
}
