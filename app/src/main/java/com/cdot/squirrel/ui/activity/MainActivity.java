package com.cdot.squirrel.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.cdot.squirrel.hoard.Hoard;
import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.databinding.MainActivityBinding;
import com.cdot.squirrel.ui.fragment.TreeFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    // AppCompatActivity is a subclass of androidx.fragment.app.FragmentActivity
    private static final String TAG = "MainActivity";

    final static long HOUR = 60 * 60 * 1000;

    private String loadTestResource(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        // load from src/test/resources
        InputStream in = classLoader.getResourceAsStream(name);
        if (in == null)
            throw new Error("Could not load " + name);
        ByteArrayOutputStream ouch = new ByteArrayOutputStream();
        int ch;
        try {
            while ((ch = in.read()) != -1)
                ouch.write(ch);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ouch.toString();
    }

    Hoard mHoard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
             mHoard = new Hoard(new JSONObject(loadTestResource("hoard.json")));
        } catch (JSONException je) {
            throw new Error("Failed to parse JSON " + je);
        }
        MainActivityBinding binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Fragment f = new TreeFragment();
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragment, f, TreeFragment.class.getName()).commit();
    }

    public Hoard getHoard() {
        return mHoard;
    }
    /**
     * Hide the current fragment, pushing it onto the stack, then open a new fragment. A neat
     * alternative to dialogs.
     *
     * @param fragment the fragment to switch to
     */
    public void pushFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ftx = fm.beginTransaction();
        // Hide, but don't close, the open fragment (which will always be the tree view)
        ftx.hide(fm.findFragmentById(R.id.fragment));
        ftx.add(R.id.fragment, fragment, fragment.getClass().getName());
        ftx.addToBackStack(null);
        ftx.commit();
    }
}