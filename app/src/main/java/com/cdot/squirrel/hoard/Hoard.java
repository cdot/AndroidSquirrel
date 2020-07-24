package com.cdot.squirrel.hoard;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;

/**
 * A hoard consists of a tree structure, and a history of timestamped actions that have
 * modified the tree since it was constructed. So a new, empty hoard consists of an empty
 * tree and an empty history. As actions are played into the hoard, they are added (with their
 * undo) to the history as ActionPairs.
 */
public class Hoard {

    /**
     * A redo/undo pair of actions
     */
    static class Event {
        Action undo;
        Action redo;

        Event(Action redo, Action undo) {
            this.redo = redo;
            this.undo = undo;
        }
    }

    /**
     * Exception raised when there is a problem playing an action into a hoard
     */
    public static class ConflictException extends Exception {
        Action action; // The action being played
        String error;  // Text of the error, a format string
        Object[] args; // Arguments to the format string

        /**
         * @param act    The action being played
         * @param report Text of the error, a format string
         * @param args   Arguments to the format string
         */
        ConflictException(Action act, String report, Object... args) {
            action = act;
            error = report;
            this.args = args;
        }

        public String getMessage() {
            return "Conflict at " + action.path + ": " + String.format(error, args);
        }
    }

    // History of Event reflecting actions played into this hoard since it was created
    private Stack<Event> mHistory;

    // The toolbar of the tree representation of the hoard
    private Fork mTree;

    /**
     * Construct a new, empty hoard
     */
    public Hoard() {
        mHistory = new Stack<>();
        mTree = new Fork(null, this); // root node
    }

    /**
     * Construct a hoard using JSON data
     */
    public Hoard(JSONObject job) {
        this();
        if (job.has("actions")) {
            try {
                JSONArray jarr = job.getJSONArray("actions");
                for (int i = 0; i < jarr.length(); i++) {
                    JSONObject ja = jarr.getJSONObject(i);
                    playAction(new Action(ja), false);
                }
            } catch (JSONException je) {
                throw new Error("JSON exception during construction " + je);
            } catch (ConflictException ce) {
                ce.printStackTrace();
                throw new Error("Conflict during construction " + ce);
            }
        } else
            throw new Error("Unsupported hoard format");
    }

    /**
     * Construct from a list of actions. Actions are NOT recorded in the history
     *
     * @param actions list to construct from
     */
    public Hoard(List<Action> actions) {
        this();
        List<ConflictException> e = playActions(actions, false);
        if (e.size() > 0)
            throw new Error("Conflicts during construction " + e);
    }

    public Stack<Event> getHistory() {
        return mHistory;
    }

    /**
     * Get the root node of the tree in the hoard
     *
     * @return the tree toolbar
     */
    public Fork getRoot() {
        return mTree;
    }

    /**
     * Get the parent node of the given node in the hoard
     *
     * @return the tree toolbar
     */
    public Fork getParentOf(HoardNode node) {
        return mTree.getParentOf(node);
    }

    /**
     * Add a list of actions into the hoard
     *
     * @param actions    list to add
     * @param addHistory whether to add the actions to the history or not
     */
    public List<ConflictException> playActions(List<Action> actions, boolean addHistory) {
        // Play actions without adding to history
        List<ConflictException> exceptions = new ArrayList<>();
        for (Action act : actions) {
            try {
                playAction(act, addHistory);
            } catch (ConflictException ce) {
                exceptions.add(ce);
            }
        }
        return exceptions;
    }

    /**
     * Clear the history
     *
     * @return the events removed
     */
    public Stack<Event> clearHistory() {
        Stack<Event> events = mHistory;
        mHistory = new Stack<>();
        return events;
    }

    /**
     * Record an action and its undo
     */
    private void recordEvent(Action redo, Action undo) {
        mHistory.add(new Event(redo, undo));
    }

    /**
     * Does the hoard require a save?
     *
     * @return true if a save is needed
     */
    public boolean requiresSave() {
        return mHistory.size() > 0;
    }

    /**
     * Undo the action most recently played
     *
     * @throws ConflictException if something goes wrong
     */
    public void undo() throws ConflictException {
        Event a = mHistory.pop();

        // Replay the reverse of the action
        playAction(a.undo, false);
    }

    /**
     * Return true if there is at least one undoable operation
     */
    public int canUndo() {
        return mHistory.size();
    }

