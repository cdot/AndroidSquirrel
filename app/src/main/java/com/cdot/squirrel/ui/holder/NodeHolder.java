package com.cdot.squirrel.ui.holder;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.cdot.squirrel.hoard.Action;
import com.cdot.squirrel.hoard.Constraints;
import com.cdot.squirrel.hoard.Hoard;
import com.cdot.squirrel.hoard.HoardNode;
import com.cdot.squirrel.hoard.Leaf;
import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.databinding.NodeHolderBinding;
import com.cdot.squirrel.ui.fragment.AddNodeFragment;
import com.cdot.squirrel.ui.fragment.AlarmFragment;
import com.cdot.squirrel.ui.fragment.ConstrainFragment;
import com.cdot.squirrel.ui.fragment.EditNodeFragment;
import com.cdot.squirrel.ui.fragment.PickFragment;
import com.unnamed.b.atv.model.TreeNode;

/**
 * View for the content (name, value) of a hoard node
 */
public class NodeHolder extends TreeNode.BaseNodeViewHolder<HoardNode> {
    private static final String TAG = "NodeHolder";

    NodeHolderBinding mBinding;
    TreeNode mTreeNode;
    public static Hoard sHoard;
    HoardNode mHoardNode;

    public NodeHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(final TreeNode tnode, HoardNode hnode) {
        mTreeNode = tnode;
        mHoardNode = hnode;
        final LayoutInflater inflater = LayoutInflater.from(context);
        mBinding = NodeHolderBinding.inflate(inflater, null, false);
        int menu;
        if (mHoardNode instanceof Leaf) {
            menu = R.menu.leaf_node;
            mBinding.openCloseIcon.setVisibility(View.GONE);
            mTreeNode.setClickListener((tn, hn) -> {
                Toast toast = Toast.makeText(context, mHoardNode.toString(), Toast.LENGTH_SHORT);
                toast.show();
            });
        } else {
            menu = R.menu.fork_node;
            mBinding.nodeValue.setVisibility(View.GONE);
            mBinding.openCloseIcon.setOnClickListener(view1 -> tView.toggleNode(mTreeNode));
        }
        mBinding.alarm.setOnClickListener(aview -> {
            Toast toast = Toast.makeText(context, mHoardNode.getAlarm().toString(), Toast.LENGTH_SHORT);
            toast.show();
        });

        mTreeNode.setLongClickListener((tn, hn) -> {
            PopupMenu popupMenu = new PopupMenu(context, mBinding.getRoot());
            popupMenu.inflate(menu);

            // StackExchange suggests this, but studio doesn't like it
            // MenuPopupHelper menuHelper = new MenuPopupHelper(context, popupMenu.getMenu)(, mView);
            // menuHelper.setForceShowIcon(true);
            // menuHelper.show();
            // TODO: use material icon font instead?

            popupMenu.setOnMenuItemClickListener(menuItem -> onAction(menuItem.getItemId()));
            popupMenu.show();
            return true;
        });

        updateView();

        return mBinding.getRoot();
    }

    public void updateView() {
        mBinding.nodeName.setText(mHoardNode.getName());
        if (mHoardNode instanceof Leaf)
            mBinding.nodeValue.setText(((Leaf) mHoardNode).getData());

        mBinding.alarm.setVisibility((mHoardNode.getAlarm() == null) ? View.GONE : View.VISIBLE);
    }

    /**
     * Hide the current fragment, pushing it onto the stack, then open a new fragment. A neat
     * alternative to dialogs.
     *
     * @param fragment the fragment to switch to
     */
    public void pushFragment(Fragment fragment) {
        // Find the activity
        Context context = getView().getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof FragmentActivity)
                break;
            context = ((ContextWrapper) context).getBaseContext();
        }
        FragmentActivity act = (FragmentActivity) context;
        FragmentManager fm = act.getSupportFragmentManager();
        FragmentTransaction ftx = fm.beginTransaction();
        // Hide, but don't close, the open fragment (which will always be the tree view)
        ftx.hide(fm.findFragmentById(R.id.fragment));
        ftx.add(R.id.fragment, fragment, fragment.getClass().getName());
        ftx.addToBackStack(null);
        ftx.commit();
    }

    public boolean onAction(int resource) {
        switch (resource) {
            case R.id.action_alarm:
                pushFragment(new AlarmFragment(mHoardNode));
                break;
            case R.id.action_randomise:
                Constraints c = ((Leaf) mHoardNode).getConstraints();
                if (c == null) c = new Constraints();
                String newVal = c.random();
                Action act = new Action(Action.EDIT, sHoard.getPath(mHoardNode), System.currentTimeMillis(), newVal);
                try {
                    sHoard.playAction(act, true);
                } catch (Hoard.ConflictException ce) {
                    Log.e(TAG, ce.getMessage());
                }
                updateView();
                break;
            case R.id.action_pick:
                pushFragment(new PickFragment((Leaf) mHoardNode));
                break;
            case R.id.action_constrain:
                pushFragment(new ConstrainFragment((Leaf) mHoardNode));
                break;
            case R.id.action_add_folder:
                pushFragment(new AddNodeFragment(mHoardNode, false));
                break;
            case R.id.action_add_value:
                pushFragment(new AddNodeFragment(mHoardNode, true));
                break;
            case R.id.action_edit:
                pushFragment(new EditNodeFragment(mHoardNode, true));
                updateView();
                break;
            case R.id.action_rename:
                pushFragment(new EditNodeFragment(mHoardNode, false));
                updateView();
                break;

            case R.id.action_copy:
            case R.id.action_delete:
                updateView();
                break;
        }
        return true;
    }

    @Override
    public NodeHolder toggle(boolean active) {
        // Called from AndroidTreeView in response to toggleNode
        mBinding.openCloseIcon.setImageResource(active ? R.drawable.ic_folder_open : R.drawable.ic_folder_closed);
        return this;
    }
}
