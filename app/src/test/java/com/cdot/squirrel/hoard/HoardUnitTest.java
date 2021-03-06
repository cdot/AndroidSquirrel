package com.cdot.squirrel.hoard;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class HoardUnitTest {

    final static long HOUR = 60 * 60 * 1000;

    @Mock
    static Context mMockContext = mock(Context.class);

    Action[] cloud_actiona = new Action[]{
            new Action(Action.NEW, new HPath("FineDining"), 1 * HOUR),
            new Action(Action.NEW, new HPath("FineDining↘Caviar"), 2 * HOUR),
            new Action(Action.NEW, new HPath("FineDining↘Caviar↘Salmon"), 3 * HOUR, "Orange Eggs"),
            new Action(Action.NEW, new HPath("FineDining↘Truffles"), 4 * HOUR)
    };
    List<Action> cloud_actions = Arrays.asList(cloud_actiona);

    // Client hoard, with populated cache and action stream
    Action[] client_actiona = new Action[]{
            // Duplicate an action that is already in the cloud, different date, should update the date
            new Action(Action.NEW, new HPath("FineDining"), 5 * HOUR),
            // Add an action that isn't in the cloud yet (no implicit intermediate node creation)
            new Action(Action.NEW, new HPath("FineDining↘Truffles"), 6 * HOUR, "Fungi"),
            new Action(Action.NEW, new HPath("FineDining↘Caviar"), 7 * HOUR),
            new Action(Action.NEW, new HPath("FineDining↘Caviar↘Lumpfish"), 8 * HOUR, "Salty")
    };
    List<Action> client_actions = Arrays.asList(client_actiona);

    @Test
    public void play_actions_into_empty_hoard() {
        // Initialise from actions, no history retained, date stamps retained
        Hoard h = new Hoard(cloud_actions);

        // There should be no undo history
        assertEquals(0, h.canUndo());

        Fork root = h.getRoot();
        assertNull(root.getName());
        assertEquals(1, root.getChildren().size());

        Fork fd = (Fork) h.getRoot().getChildByName("FineDining");
        assertNotNull(fd);
        assertEquals(4 * HOUR, fd.getTime()); // truffles timestamp
        assertEquals(2, fd.getChildren().size());

        Fork caviar = (Fork) fd.getChildren().get("Caviar");
        assertNotNull(caviar);
        assertEquals(3 * HOUR, caviar.getTime()); // salmon timestamp

        Leaf salmon = (Leaf) caviar.getChildren().get("Salmon");
        assertNotNull(salmon);
        assertEquals(3 * HOUR, salmon.getTime());
        assertEquals("Orange Eggs", salmon.getData());

        Fork truffles = (Fork) fd.getChildren().get("Truffles");
        assertNotNull(truffles);
        assertEquals(4 * HOUR, truffles.getTime());
    }

    @Test
    public void play_actions_into_populated_hoard() {
        // Play the cloud action set into a populated client hoard
        Hoard h = new Hoard(client_actions);

        for (Action act : cloud_actions) {
            try {
                h.playAction(act, false);
            } catch (Hoard.ConflictException ce) {
                if ("FineDining".equals(ce.action.path.toString())) {
                    // FineDining should conflict
                    assertEquals("FineDining", act.path.toString());
                    assertEquals("Conflict at FineDining: it was already created @ Thu Jan 01 08:00:00 GMT 1970", ce.getMessage());
                } else if ("FineDining↘Caviar".equals(act.path.toString())) {
                    // Caviar should succeed, Truffles should conflict
                    assertEquals("Conflict at FineDining↘Caviar: it was already created @ Thu Jan 01 09:00:00 GMT 1970", ce.getMessage());
                } else if ("FineDining↘Truffles".equals(act.path.toString())) {
                    // Caviar should succeed, Truffles should conflict
                    assertEquals("Conflict at FineDining↘Truffles: it was already created @ Thu Jan 01 07:00:00 GMT 1970", ce.getMessage());
                } else {
                    fail(ce.getMessage());
                }
            }
        }
        // There should be no undo history
        assertEquals(0, h.canUndo());
        Fork root = h.getRoot();
        assertNull(root.getName());
        assertEquals(1, root.getChildren().size());

        Fork fd = (Fork) h.getRoot().getChildByName("FineDining");
        assertNotNull(fd);
        assertEquals(7 * HOUR, fd.getTime()); // truffles timestamp
        assertEquals(2, fd.getChildren().size());

        // client_actions was read first
        Leaf truffles = (Leaf) fd.getChildren().get("Truffles");
        Fork caviar = (Fork) fd.getChildren().get("Caviar");
        assertNotNull(caviar);
        assertEquals(caviar.getName(), "Caviar");
        assertEquals(8 * HOUR, caviar.getTime()); // salmon timestamp

        Leaf lumpfish = (Leaf) caviar.getChildren().get("Lumpfish");
        assertNotNull(lumpfish);
        assertEquals("Salty", lumpfish.getData());

        assertEquals(lumpfish, h.getNode(new HPath("FineDining↘Caviar↘Lumpfish")));

        Leaf salmon = (Leaf) caviar.getChildren().get("Salmon");
        assertEquals("Salmon", salmon.getName());
        assertEquals(3 * HOUR, salmon.getTime());
        assertEquals("Orange Eggs", salmon.getData());

        assertEquals(truffles.getName(), "Truffles");
        assertEquals(6 * HOUR, truffles.getTime());
    }

    @Test
    public void to_and_from_json() {
        // Initialise from actions, no history retained, date stamps retained
        Hoard h1 = new Hoard(cloud_actions);

        JSONObject job = h1.getRoot().toJSON();
        Hoard h2 = new Hoard();
        try {
            System.out.println(job.toString(1));
            h2.getRoot().fromJSON(job);
        } catch (JSONException je) {
            fail(je.getMessage());
        }

        assertEquals(h1.getRoot(), h2.getRoot());

        System.out.println(h2.getRoot().toString());
    }

    @Test
    public void play_actions_into_populated_hoard_with_undo() {
        // Play the cloud action set into a populated client hoard
        Hoard h = new Hoard(client_actions);
        int conflicted = 0;
        for (Action act : cloud_actions) {
            try {
                h.playAction(act, true);
            } catch (Hoard.ConflictException ce) {
                conflicted++;
            }
        }
        assertEquals(3, conflicted);
        // There were two conflicts, so history should contain one action, a delete of the "new" node
        assertEquals(1, h.canUndo());
        Hoard.Event hist = h.getHistory().get(0);
        assertTrue(cloud_actiona[2].equals(hist.redo));
        Action undie = new Action(Action.DELETE, new HPath("FineDining↘Caviar↘Salmon"), 8 * HOUR);
        assertEquals(undie, hist.undo);
    }

    @Test
    public void create_action_stream() {
        Hoard h = new Hoard(client_actions);
        h.playActions(cloud_actions, false);
        List<Action> res = h.actionsToCreate();

        int i = 0;
        assertEquals(new Action(Action.NEW, new HPath("FineDining"), 7 * HOUR), res.get(i++));
        assertEquals(new Action(Action.NEW, new HPath("FineDining↘Caviar"), 8 * HOUR), res.get(i++));
        assertEquals(new Action(Action.NEW, new HPath("FineDining↘Caviar↘Lumpfish"), 8 * HOUR, "Salty"), res.get(i++));
        assertEquals(new Action(Action.NEW, new HPath("FineDining↘Caviar↘Salmon"), 3 * HOUR, "Orange Eggs"), res.get(i++));
        assertEquals(new Action(Action.NEW, new HPath("FineDining↘Truffles"), 6 * HOUR, "Fungi"), res.get(i++));
        assertEquals(5, res.size());
    }

    @Test
    public void reject_NEW_under_leaf() {
        Hoard h = new Hoard();
        Action basic = new Action(Action.NEW, new HPath("Lunch"), 1 * HOUR, "Sausages");
        Action cisab = new Action(Action.DELETE, new HPath("Lunch"), 2 * HOUR);
        try {
            h.playAction(basic, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        Action slipup = new Action(Action.NEW, new HPath("Lunch↘Break"), 3 * HOUR, "Crisps");
        try {
            h.playAction(slipup, true);
        } catch (Hoard.ConflictException ce) {
            assertEquals(slipup, ce.action);
            assertEquals("Conflict at Lunch↘Break: parent 'Lunch' is not a folder", ce.getMessage());
        }
    }

    @Test
    public void MOVE() {
        List<Action> list = new ArrayList<>();
        for (Action act : cloud_actions) {
            list.add(act);
        }
        Hoard h = new Hoard(list);
        try {
            h.playAction(new Action(Action.NEW, new HPath("FineDining↘Roe"), 7 * HOUR), false);
            h.playAction(new Action(Action.NEW, new HPath("FineDining↘Caviar↘Sevruga"), 8 * HOUR, "Meaty"), false);
            h.playAction(new Action(Action.NEW, new HPath("FineDining↘Caviar↘Beluga"), 9 * HOUR, "Fishy"), false);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }

        // Move Beluga to be a subnode of Roe
        // action.data is the path of the new parent
        Action movact = new Action(Action.MOVE, new HPath("FineDining↘Caviar↘Beluga"), 10 * HOUR, "FineDining↘Roe");
        try {
            h.playAction(movact, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        assertEquals(h.canUndo(), 1);
        assertEquals(movact, h.getHistory().get(0).redo);

        Action unact = new Action(Action.MOVE, new HPath("FineDining↘Roe↘Beluga"), 9 * HOUR, "FineDining↘Caviar");
        assertEquals(unact, h.getHistory().get(0).undo);

        Fork root = h.getRoot();
        assertEquals(1, root.getChildren().size());
        root = (Fork) root.getChildren().get("FineDining");
        assertEquals("FineDining", root.getName());

        // Roe, Caviar, Truffles
        assertEquals(3, root.getChildren().size());

        Fork caviar = (Fork) root.getChildren().get("Caviar");
        assertEquals("Caviar", caviar.getName());
        assertEquals(caviar.getTime(), 10 * HOUR);
        Leaf salmon = (Leaf) caviar.getChildren().get("Salmon");
        assertEquals("Salmon", salmon.getName());
        assertEquals(salmon.getTime(), 3 * HOUR);

        Fork truffles = (Fork) root.getChildren().get("Truffles");
        assertEquals("Truffles", truffles.getName());
        assertEquals(truffles.getTime(), 4 * HOUR);

        assertEquals(1, h.canUndo());
        try {
            h.undo();
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        // The time for "Roe" will not be restored to the original
        // creation time, but will get the time on "Caviar"
        // - this is an accepted limitation of the move process, it's
        // not perfectly symmetrical
    }

    @Test
    public void RENAME() {
        Hoard h = new Hoard(cloud_actions);

        Action act = new Action(Action.RENAME, new HPath("FineDining↘Truffles"), 5 * HOUR, "Earthball");
        Action tca = new Action(Action.RENAME, new HPath("FineDining↘Earthball"), 4 * HOUR, "Truffles");
        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        assertEquals(act, h.getHistory().get(0).redo);
        assertEquals(tca, h.getHistory().get(0).undo);

        Fork root = h.getRoot();
        assertNull(root.getName());
        assertEquals(1, root.getChildren().size());

        Fork fd = (Fork) h.getRoot().getChildByName("FineDining");
        assertEquals("FineDining", fd.getName());
        assertEquals(4 * HOUR, fd.getTime()); // truffles timestamp
        assertEquals(2, fd.getChildren().size());

        Fork caviar = (Fork) fd.getChildren().get("Caviar");
        assertEquals("Caviar", caviar.getName());
        assertEquals(3 * HOUR, caviar.getTime()); // salmon timestamp

        Leaf salmon = (Leaf) caviar.getChildren().get("Salmon");
        assertEquals("Salmon", salmon.getName());
        assertEquals(3 * HOUR, salmon.getTime());
        assertEquals("Orange Eggs", salmon.getData());

        Fork truffles = (Fork) fd.getChildren().get("Earthball");
        assertEquals("Earthball", truffles.getName());
        assertEquals(5 * HOUR, truffles.getTime());
    }

    @Test
    public void reject_RENAME_same_name() {
        Hoard h = new Hoard(cloud_actions);

        Action act = new Action(Action.RENAME, new HPath("FineDining↘Truffles"), 5 * HOUR, "Truffles");
        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            assertEquals("Conflict at FineDining↘Truffles: it already exists", ce.getMessage());
            return;
        }
        fail("Conflict expected");
    }

    @Test
    public void EDIT() {
        Hoard h = new Hoard(cloud_actions);
        Action act = new Action(Action.EDIT, new HPath("FineDining↘Caviar↘Salmon"), 9 * HOUR, "Earthball");
        Action tca = new Action(Action.EDIT, new HPath("FineDining↘Caviar↘Salmon"), 3 * HOUR, "Orange Eggs");

        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        assertEquals(1, h.canUndo());
        assertEquals(act, h.getHistory().get(0).redo);
        assertEquals(tca, h.getHistory().get(0).undo);
        h.clearHistory();
        assertEquals(0, h.canUndo());

        Fork root = h.getRoot();
        assertNull(root.getName());
        assertEquals(1, root.getChildren().size());

        Fork fd = (Fork) h.getRoot().getChildByName("FineDining");
        assertEquals("FineDining", fd.getName());
        assertEquals(4 * HOUR, fd.getTime()); // truffles timestamp
        assertEquals(2, fd.getChildren().size());

        Fork caviar = (Fork) fd.getChildren().get("Caviar");
        assertEquals("Caviar", caviar.getName());
        assertEquals(3 * HOUR, caviar.getTime()); // salmon timestamp

        Leaf salmon = (Leaf) caviar.getChildren().get("Salmon");
        assertEquals("Salmon", salmon.getName());
        assertEquals(9 * HOUR, salmon.getTime());
        assertEquals("Earthball", salmon.getData());

        Fork truffles = (Fork) fd.getChildren().get("Truffles");
        assertEquals("Truffles", truffles.getName());
        assertEquals(4 * HOUR, truffles.getTime());
    }

    @Test
    public void INSERT_DELETE() {
        Hoard h = new Hoard(cloud_actions);
        Action act = new Action(Action.INSERT, new HPath("FineDining↘Lobster"), 4 * HOUR, "{\"data\":\"Black\",\"time\":" + 3 * HOUR + "}");
        Action tca = new Action(Action.DELETE, new HPath("FineDining↘Lobster"), 11 * HOUR);
        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        assertEquals(1, h.canUndo());
        try {
            h.undo();
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        assertEquals(0, h.canUndo());
        System.out.println(h.getRoot());

        //assertEquals(new Hoard(cloud_actions).getRoot(), h.getRoot());
    }

    @Test
    public void DELETE_INSERT() {
        Hoard h = new Hoard(cloud_actions);

        Action act = new Action(Action.DELETE, new HPath("FineDining↘Truffles"), 11 * HOUR);
        JSONObject j = h.getRoot().getByPath(new HPath("FineDining↘Truffles")).toJSON();
        Action tca = new Action(Action.INSERT, new HPath("FineDining↘Truffles"), 4 * HOUR, j.toString());

        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        assertEquals(1, h.canUndo());
        assertEquals(act, h.getHistory().get(0).redo);
        assertEquals(tca, h.getHistory().get(0).undo);

        Fork root = h.getRoot();
        assertNull(root.getName());
        assertEquals(1, root.getChildren().size());

        Fork fd = (Fork) h.getRoot().getChildByName("FineDining");
        assertEquals("FineDining", fd.getName());
        assertEquals(11 * HOUR, fd.getTime()); // truffles timestamp
        assertEquals(1, fd.getChildren().size());

        Fork caviar = (Fork) fd.getChildren().get("Caviar");
        assertEquals("Caviar", caviar.getName());
        assertEquals(3 * HOUR, caviar.getTime()); // salmon timestamp

        Leaf salmon = (Leaf) caviar.getChildren().get("Salmon");
        assertEquals("Salmon", salmon.getName());
        assertEquals(3 * HOUR, salmon.getTime());
        assertEquals("Orange Eggs", salmon.getData());

        try {
            h.undo();
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        assertEquals(0, h.canUndo());

        //assertEquals(new Hoard(cloud_actions).getRoot(), h.getRoot());

        root = h.getRoot();
        assertNull(root.getName());
        assertEquals(1, root.getChildren().size());

        fd = (Fork) h.getRoot().getChildByName("FineDining");
        assertEquals("FineDining", fd.getName());
        assertEquals(11 * HOUR, fd.getTime()); // truffles timestamp
        assertEquals(2, fd.getChildren().size());

        caviar = (Fork) fd.getChildren().get("Caviar");
        assertEquals("Caviar", caviar.getName());
        assertEquals(3 * HOUR, caviar.getTime()); // salmon timestamp

        salmon = (Leaf) caviar.getChildren().get("Salmon");
        assertEquals("Salmon", salmon.getName());
        assertEquals(3 * HOUR, salmon.getTime());
        assertEquals("Orange Eggs", salmon.getData());

        Fork truffles = (Fork) fd.getChildren().get("Truffles");
        assertEquals("Truffles", truffles.getName());
        assertEquals(4 * HOUR, truffles.getTime());
    }

    @Test
    public void reject_NEW_zero_path() {
        Hoard h = new Hoard();
        Action act = new Action(Action.NEW, new HPath(), 1 * HOUR);
        try {
            h.playAction(act, false);
        } catch (Hoard.ConflictException ce) {
            assertEquals(act.toString(), ce.action.toString());
            assertEquals("Conflict at : Internal error: Zero length path", ce.getMessage());
            return;
        }
        fail("Expected exception");
    }

    @Test
    public void reject_NEW_no_parent() {
        Hoard h = new Hoard(cloud_actions);
        Action act = new Action(Action.NEW, new HPath("Junk↘Burger"), 24 * HOUR);

        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            assertEquals(act, ce.action);
            assertEquals(
                    "Conflict at Junk↘Burger: parent 'Junk' was not found", ce.getMessage());
            return;
        }
        fail("Conflict expected");
    }

    @Test
    public void reject_DELETE_no_such_node() {
        Hoard h = new Hoard();
        Action act = new Action(Action.DELETE, new HPath("FineDining↘La Gavroche"), 48 * HOUR);
        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            assertEquals(act, ce.action);
            assertEquals(
                    "Conflict at FineDining↘La Gavroche: parent 'FineDining' was not found",
                    ce.getMessage());
        }
    }

    @Test
    public void reject_EDIT_folder() {
        Hoard h = new Hoard(cloud_actions);
        Action act = new Action(Action.EDIT,
                new HPath("FineDining↘Caviar"),
                72 * HOUR,
                "Sausages");
        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            assertEquals(act, ce.action);
            assertEquals(
                    "Conflict at FineDining↘Caviar: cannot edit a folder",
                    ce.getMessage());
            return;
        }
        fail("Conflict expected");
    }

    @Test
    public void reject_EDIT_no_such_node() {
        Hoard h = new Hoard(cloud_actions);
        Action act = new Action(Action.EDIT,
                new HPath("FineDining↘Doner"),
                72 * HOUR,
                "Sausages");
        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            assertEquals(act, ce.action);
            assertEquals(
                    "Conflict at FineDining↘Doner: it does not exist",
                    ce.getMessage());
            return;
        }
        fail("Conflict expected");
    }

    @Test
    public void verbose() {
        cloud_actiona[0].verbose();
    }

    @Test
    public void diff() {
        Hoard a = new Hoard(cloud_actions);
        Hoard b = new Hoard(client_actions);
        // Create new fork node
        Action extract = new Action(Action.INSERT, new HPath("FineDining↘Seeds"), "{\"data\":{},\"time\":0}");
        Action leaf = new Action(Action.NEW, new HPath("FineDining↘Seeds↘Nuts"), "Hazel");
        Action releaf = new Action(Action.NEW, new HPath("FineDining↘Seeds↘Nuts"), "Pea");
        try {
            a.playAction(new Action(Action.NEW, new HPath("FineDining↘Seeds")), true);
            a.playAction(leaf, true);
            b.playAction(extract, true);
            b.playAction(releaf, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        final List<Action> acts = new ArrayList<>();
        a.diff(b, new HoardNode.DiffReporter() {
            @Override
            public void difference(Action act, HoardNode a, HoardNode b) {
                System.out.println(act);
                acts.add(act);
            }
        });
        assertEquals(3, acts.size());
        int i = 0;
        assertEquals(new Action(Action.DELETE, new HPath("FineDining↘Caviar↘Salmon")), acts.get(i++));
        assertEquals(new Action(Action.NEW, new HPath("FineDining↘Caviar↘Lumpfish"), "Salty"), acts.get(i++));
        assertEquals(new Action(Action.EDIT, new HPath("FineDining↘Seeds↘Nuts"), "Pea"), acts.get(i++));
    }

    @Test
    public void CONSTRAIN() {
        Hoard h = new Hoard(client_actions);
        Action act = new Action(Action.CONSTRAIN,
                new HPath("FineDining↘Truffles"),
                "{ \"size\": 10, \"chars\": \"a-fA-F0-9\" }");
        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        HoardNode trouffe = h.getNode(new HPath("FineDining↘Truffles"));
        assertNotNull(trouffe);
        assertTrue(trouffe instanceof Leaf);
        Leaf fungi = (Leaf) trouffe;
        assertNotNull(fungi.getConstraints());
        assertEquals(10, fungi.getConstraints().length);
        assertEquals("a-fA-F0-9", fungi.getConstraints().characters);
        assertTrue(fungi.getConstraints().isAcceptable("abcdefABCDEF0123456789"));
        assertFalse(fungi.getConstraints().isAcceptable("abcdefAB"));
        assertFalse(fungi.getConstraints().isAcceptable(""));
        assertFalse(fungi.getConstraints().isAcceptable(null));
        assertFalse(fungi.getConstraints().isAcceptable("abcdxfABCDEF0123456789"));
        assertEquals("[N: Truffles @01-Jan-1970 01:00:00 Fungi, X:  @01-Jan-1970 01:00:00 {\"size\":10,\"chars\":\"a-fA-F0-9\"}]", fungi.actionsToCreate().toString());
        try {
            h.undo();
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        trouffe = h.getNode(new HPath("FineDining↘Truffles"));
        assertNotNull(trouffe);
        assertTrue(trouffe instanceof Leaf);
        fungi = (Leaf) trouffe;
        assertNull(fungi.getConstraints());
    }

    @Test
    public void CONSTRAIN_old() {
        Hoard h = new Hoard(client_actions);

        Action act = new Action(Action.CONSTRAIN,
                new HPath("FineDining↘Truffles"),
                "30;A-Za-z0-9!%^&*_$+-=;:@#~,./?");
        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        HoardNode trouffe = h.getNode(new HPath("FineDining↘Truffles"));
        assertNotNull(trouffe);
        assertTrue(trouffe instanceof Leaf);
        Leaf fungi = (Leaf) trouffe;
        assertNotNull(fungi.getConstraints());
        assertEquals(30, fungi.getConstraints().length);
        assertEquals("A-Za-z0-9!%^&*_$+-=;:@#~,./?", fungi.getConstraints().characters);
        try {
            h.undo();
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        trouffe = h.getNode(new HPath("FineDining↘Truffles"));
        assertNotNull(trouffe);
        assertTrue(trouffe instanceof Leaf);
        fungi = (Leaf) trouffe;
        assertNull(fungi.getConstraints());
    }

    @Test
    public void SET_ALARM() {
        Hoard h = new Hoard(cloud_actions);
        Action act = new Action(Action.SET_ALARM, new HPath("FineDining↘Caviar"), "{\"due\":1,\"repeat\":1000000}");
        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        HoardNode n = h.getNode(new HPath("FineDining↘Caviar"));
        assertNotNull(n);
        assertTrue(n instanceof Fork);
        assertNotNull(n.getAlarm());
        assertEquals(1, n.getAlarm().due);
        assertEquals(1000000, n.getAlarm().repeat);

        // A set alarm with no alarm data means clear the alarm
        act = new Action(Action.SET_ALARM, new HPath("FineDining↘Caviar"));
        try {
            h.playAction(act, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        n = h.getNode(new HPath("FineDining↘Caviar"));
        assertNull(n.getAlarm());

        try {
            h.undo();
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        n = h.getNode(new HPath("FineDining↘Caviar"));
        assertNotNull(n.getAlarm());
        assertEquals(1, n.getAlarm().due);
        assertEquals(1000000, n.getAlarm().repeat);
        assertEquals("[N: Caviar @01-Jan-1970 05:00:00, N: Caviar↘Salmon @01-Jan-1970 04:00:00 Orange Eggs, A: Caviar @01-Jan-1970 01:00:00 {\"due\":1,\"repeat\":1000000}]", n.actionsToCreate().toString());

        try {
            h.undo();
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        n = h.getNode(new HPath("FineDining↘Caviar"));

        assertNull(n.getAlarm());
    }

    @Test
    public void should_ring_alarms() {
        Hoard h = new Hoard(cloud_actions);
        try {
            h.playAction(new Action(Action.SET_ALARM, new HPath("FineDining"), "{\"due\":" + 72 * HOUR + ", \"repeat\":" + (48 * HOUR) + "}"), false);
            h.playAction(new Action(Action.SET_ALARM, new HPath("FineDining↘Caviar↘Salmon"), "{\"due\":" + 24 * HOUR + ", \"repeat\":0}"), false);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }

        h.checkAlarms(0 * HOUR, new Alarm.Ringer() {
            public void ring(HPath path, Date when) {
                fail(path + " should never ring");
            }
        });

        final Map<String, Date> rung = new HashMap<>();
        h.checkAlarms(96 * HOUR, new Alarm.Ringer() {
            public void ring(HPath path, Date when) {
                assertNull(path.toString(), rung.get(path.toString()));
                rung.put(path.toString(), when);
            }
        });
        assertNotNull(rung.get("FineDining"));
        assertNotNull(rung.get("FineDining↘Caviar↘Salmon"));
    }

    @Test
    public void should_call_change_listener() {
        Hoard h = new Hoard();

        final Action a = new Action(Action.NEW, new HPath("A"), 1 * HOUR);
        final boolean[] listed = {false};
        h.addChangeListener((act, parent, node, newParent) -> {
            assertEquals(a, act);
            HoardNode n = h.getRoot().getByPath(new HPath("A"));
            assertEquals(n, node);
            assertNull(parent.getName());
            assertNull(newParent);
            listed[0] = true;
        });
        try {
            h.playAction(a, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        assert (listed[0]);
        h.clearChangeListeners();

        listed[0] = false;
        final Action b = new Action(Action.NEW, new HPath("B"), 1 * HOUR);
        h.addChangeListener((act, parent, node, newParent) -> {
            assertEquals(b, act);
            HoardNode n = h.getRoot().getByPath(new HPath("B"));
            assertEquals(n, node);
            assertNull(parent.getName());
            assertNull(newParent);
            listed[0] = true;
        });
        try {
            h.playAction(b, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        assert (listed[0]);

        h.clearChangeListeners();
        listed[0] = false;
        final Action ac = new Action(Action.NEW, new HPath("A↘C"), 1 * HOUR);
        h.addChangeListener((act, parent, node, newParent) -> {
            assertEquals(ac, act);
            HoardNode n = h.getRoot().getByPath(new HPath("A↘C"));
            assertEquals(n, node);
            assertEquals("A", parent.getName());
            assertNull(newParent);
            listed[0] = true;
        });
        try {
            h.playAction(ac, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        assert (listed[0]);

        h.clearChangeListeners();
        listed[0] = false;
        final Action acb = new Action(Action.MOVE, new HPath("A↘C"), 1 * HOUR, "B");
        h.addChangeListener((act, parent, node, newParent) -> {
            assertEquals(acb, act);
            assertNull(h.getRoot().getByPath(new HPath("A↘C")));
            HoardNode n = h.getRoot().getByPath(new HPath("B↘C"));
            assertEquals(n, node);
            assertEquals("A", parent.getName());
            assertEquals("B", newParent.getName());
            listed[0] = true;
        });
        try {
            h.playAction(acb, true);
        } catch (Hoard.ConflictException ce) {
            fail(ce.getMessage());
        }
        assert (listed[0]);
    }
}