    /**
     * Promise to play a single action into the tree.
     * <p>
     * Action types:
     * <ul>
     * <li>'N' with no data - create collection</li>
     * <li>'N' with data - create leaf</li>
     * <li>'D' delete node, no data. Will delete the entire node tree.</li>
     * <li>'I' insert node, insert an entire node tree at a named node.</li>
     * <li>'E' edit node - modify the leaf data in a node</li>
     * <li>'R' rename node - data contains new name</li>
     * </ul>
     * Returns a conflict object if there was an error. This has two fields,
     * 'action' for the action record and 'message'.
     *
     * @param action   action the action record
     * @param undoable if true and action that undos this action
     *                 will be added to the undo history. Default is true.
     */
    public void playAction(Action action, boolean undoable) throws ConflictException {
        action = new Action(action);

        if (action.path.size() == 0)
            throw new ConflictException(action, "Internal error: Zero length path");

        HoardNode actionNode = mTree.getByPath(action.path.parent());

        // HPath must always point to a valid parent Fork pre-existing
        // in the tree. parent will never be null
        if (actionNode == null)
            throw new ConflictException(action, "parent '%s' was not found", action.path.parent());
        if (!(actionNode instanceof Fork))
            throw new ConflictException(action, "parent '%s' is not a folder", action.path.parent());

        Fork actionNodeParent = (Fork) actionNode;
        Fork actionNodeNewParent = null;

        String actionNodeName = action.path.get(action.path.size() - 1);
        // HoardNode may be undefined e.g. if we are creating
        actionNode = actionNodeParent.getChildByName(actionNodeName);
        Leaf leaf;

        if (action.type == Action.NEW) { // New
            if (actionNode != null)
                // This is not really an error, we can survive it
                // easily enough. However if we don't signal a
                // conflict, the tree will be told to create a duplicate
                // node, which it mustn't do.
                throw new ConflictException(action, "it was already created @ %s", new Date(actionNode.getTime()));

            if (undoable)
                recordEvent(action, new Action(Action.DELETE, action.path, actionNodeParent.getTime()));

            if (action.data == null)
                actionNode = new Fork(actionNodeName, this);
            else
                actionNode = new Leaf(actionNodeName, this, action.data);

            actionNode.setTime(action.time);
            actionNodeParent.addChild(actionNode);
            if (actionNodeParent.getTime() < action.time)
                actionNodeParent.setTime(action.time);

        } else if (action.type == Action.INSERT) { // Insert
            if (undoable)
                recordEvent(action, new Action(Action.DELETE, action.path, action.time));
            if (action.data == null)
                throw new ConflictException(action, "InternalError: null data");
            try {
                JSONObject job = new JSONObject(action.data);
                if (job.get("data") instanceof JSONObject)
                    actionNode = new Fork(actionNodeName, this, job);
                else
                    actionNode = new Leaf(actionNodeName, this, job);
            } catch (JSONException je) {
                actionNode = new Leaf(null, this, action.data);
            }
            actionNodeParent.addChild(actionNode);

            if (actionNodeParent.getTime() < action.time)
                actionNodeParent.setTime(action.time);

        } else { // all other actions require an existing node
            if (actionNode == null)
                throw new ConflictException(action, "it does not exist");

            switch (action.type) {

                case Action.DELETE: // Delete, action path is node being deleted
                    if (undoable) {
                        recordEvent(action, new Action(Action.INSERT, action.path, actionNodeParent.getTime(), actionNode.toJSON().toString()));
                    }
                    if (actionNodeParent.getChildByName(actionNodeName) != actionNode)
                        throw new Error("Unexpected");
                    actionNodeParent.removeChild(actionNode);
                    if (actionNodeParent.getTime() < action.time)
                        actionNodeParent.setTime(action.time);
                    break;

                case Action.EDIT: // Edit
                    if (!(actionNode instanceof Leaf))
                        throw new ConflictException(action, "cannot edit a folder");
                    leaf = (Leaf) actionNode;
                    if (undoable)
                        recordEvent(action, new Action(Action.EDIT, action.path, actionNode.getTime(), leaf.getData()));
                    leaf.setData(action.data);
                    if (leaf.getTime() < action.time)
                        leaf.setTime(action.time);
                    break;

                case Action.MOVE: // Move to another parent
                    // action.data is the path of the new parent
                    HPath new_parent_path = new HPath(action.data);
                    actionNodeNewParent = (Fork) mTree.getByPath(new_parent_path);
                    if (actionNodeNewParent == null)
                        throw new ConflictException(action, "target folder '%s' does not exist", action.data);

                    if (actionNodeNewParent.getChildByName(actionNodeName) != null)
                        throw new ConflictException(action, "it already exists");

                    if (undoable) {
                        // Undo moves the node back to the original parent
                        HPath from_parent = new HPath(action.path).parent();
                        Action undo = new Action(Action.MOVE, new_parent_path.with(actionNodeName), actionNodeParent.getTime(), from_parent.toString());
                        recordEvent(action, undo);
                    }

                    actionNodeParent.removeChild(actionNodeParent.getChildByName(actionNodeName));
                    if (actionNodeParent.getTime() < action.time)
                        actionNodeParent.setTime(action.time);

                    actionNodeNewParent.addChild(actionNode);
                    if (actionNodeNewParent.getTime() < action.time)
                        actionNodeNewParent.setTime(action.time);
                    break;

                case Action.RENAME: // Rename
                    String new_name = action.data;
                    if (actionNodeParent.getChildByName(new_name) != null)
                        throw new ConflictException(action, "it already exists");
                    if (undoable) {
                        HPath p = new HPath(action.path);
                        p.remove(p.size() - 1);
                        p.add(new_name);
                        recordEvent(action, new Action(Action.RENAME, p, actionNodeParent.getTime(), actionNodeName));
                    }
                    actionNodeParent.removeChild(actionNode);
                    actionNode.setName(new_name);
                    actionNode.setTime(action.time);
                    actionNodeParent.addChild(actionNode);
                    break;

                case Action.SET_ALARM:
                    if (undoable) {
                        if (actionNode.getAlarm() == null)
                            // Undo by cancelling the new alarm
                            recordEvent(action, new Action(Action.CANCEL_ALARM, action.path, actionNodeParent.getTime(), actionNodeName));
                        else
                            recordEvent(action, new Action(Action.SET_ALARM, action.path, actionNodeParent.getTime(), actionNode.getAlarm().toJSON().toString()));
                    }
                    if (action.data == null)
                        actionNode.setAlarm(null);
                    else {
                        try {
                            actionNode.setAlarm(new Alarm(new JSONObject(action.data)));
                        } catch (JSONException je) {
                            throw new ConflictException(action, je.getMessage());
                        }
                    }
                    actionNode.setTime(action.time);
                    break;

                case Action.CANCEL_ALARM:
                    // Never generated, same as a set with null data
                    if (undoable)
                        recordEvent(action, new Action(Action.SET_ALARM, action.path, actionNodeParent.getTime(), actionNode.getAlarm().toJSON().toString()));
                    actionNode.setAlarm(null);
                    actionNode.setTime(action.time);
                    break;

                case Action.CONSTRAIN:
                    if (!(actionNode instanceof Leaf))
                        throw new Error("Cannot constrain non-leaf node");
                    leaf = (Leaf) actionNode;
                    if (undoable) {
                        if (leaf.getConstraints() != null)
                            recordEvent(action, new Action(Action.CONSTRAIN, action.path, actionNodeParent.getTime(), leaf.getConstraints().toJSON().toString()));
                        else
                            recordEvent(action, new Action(Action.CONSTRAIN, action.path, actionNodeParent.getTime(), null));
                    }
                    if (action.data == null)
                        leaf.setConstraints(null);
                    else {
                        try {
                            leaf.setConstraints(new Constraints(new JSONObject(action.data)));
                        } catch (JSONException je) {
                            String[] bits = action.data.split(";", 2);
                            try {
                                leaf.setConstraints(new Constraints(Integer.parseInt(bits[0]), bits[1]));
                            } catch (NumberFormatException nfe) {
                                throw new ConflictException(action, je.getMessage());
                            }
                        }
                    }
                    actionNode.setTime(action.time);
                    break;
                default:
                    // Version incompatibility?
                    throw new ConflictException(action, "Unrecognised action type");
            }
        }

        // Notify listeners
        for (ChangeListener listener : mListeners)
            listener.actionPlayed(action, actionNodeParent, actionNode, actionNodeNewParent);
    }

