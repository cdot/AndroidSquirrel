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
import com.cdot.squirrel.hoard.Leaf;
import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.activity.MainActivity;
import com.cdot.squirrel.ui.databinding.EditNodeFragmentBinding;

public class EditNodeFragment extends Fragment {
    private static final String TAG = "EditNodeFragment";
    private EditNodeFragmentBinding mBinding;
    private boolean mEditValue;
    HoardNode mNode;

    public EditNodeFragment(HoardNode node, boolean editValue) {
        mEditValue = editValue;
        mNode = node;
    }

    @Override // Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO: make this an "edit node" dialog, rename doesn't have to be visible to the user
        mBinding = EditNodeFragmentBinding.inflate(inflater, container, false);
        mBinding.title.setText(getString(R.string.edit_node_fragment_title, mNode.getName()));
        if (mEditValue)
            mBinding.value.setText(((Leaf)mNode).getData());
        else
            mBinding.name.setText(mNode.getName());
        mBinding.nameLayout.setVisibility(mEditValue ? View.GONE : View.VISIBLE);
        mBinding.valueLayout.setVisibility(mEditValue ? View.VISIBLE : View.GONE);
        mBinding.save.setOnClickListener(v -> {
            String name = mBinding.name.getText().toString();
            String value = mBinding.value.getText().toString();
            Hoard h = ((MainActivity) getActivity()).getHoard();
            Action act = new Action(mEditValue ? Action.EDIT : Action.RENAME, h.getPath(mNode), System.currentTimeMillis(), mEditValue ? value : name);
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
