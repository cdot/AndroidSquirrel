package com.cdot.squirrel.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.cdot.squirrel.hoard.Action;
import com.cdot.squirrel.hoard.HPath;
import com.cdot.squirrel.hoard.Hoard;
import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.databinding.MainActivityBinding;
import com.cdot.squirrel.ui.fragment.TreeFragment;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // AppCompatActivity is a subclass of androidx.fragment.app.FragmentActivity

    private Hoard mHoard;

    final static long HOUR = 60 * 60 * 1000;

    Action[] test_actiona = new Action[]{
            new Action(Action.NEW, new HPath("FineDining"), 1 * HOUR),
            new Action(Action.SET_ALARM, new HPath("FineDining"), 3 * HOUR / 2, "{\"due\":1,\"repeat\":1000000}"),
            new Action(Action.NEW, new HPath("FineDining↘Caviar"), 2 * HOUR),
            new Action(Action.NEW, new HPath("FineDining↘Caviar↘Salmon"), 3 * HOUR, "Orange Eggs"),
            new Action(Action.SET_ALARM, new HPath("FineDining↘Caviar↘Salmon"), 3 * HOUR + HOUR / 2, "{\"due\":100000,\"repeat\":1000000}"),
            new Action(Action.NEW, new HPath("FineDining↘Truffles"), 4 * HOUR)
    };
    List<Action> test_actions = Arrays.asList(test_actiona);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainActivityBinding binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mHoard = new Hoard(test_actions);

        Fragment f = new TreeFragment();
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragment, f, TreeFragment.class.getName()).commit();
    }

    public Hoard getHoard() {
        return mHoard;
    }
}