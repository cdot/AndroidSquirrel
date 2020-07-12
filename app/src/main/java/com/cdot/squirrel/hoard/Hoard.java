package com.cdot.squirrel.hoard;

import com.cdot.squirrel.ui.R;

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

    public static class ConflictException extends Exception {
        Action action;
        String error;
        Object[] args;

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
    Stack<Event> history;

    // The toolbar of the tree representation of the hoard
    private Fork tree;

    /**
     * Construct a new, empty hoard
     */
    public Hoard() {
        history = new Stack<>();
        tree = new Fork((String) null); // toolbar node
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
            throw new Error("Conflicts during construction");
    }

    /**
     * Get the toolbar node of the tree in the hoard
     *
     * @return the tree toolbar
     */
    public Fork getRoot() {
        return tree;
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
        Stack<Event> events = history;
        history = new Stack<>();
        return events;
    }

    /**
     * Record an action and its undo
     */
    private void recordEvent(Action redo, Action undo) {
        history.add(new Event(redo, undo));
    }

    /**
     * Does the hoard require a save?
     * @return true if a save is needed
     */
    public boolean requiresSave() {
        return history.size() > 0;
    }

    /**
     * Undo the action most recently played
     *
     * @throws ConflictException if something goes wrong
     */
    public void undo() throws ConflictException {
        Event a = history.pop();

        // Replay the reverse of the action
        playAction(a.undo, false);
    }

    /**
     * Return true if there is at least one undoable operation
     */
    public int canUndo() {
        return history.size();
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

        HoardNode node = tree.getByPath(action.path.parent());

        // HPath must always point to a valid parent Fork pre-existing
        // in the tree. parent will never be null
        if (node == null)
            throw new ConflictException(action, "parent '%s' was not found", action.path.parent());
        if (!(node instanceof Fork))
            throw new ConflictException(action, "parent '%s' is not a folder", action.path.parent());
        Fork parent = (Fork) node;

        String name = action.path.get(action.path.size() - 1);
        // HoardNode may be undefined e.g. if we are creating
        node = parent.getChildByName(name);
        Leaf leaf;

        if (action.type == Action.NEW) { // New
            if (node != null)
                // This is not really an error, we can survive it
                // easily enough. However if we don't signal a
                // conflict, the tree will be told to create a duplicate
                // node, which it mustn't do.
                throw new ConflictException(action, "it was already created @ %s", new Date(node.time));

            if (undoable)
                recordEvent(action, new Action(Action.DELETE, action.path, parent.time));

            HoardNode child;
            if (action.data != null)
                child = new Leaf(name, action.data);
            else
                child = new Fork(name);
            child.time = action.time;
            parent.addChild(child);
            if (parent.time < action.time)
                parent.time = action.time;

        } else if (action.type == Action.INSERT) { // Insert
            if (undoable)
                recordEvent(action, new Action(Action.DELETE, action.path, action.time));
            if (action.data == null)
                throw new ConflictException(action, "InternalError: null data");
            try {
                JSONObject job = new JSONObject(action.data);
                if (job.get("data") instanceof JSONObject)
                    parent.addChild(new Fork(name, job));
                else
                    parent.addChild(new Leaf(name, job));
            } catch (JSONException je) {
                parent.addChild(new Leaf(null, action.data));
            }

            if (parent.time < action.time)
                parent.time = action.time;

        } else { // all other actions require an existing node
            if (node == null)
                throw new ConflictException(action, "it does not exist");
            Fork new_parent;

            switch (action.type) {

                case Action.DELETE: // Delete
                    if (undoable) {
                        recordEvent(action, new Action(Action.INSERT, action.path, parent.time, node.toJSON().toString()));
                    }
                    parent.removeChild(parent.getChildByName(name));
                    if (parent.time < action.time)
                        parent.time = action.time;
                    break;

                case Action.EDIT: // Edit
                    if (!(node instanceof Leaf))
                        throw new ConflictException(action, "cannot edit a folder");
                    leaf = (Leaf) node;
                    if (undoable)
                        recordEvent(action, new Action(Action.EDIT, action.path, node.time, leaf.getData()));
                    leaf.setData(action.data);
                    if (leaf.time < action.time)
                        leaf.time = action.time;
                    break;

                case Action.MOVE: // Move to another parent
                    // action.data is the path of the new parent
                    HPath new_parent_path = new HPath(action.data);
                    new_parent = (Fork) tree.getByPath(new_parent_path);
                    if (new_parent == null)
                        throw new ConflictException(action, "target folder '%s' does not exist", action.data);

                    if (new_parent.getChildByName(name) != null)
                        throw new ConflictException(action, "it already exists");

                    if (undoable) {
                        // Undo moves the node back to the original parent
                        HPath from_parent = new HPath(action.path).parent();
                        Action undo = new Action(Action.MOVE, new_parent_path.append(name), parent.time, from_parent.toString());
                        recordEvent(action, undo);
                    }

                    parent.removeChild(parent.getChildByName(name));
                    if (parent.time < action.time)
                        parent.time = action.time;

                    new_parent.addChild(node);
                    if (new_parent.time < action.time)
                        new_parent.time = action.time;
                    break;

                case Action.RENAME: // Rename
                    String new_name = action.data;
                    if (parent.getChildByName(new_name) != null)
                        throw new ConflictException(action, "it already exists");
                    if (undoable) {
                        HPath p = new HPath(action.path);
                        p.remove(p.size() - 1);
                        p.add(new_name);
                        recordEvent(action, new Action(Action.RENAME, p, parent.time, name));
                    }
                    parent.removeChild(node);
                    node.name = new_name;
                    node.time = action.time;
                    parent.addChild(node);
                    break;

                case Action.SET_ALARM:
                    if (undoable) {
                        if (node.alarm == null)
                            // Undo by cancelling the new alarm
                            recordEvent(action, new Action(Action.CANCEL_ALARM, action.path, parent.time, name));
                        else
                            recordEvent(action, new Action(Action.SET_ALARM, action.path, parent.time, node.alarm.toJSON().toString()));
                    }
                    if (action.data == null)
                        node.alarm = null;
                    else {
                        try {
                            node.alarm = new Alarm(new JSONObject(action.data));
                        } catch (JSONException je) {
                            throw new ConflictException(action, je.getMessage());
                        }
                    }
                    node.time = action.time;
                    break;

                case Action.CANCEL_ALARM:
                    // Never generated, same as a set with null data
                    if (undoable)
                        recordEvent(action, new Action(Action.SET_ALARM, action.path, parent.time, node.alarm.toJSON().toString()));
                    node.alarm = null;
                    node.time = action.time;
                    break;

                case Action.CONSTRAIN:
                    if (!(node instanceof Leaf))
                        throw new Error("Cannot constrain non-leaf node");
                    leaf = (Leaf) node;
                    if (undoable) {
                        if (leaf.constraints != null)
                            recordEvent(action, new Action(Action.CONSTRAIN, action.path, parent.time, leaf.constraints.toJSON().toString()));
                        else
                            recordEvent(action, new Action(Action.CONSTRAIN, action.path, parent.time, null));
                    }
                    if (action.data == null)
                        leaf.constraints = null;
                    else {
                        try {
                            leaf.constraints = new Constraints(new JSONObject(action.data));
                        } catch (JSONException je) {
                            String[] bits = action.data.split(";", 2);
                            try {
                                leaf.constraints = new Constraints(Integer.parseInt(bits[0]), bits[1]);
                            } catch (NumberFormatException nfe) {
                                throw new ConflictException(action, je.getMessage());
                            }
                        }
                    }
                    node.time = action.time;
                    break;
                default:
                    // Version incompatibility?
                    throw new ConflictException(action, "Unrecognised action type");
            }
        }
    }

    /**
     * Construct the minimal action stream required to recreate the tree.
     *
     * @return an array of actions
     */
    public List<Action> actionsToCreate() {
        return tree.actionsToCreate();
    }

    /**
     * @param b      other hoard
     * @param differ difference reporter
     * @see HoardNode .diff(HPath, HoardNode, HoardNode.DiffReporter)
     */
    public void diff(Hoard b, HoardNode.DiffReporter differ) {
        tree.diff(new HPath(), b.tree, differ);
    }

    /**
     * Return the tree node identified by the path.
     *
     * @param path array of path elements, toolbar first
     * @return a tree node, or null if not found.
     */
    public HoardNode getNode(HPath path) {
        return tree.getByPath(path);
    }

    /**
     * Promise to check all alarms. Returns a promise to resolve all the
     * promises returned by 'ring'.
     * @param now the "current" time
     * @param ringfn function([], Date)
     * @return a promise that resolves to the number of changes that need
     * to be saved
     */
    public void checkAlarms(long now, Alarm.Ringer ringfn) {
        tree.checkAlarms(new HPath(), now, ringfn);
    }
}
