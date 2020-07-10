package com.cdot.squirrel.ui.activity;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.cdot.squirrel.ui.fragment.TreeFragment;
import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.databinding.ActivityMainBinding;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Fragment f = Fragment.instantiate(this, TreeFragment.class.getName());
        getFragmentManager().beginTransaction().replace(R.id.fragment, f, TreeFragment.class.getName()).commit();
    }
}