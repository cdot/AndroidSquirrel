package com.cdot.squirrel.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.cdot.squirrel.hoard.Action;
import com.cdot.squirrel.hoard.Alarm;
import com.cdot.squirrel.hoard.Hoard;
import com.cdot.squirrel.hoard.HoardNode;
import com.cdot.squirrel.ui.R;
import com.cdot.squirrel.ui.activity.MainActivity;
import com.cdot.squirrel.ui.databinding.AlarmFragmentBinding;

import java.util.Date;

/**
 * Alarm 'pseudo-dialog'
 */
public class AlarmFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final long DAY = 24 * 60 * 60 * 1000;
    private static final long MONTH = 30 * DAY;
    private static final long YEAR = 365 * DAY;

    HoardNode mNode;
    long mDueUnit;
    long mRepeatUnit;
    AlarmFragmentBinding mBinding;

    public AlarmFragment(HoardNode node) {
        mNode = node;
        mDueUnit = DAY;
        mRepeatUnit = mDueUnit;
    }

    private void clear() {
        mBinding.due.setText("");
        mBinding.repeat.setText("");
        mBinding.dueUnits.setSelection(0);
        mBinding.repeatUnits.setSelection(0);
    }

    @Override // Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = AlarmFragmentBinding.inflate(inflater, container, false);
        final Alarm alm = mNode.getAlarm();
        mBinding.title.setText(getString(R.string.alarm_fragment_title, mNode.getName()));
        mBinding.dueUnits.setSelection(0);
        mBinding.repeatUnits.setSelection(0);
        if (alm != null) {
            mBinding.due.setText(Long.toString((alm.due - new Date().getTime()) / DAY));
            mBinding.repeat.setText(Long.toString(alm.repeat / DAY));
        } else
            clear();
        mBinding.dueUnits.setOnItemSelectedListener(this);
        mBinding.repeatUnits.setOnItemSelectedListener(this);
        mBinding.setAlarm.setOnClickListener((v) -> {
            String s = mBinding.due.getText().toString();
            int due = s.length() > 0 ? Integer.parseInt(s) : 0;
            s = mBinding.due.getText().toString();
            int repeat = s.length() > 0 ? Integer.parseInt(s) : 0;
            Alarm na = new Alarm(new Date().getTime() + mDueUnit * due, mRepeatUnit * repeat);
            Hoard h = ((MainActivity) getActivity()).getHoard();
            Action act = new Action(Action.SET_ALARM, h.getPathOf(mNode), System.currentTimeMillis(), na.toJSON().toString());
            try {
                h.playAction(act, true);
            } catch (Hoard.ConflictException ce) {
                Toast.makeText(getActivity(), ce.getMessage(), Toast.LENGTH_SHORT).show();
            }
            getActivity().getSupportFragmentManager().popBackStackImmediate();
        });
        mBinding.clearAlarm.setOnClickListener((v) -> {
            clear();
            Hoard h = ((MainActivity) getActivity()).getHoard();
            Action act = new Action(Action.SET_ALARM, h.getPathOf(mNode), System.currentTimeMillis(), null);
            try {
                h.playAction(act, true);
            } catch (Hoard.ConflictException ce) {
                Toast.makeText(getActivity(), ce.getMessage(), Toast.LENGTH_SHORT).show();
            }
            getActivity().getSupportFragmentManager().popBackStackImmediate();
        });

        return mBinding.getRoot();
    }

    public void onItemSelected(AdapterView av, View v, int pos, long id) {
        long unit = DAY;
        if (pos == 2)
            unit = YEAR;
        else if (pos == 1)
            unit = MONTH;
        if (v == mBinding.dueUnits)
            mDueUnit = unit;
        else
            mRepeatUnit = unit;
    }

    public void onNothingSelected(AdapterView av) {
    }
}
