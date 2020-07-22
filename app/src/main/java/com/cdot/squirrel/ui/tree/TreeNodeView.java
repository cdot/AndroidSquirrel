package com.cdot.squirrel.ui.tree;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.cdot.squirrel.hoard.Action;
import com.cdot.squirrel.hoard.Constraints;
import com.cdot.squirrel.hoard.Hoard;
import com.cdot.squirrel.hoard.HoardNode;
import com.cdot.squirrel.hoard.Leaf;
import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.activity.MainActivity;
import com.cdot.squirrel.ui.databinding.TreeNodeViewBinding;
import com.cdot.squirrel.ui.fragment.AddNodeFragment;
import com.cdot.squirrel.ui.fragment.AlarmFragment;
import com.cdot.squirrel.ui.fragment.ConstrainFragment;
import com.cdot.squirrel.ui.fragment.EditNodeFragment;
import com.cdot.squirrel.ui.fragment.PickFragment;

/**
 * Holder for a View of the content (name, value) of a hoard node
 */
public class TreeNodeView {
    private static final String TAG = "TreeNodeView";

    private TreeNodeViewBinding mBinding;

    TreeNode mTreeNode;
    TreeRootView mRootView;

    public TreeNodeView(TreeRootView rootView, TreeNode node) {
        mRootView = rootView;
        mTreeNode = node;
    }

    public View createView(final LayoutInflater inflater) {
        if (mBinding != null)
            return mBinding.getRoot();

        mBinding = TreeNodeViewBinding.inflate(inflater, null, false);

        HoardNode hnode = mTreeNode.mHoardNode;
        if (hnode instanceof Leaf)
            mBinding.openCloseIcon.setVisibility(View.GONE);
        else {
            mBinding.nodeValue.setVisibility(View.GONE);
            mBinding.openCloseIcon.setOnClickListener(view1 -> mTreeNode.mTreeNodeView.toggle());
        }

        mBinding.alarm.setOnClickListener(aview -> {
            Toast toast = Toast.makeText(mRootView.getContext(), hnode.getAlarm().toString(), Toast.LENGTH_SHORT);
            toast.show();
        });

        updateView();

        return mBinding.getRoot();
    }

    /**
     * Get the view of the child nodes
     * @return the layout for the child nodes
     */
    public ViewGroup getChildrenView() {
        return mBinding.nodeChildren;
    }

    public void updateView() {
        HoardNode hnode = mTreeNode.mHoardNode;
        mBinding.nodeName.setText(hnode.getName());
        if (hnode instanceof Leaf)
            mBinding.nodeValue.setText(((Leaf) hnode).getData());
        mBinding.alarm.setVisibility((hnode.getAlarm() == null) ? View.GONE : View.VISIBLE);
    }

    /**
     * Get the current main activity
     */
    public MainActivity getMainActivity() {
        Context context = mRootView.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof MainActivity)
                return (MainActivity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        throw new Error("No MainActivity");
    }

    // Handle a menu item
    boolean onMenuItemClick(int resource) {
        Action act;
        HoardNode hnode = mTreeNode.mHoardNode;

        switch (resource) {
            case R.id.action_alarm:
                getMainActivity().pushFragment(new AlarmFragment(hnode));
                break;
            case R.id.action_randomise:
                Constraints c = ((Leaf) hnode).getConstraints();
                if (c == null) c = new Constraints();
                String newVal = c.random();
                act = new Action(Action.EDIT, hnode.makePath(), System.currentTimeMillis(), newVal);
                try {
                    hnode.getHoard().playAction(act, true);
                } catch (Hoard.ConflictException ce) {
                    Log.e(TAG, ce.getMessage());
                }
                break;

            case R.id.action_pick:
                getMainActivity().pushFragment(new PickFragment((Leaf) hnode));
                break;

            case R.id.action_constrain:
                getMainActivity().pushFragment(new ConstrainFragment((Leaf) hnode));
                break;

            case R.id.action_add_folder:
                getMainActivity().pushFragment(new AddNodeFragment(hnode, false));
                break;

            case R.id.action_add_value:
                getMainActivity().pushFragment(new AddNodeFragment(hnode, true));
                break;

            case R.id.action_edit:
                getMainActivity().pushFragment(new EditNodeFragment(hnode, true));
                break;

            case R.id.action_rename:
                getMainActivity().pushFragment(new EditNodeFragment(hnode, false));
                break;

            case R.id.action_delete:
                act = new Action(Action.DELETE, hnode.makePath(), System.currentTimeMillis());
                try {
                    hnode.getHoard().playAction(act, true);
                } catch (Hoard.ConflictException ce) {
                    Log.e(TAG, ce.getMessage());
                }
                break;
        }
        return true;
    }