    /**
     * Construct the minimal action stream required to recreate the tree.
     *
     * @return an array of actions
     */
    public List<Action> actionsToCreate() {
        return mTree.actionsToCreate();
    }

    /**
     * @param b      other hoard
     * @param differ difference reporter
     * @see HoardNode .diff(HPath, HoardNode, HoardNode.DiffReporter)
     */
    public void diff(Hoard b, HoardNode.DiffReporter differ) {
        mTree.diff(new HPath(), b.mTree, differ);
    }

    /**
     * Return the tree node identified by the path.
     *
     * @param path array of path elements, toolbar first
     * @return a tree node, or null if not found.
     */
    public HoardNode getNode(HPath path) {
        return mTree.getByPath(path);
    }

    /**
     * Return the path to the given node.
     *
     * @param node the node to find
     * @return the path, or null if the node is not found in the tree.
     */
    public HPath getPathOf(HoardNode node) {
        return mTree.makePath(node, new HPath());
    }

    /**
     * Promise to check all alarms. Returns a promise to resolve all the
     * promises returned by 'ring'.
     *
     * @param now    the "current" time
     * @param ringfn function([], Date)
     */
    public void checkAlarms(long now, Alarm.Ringer ringfn) {
        mTree.checkAlarms(new HPath(), now, ringfn);
    }

    public interface ChangeListener {
        /**
         * Invoked when an action is played
         *
         * @param act       the action being played
         * @param parent    parent of node (or old parent, if the node was moved
         * @param node      affected node
         * @param newParent new parent node, if action is MOVE, null otherwise
         */
        void actionPlayed(Action act, HoardNode parent, HoardNode node, HoardNode newParent);
    }

    List<ChangeListener> mListeners = new ArrayList<>();

    public void addChangeListener(ChangeListener listener) {
        if (mListeners.indexOf(listener) == -1)
            mListeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        mListeners.remove(listener);
    }

    public void clearChangeListeners() {
        mListeners.clear();
    }
}
