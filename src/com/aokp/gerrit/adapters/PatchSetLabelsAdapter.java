package com.aokp.gerrit.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.aokp.gerrit.R;
import com.aokp.gerrit.objects.Reviewer;

import java.util.List;


public class PatchSetLabelsAdapter extends ArrayAdapter<Reviewer> {
    private final Context context;
    private final List<Reviewer> values;
    private static final String TAG = PatchSetLabelsAdapter.class.getSimpleName();

    public PatchSetLabelsAdapter(Context context, List<Reviewer> values) {
        super(context, R.layout.patchset_labels_list_item, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View root = convertView;
        if (root == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            root = inflater.inflate(R.layout.patchset_labels_list_item, null);
        }
        TextView approval = (TextView) root.findViewById(R.id.labels_card_approval);
        TextView name = (TextView) root.findViewById(R.id.labels_card_reviewer_name);
        Reviewer reviewer = values.get(position);
        Log.d(TAG, "Found Reviewer: " + reviewer.toString() + " at position:" + position + '/' + values.size());
        setColoredApproval(reviewer.getValue(), approval);
        name.setText(reviewer.getName());
        return root;
    }

    private void setColoredApproval(String value, TextView approval) {
        int plusStatus = 0;
        if (value == null) {
            value = "0";
        }
        try {
            plusStatus = Integer.parseInt(value);
            if (plusStatus >= 1) {
                approval.setText('+' + value);
                approval.setTextColor(Color.GREEN);
            } else if (plusStatus <= -1) {
                approval.setText(value);
                approval.setTextColor(Color.RED);
            } else {
                approval.setText(Reviewer.NO_SCORE);
            }
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Failed to grab reviewers approval");
        }
    }
} 