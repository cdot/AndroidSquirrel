package com.cdot.squirrel.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cdot.squirrel.hoard.Action;
import com.cdot.squirrel.hoard.Fork;
import com.cdot.squirrel.hoard.HPath;
import com.cdot.squirrel.hoard.Hoard;
import com.cdot.squirrel.hoard.HoardNode;
import com.cdot.squirrel.hoard.Leaf;
import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.databinding.LayoutTreeFragmentBinding;
import com.cdot.squirrel.ui.holder.NodeHolder;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.util.Arrays;
import java.util.List;

/**
 * Container for tree nodes
 */
public class TreeFragment extends Fragment implements TreeNode.TreeNodeClickListener{
    private static final String NAME = "Very long name for folder";

    private AndroidTreeView mTreeView;

    final static long HOUR = 60 * 60 * 1000;

    Action[] test_actiona = new Action[]{
            new Action(Action.NEW, new HPath("FineDining"), 1 * HOUR),
            new Action(Action.NEW, new HPath("FineDining↘Caviar"), 2 * HOUR),
            new Action(Action.NEW, new HPath("FineDining↘Caviar↘Salmon"), 3 * HOUR, "Orange Eggs"),
            new Action(Action.NEW, new HPath("FineDining↘Truffles"), 4 * HOUR)
    };
    List<Action> test_actions = Arrays.asList(test_actiona);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LayoutTreeFragmentBinding binding = LayoutTreeFragmentBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        Hoard h = new Hoard(test_actions);

        TreeNode root = TreeNode.root();
        root.addChildren(buildTree(h.getRoot()));
        root.setViewHolder(new NodeHolder(getActivity()));
        
        mTreeView = new AndroidTreeView(getActivity(), root)
                            .setDefaultAnimation(true)
                            .setUse2dScroll(true)
                            .setDefaultContainerStyle(R.style.TreeNodeStyleCustom)
                            .setDefaultNodeClickListener(TreeFragment.this)
                            .setDefaultViewHolder(NodeHolder.class);

        binding.treenodeLayout.addView(mTreeView.getView());
        mTreeView.setUseAutoToggle(false);

        mTreeView.expandAll();

        if (savedInstanceState != null) {
            String state = savedInstanceState.getString("tState");
            if (!TextUtils.isEmpty(state)) {
                mTreeView.restoreState(state);
            }
        }
        return rootView;
    }

    private TreeNode buildTree(HoardNode node) {
        TreeNode tnode = new TreeNode(node);
        if (node instanceof Fork) {
            Fork fork = (Fork) node;
            for (HoardNode child : fork.getChildren().values()) {
                tnode.addChild(buildTree(child));
            }
        } else {
            Leaf leaf = (Leaf) node;
        }
        return tnode;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tState", mTreeView.getSaveState());
    }

    @Override
    public void onClick(TreeNode node, Object value) {
        Toast toast = Toast.makeText(getActivity(), value.toString(), Toast.LENGTH_SHORT);
        toast.show();
    }
}
