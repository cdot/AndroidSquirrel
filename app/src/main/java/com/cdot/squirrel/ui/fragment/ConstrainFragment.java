package com.cdot.squirrel.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.cdot.squirrel.hoard.Constraints;
import com.cdot.squirrel.hoard.Leaf;
import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.databinding.ConstrainFragmentBinding;

/**
 * Pseudo-dialog for value constraints
 */
public class ConstrainFragment extends Fragment {

    private Leaf mNode;
    private ConstrainFragmentBinding mBinding = null;
    private Constraints mConstraints;
    private TextWatcher mTW = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            int nl = Integer.parseInt(mBinding.length.getText().toString());
            boolean changed = false;
            if (nl != mConstraints.length) {
                mConstraints.length = nl;
                changed = true;
            }
            String nc = mBinding.characters.getText().toString();
            if (!nc.equals(mConstraints.characters)) {
                mConstraints.characters = nc;
                changed = true;
            }
            if (changed) {
                mNode.setConstraints(mConstraints.isDefault() ? null : mConstraints);
                reset();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };
    private void reset() {
        mBinding.clearConstraint.setEnabled(!mConstraints.isDefault());
        mBinding.length.removeTextChangedListener(mTW);
        mBinding.characters.removeTextChangedListener(mTW);
        mBinding.length.setText(Integer.toString(mConstraints.length));
        mBinding.characters.setText(mConstraints.characters);
        mBinding.length.addTextChangedListener(mTW);
        mBinding.characters.addTextChangedListener(mTW);
    }

    public ConstrainFragment(Leaf node) {
        mNode = node;
        mConstraints = mNode.getConstraints();
        if (mConstraints == null)
            mConstraints = new Constraints();
    }

    @Override // Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = ConstrainFragmentBinding.inflate(inflater, container, false);
        mBinding.constraintsFor.setText(getString(R.string.constrain_fragment_title, mNode.getName()));
        reset();
        mBinding.clearConstraint.setOnClickListener(v -> {
            mNode.setConstraints(null);
            mConstraints = new Constraints();
            reset();
        });
        return mBinding.getRoot();
    }
}
