package com.cdot.squirrel.ui.holder;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.cdot.squirrel.hoard.HoardNode;
import com.cdot.squirrel.hoard.Leaf;
import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.databinding.NodeHolderBinding;
import com.cdot.squirrel.ui.fragment.PickFragment;
import com.cdot.squirrel.ui.fragment.SettingsFragment;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

/**
 * View for the content (name, value) of a hoard node
 */
public class NodeHolder extends TreeNode.BaseNodeViewHolder<HoardNode> {
    NodeHolderBinding mBinding;
    TreeNode mTreeNode;
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

    private void updateView() {
        mBinding.nodeName.setText(mHoardNode.getName());
        if (mHoardNode instanceof Leaf)
            mBinding.nodeValue.setText(((Leaf) mHoardNode).getData());

        if (mHoardNode.getAlarm() == null)
            mBinding.alarm.setVisibility(View.GONE);
        else {
            mBinding.alarm.setVisibility(View.VISIBLE);
            mBinding.alarm.setOnClickListener(aview -> {
                Toast toast = Toast.makeText(context, "" + mHoardNode.getAlarm(), Toast.LENGTH_SHORT);
                toast.show();
            });
        }
    }

    public void pushFragment(Fragment fragment) {
        // How do I get to the fragment?
        View v = getView();
        Context context = v.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof FragmentActivity)
                break;
            context = ((ContextWrapper) context).getBaseContext();
        }
        FragmentActivity act = (FragmentActivity) context;
        FragmentTransaction ftx = act.getSupportFragmentManager().beginTransaction();
        ftx.replace(R.id.fragment, fragment, SettingsFragment.class.getName());
        ftx.addToBackStack(null);
        ftx.commit();
    }

    public boolean onAction(int resource) {
            switch (resource) {
            case R.id.action_add_alarm:
                updateView();
                break;
            case R.id.action_add_folder:
                updateView();
                break;
            case R.id.action_add_value:
                updateView();
                break;
            case R.id.action_copy:
                updateView();
                break;
            case R.id.action_pick:
                pushFragment(new PickFragment((Leaf)mHoardNode));
                break;
            case R.id.action_delete:
                updateView();
                break;
            case R.id.action_edit:
                updateView();
                break;
            case R.id.action_randomise:
                updateView();
                break;
            case R.id.action_rename:
                updateView();
                break;
        }
        return true;
    }

    @Override
    public NodeHolder toggle(boolean active) {
        // Called from AndroidTreeView in response to toggleNode
        mBinding.openCloseIcon.setImageResource(active ? R.drawable.ic_folder_open : R.drawable.ic_folder_closed);
        mHoardNode.isOpen = active;
        return this;
    }
}
