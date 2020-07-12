package com.cdot.squirrel.ui.fragment;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.cdot.squirrel.hoard.Fork;
import com.cdot.squirrel.hoard.Hoard;
import com.cdot.squirrel.hoard.HoardNode;
import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.activity.MainActivity;
import com.cdot.squirrel.ui.databinding.TreeFragmentBinding;
import com.cdot.squirrel.ui.holder.NodeHolder;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

/**
 * Container for tree nodes
 */
public class TreeFragment extends Fragment {
    private static final String TAG = "TreeFragment";

    private AndroidTreeView mTreeView;
    private Hoard mHoard;

    @Override // Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Called on create and then ever time the view is gone "back" to e.g. from another fragment
        Log.d(TAG, "onCreateView");
        TreeFragmentBinding binding = TreeFragmentBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        setHasOptionsMenu(true);

        MainActivity act = (MainActivity) getActivity();
        TreeNode root = TreeNode.root();
        mHoard = act.getHoard();
        populateTree(root, (Fork)mHoard.getRoot());
        root.setViewHolder(new NodeHolder(getActivity()));
        mTreeView = new AndroidTreeView(getActivity(), root)
                .setDefaultAnimation(true)
                .setUse2dScroll(true)
                .setDefaultContainerStyle(R.style.TreeNodeStyleCustom)
                .setDefaultViewHolder(NodeHolder.class);
        mTreeView.setUseAutoToggle(false);

        binding.treenodeLayout.addView(mTreeView.getView());

        if (savedInstanceState != null) {
            String state = savedInstanceState.getString("tState");
            if (!TextUtils.isEmpty(state)) {
                mTreeView.restoreState(state);
            }
        }
        return rootView;
    }

    @Override // AppCompatActivity
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                Log.i("onQueryTextChange", newText);

                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.i("onQueryTextSubmit", query);

                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override // Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Fragment fragment;
        FragmentTransaction ftx;
        switch (menuItem.getItemId()) {
            default:
                return super.onOptionsItemSelected(menuItem);
            case R.id.undo:
                try {
                    mHoard.undo();
                } catch (Hoard.ConflictException ce) {
                }
                return true;
            case R.id.save:
            case R.id.settings:
                // Switch to settings fragment
                fragment = new SettingsFragment();
                ftx = getActivity().getSupportFragmentManager().beginTransaction();
                ftx.replace(R.id.fragment, fragment, SettingsFragment.class.getName());
                ftx.addToBackStack(null);
                ftx.commit();
                return true;
            case R.id.help:
                // Switch to help fragment, main page
                fragment = new HelpFragment("main");
                ftx = getActivity().getSupportFragmentManager().beginTransaction();
                ftx.replace(R.id.fragment, fragment, HelpFragment.class.getName());
                ftx.addToBackStack(null);
                ftx.commit();
                return true;
        }
    }

    @Override // Fragment
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.undo).setVisible(mHoard.canUndo() > 0);
        menu.findItem(R.id.save).setVisible(mHoard.requiresSave());

        super.onPrepareOptionsMenu(menu);
    }

    /**
     * Fill the given treenode with child treenodes
     * @param tnode TreeNode to fill
     * @param fork hoard node we are modelling
     */
    private void populateTree(TreeNode tnode, Fork fork) {
        for (HoardNode hchild : fork.getChildren().values()) {
            TreeNode tchild = new TreeNode(hchild);
            tnode.addChild(tchild);
            if (hchild instanceof Fork)
                populateTree(tchild, (Fork) hchild);
        }
    }

    @Override // Fragment
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tState", mTreeView.getSaveState());
    }
}
