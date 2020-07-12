package com.cdot.squirrel.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.databinding.HelpFragmentBinding;

public class HelpFragment  extends Fragment {
    String mPage;

    public HelpFragment(String page) {
        mPage = page;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HelpFragmentBinding binding = HelpFragmentBinding.inflate(inflater, container, false);
        binding.webview.getSettings().setBuiltInZoomControls(true);
        binding.webview.loadUrl("file:///android_asset/html/" + getString(R.string.locale_prefix) + "/" + mPage + ".html");
        return binding.getRoot();
    }
}
