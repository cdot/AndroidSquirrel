package com.cdot.squirrel.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.cdot.squirrel.ui.databinding.SettingsFragmentBinding;

public class SettingsFragment extends Fragment {

    @Override // Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SettingsFragmentBinding binding = SettingsFragmentBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();
        return rootView;
    }
}
