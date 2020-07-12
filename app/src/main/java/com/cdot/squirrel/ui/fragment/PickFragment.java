package com.cdot.squirrel.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.cdot.squirrel.hoard.HoardNode;
import com.cdot.squirrel.hoard.Leaf;
import com.cdot.squirrel.ui.databinding.NodeHolderBinding;
import com.cdot.squirrel.ui.databinding.PickFragmentBinding;
import com.cdot.squirrel.ui.databinding.PickHolderBinding;

import java.util.ArrayList;
import java.util.List;

public class PickFragment extends Fragment {
    Leaf mNode;

    private LinearLayout getPickHolder(int i, char c) {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        PickHolderBinding binding = PickHolderBinding.inflate(inflater, null, false);
        binding.pickIndex.setText("" + i);
        binding.pickChar.setText("" + c);
        return binding.getRoot();
    }

    public PickFragment(Leaf node) {
        mNode = node;
    }

    @Override // Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PickFragmentBinding binding = PickFragmentBinding.inflate(inflater, container, false);
        String chars = mNode.getData();
        int maxw = 0;
        List<View> views = new ArrayList<>();
        for (int i = 0; i < chars.length(); i++) {
            View voo = getPickHolder(i + 1, chars.charAt(i));
            binding.pickChars.addView(voo);
            int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            voo.measure(widthMeasureSpec, heightMeasureSpec);
            int w = voo.getMeasuredWidth();
            if (w > maxw) maxw = w;
            views.add(voo);
        }

        for (View v : views)
            v.setMinimumWidth(maxw);

        return binding.getRoot();
    }
}
