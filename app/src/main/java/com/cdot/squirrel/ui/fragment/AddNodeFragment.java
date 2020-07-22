package com.cdot.squirrel.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.cdot.squirrel.hoard.Action;
import com.cdot.squirrel.hoard.Hoard;
import com.cdot.squirrel.hoard.HoardNode;
import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.activity.MainActivity;
import com.cdot.squirrel.ui.databinding.AddNodeFragmentBinding;

public class AddNodeFragment extends Fragment {
    private static final String TAG = "AddNodeFragment";
    private AddNodeFragmentBinding mBinding;
    private boolean mLeaf;
    HoardNode mNode;

    public AddNodeFragment(HoardNode node, boolean isLeaf) {
        mLeaf = isLeaf;
        mNode = node;
    }

    @Override // Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO: make one menu item/dialog to add both node types
        mBinding = AddNodeFragmentBinding.inflate(inflater, container, false);
        mBinding.title.setText(getString(R.string.add_node_fragment_title, mLeaf ? getString(R.string.value) : getString(R.string.folder), mNode.getName()));
        mBinding.valueLayout.setVisibility(mLeaf ? View.VISIBLE : View.GONE);
        mBinding.add.setOnClickListener(v -> {
            String name = mBinding.name.getText().toString();
            String data = mLeaf ? mBinding.value.getText().toString() : null;
            Hoard h = ((MainActivity) getActivity()).getHoard();
            Action act = new Action(Action.NEW, h.getPathOf(mNode).with(name), System.currentTimeMillis(), data);
            try {
                h.playAction(act, true);
            } catch (Hoard.ConflictException ce) {
                Toast.makeText(getActivity(), ce.getMessage(), Toast.LENGTH_SHORT).show();
            }
            getActivity().getSupportFragmentManager().popBackStackImmediate();
        });
        return mBinding.getRoot();
    }
}