    public void toggleIcon(boolean active) {
        if (mBinding != null)
            mBinding.openCloseIcon.setImageResource(active ? R.drawable.ic_folder_open : R.drawable.ic_folder_closed);
    }

    void inflate() {
        if (mBinding == null)
            return;
        final View v = getChildrenView();
        v.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? LinearLayout.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    void deflate() {
        // Animate the collapse of the children
        final View v = getChildrenView();
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    /**
     * Expand (open|) the given node
     *
     * @param includeSubnodes whether to open all nodes lower in the tree
     */
    void expand(boolean includeSubnodes) {
        ViewGroup container = getChildrenView();
        container.removeAllViews();
        toggleIcon(true);

        // Populate the container
        for (final TreeNode n : mTreeNode.getChildren()) {
            addChildView(n);

            if (n.mExpanded || includeSubnodes)
                n.mTreeNodeView.expand(includeSubnodes);
        }

        inflate();

        mTreeNode.mExpanded = true;
    }

    /**
     * Collapse (close) the given node
     *
     * @param includeSubnodes whether to close all nodes lower in the tree
     */
    void collapse(final boolean includeSubnodes) {
        deflate();
        toggleIcon(false);

        if (includeSubnodes) {
            for (TreeNode n : mTreeNode.getChildren()) {
                n.mTreeNodeView.collapse(includeSubnodes);
            }
        }
        mTreeNode.mExpanded = false;
    }

    /**
     * Add a view for the given node - which must be a child of the treenode we are a view of
     * @param childTreeNode child not to add a view of
     */
    void addChildView(final TreeNode childTreeNode) {
        TreeNodeView childTreeView = childTreeNode.mTreeNodeView;
        if (childTreeView != null)
            Log.e(TAG, "Trying to move a view without decoupling it first");
        childTreeView = childTreeNode.mTreeNodeView = new TreeNodeView(mRootView, childTreeNode);
        childTreeNode.mTreeNodeView = childTreeView;
        final LayoutInflater inflater = LayoutInflater.from(mRootView.getContext());
        final View childView = childTreeView.createView(inflater);
        getChildrenView().addView(childView);

        childView.setOnClickListener(v -> {
            Toast toast = Toast.makeText(mRootView.getContext(), childTreeNode.mHoardNode.toString(), Toast.LENGTH_SHORT);
            toast.show();
        });

        final TreeNodeView cnv = childTreeView;
        childView.setOnLongClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(mRootView.getContext(), cnv.mBinding.getRoot());
            if (childTreeNode.mHoardNode instanceof Leaf)
                popupMenu.inflate(R.menu.leaf_node);
            else
                popupMenu.inflate(R.menu.fork_node);
            popupMenu.setOnMenuItemClickListener(menuItem -> cnv.onMenuItemClick(menuItem.getItemId()));
            popupMenu.show();
            return true;
        });
    }

    /**
     * Toggle open/closed state of the node
     *
     * @return this, to allow chaining
     */
    public void toggle() {
        if (mTreeNode.mExpanded) {
            collapse(false);
        } else {
            expand(false);
        }
    }

    public void removeChildView(int index) {
        getChildrenView().removeViewAt(index);
    }
}
