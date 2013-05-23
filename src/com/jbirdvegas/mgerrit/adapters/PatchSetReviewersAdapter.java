package com.jbirdvegas.mgerrit.adapters;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Jon Stanford (JBirdVegas), 2013
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.listeners.TrackingClickListener;
import com.jbirdvegas.mgerrit.objects.Reviewer;

import java.util.List;

public class PatchSetReviewersAdapter extends ArrayAdapter<Reviewer> {
    private static final String TAG = PatchSetReviewersAdapter.class.getSimpleName();
    private static final boolean DEBUG = true;
    private final Activity activity;
    private final List<Reviewer> values;
    private final RequestQueue mRequestQueue;

    public PatchSetReviewersAdapter(Activity activity, List<Reviewer> values, RequestQueue requestQueue) {
        super(activity, R.layout.patchset_labels_list_item, values);
        this.activity = activity;
        this.values = values;
        this.mRequestQueue = requestQueue;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View root = convertView;
        if (root == null) {
            LayoutInflater inflater = (LayoutInflater)
                    activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            root = inflater.inflate(R.layout.patchset_labels_list_item, null);
        }
        TextView approval = (TextView) root.findViewById(R.id.labels_card_approval);
        TextView name = (TextView) root.findViewById(R.id.labels_card_reviewer_name);
        name.setOnClickListener(
                new TrackingClickListener(
                        activity,
                        values.get(position).getCommiterObject()));
        GravatarHelper.attachGravatarToTextView(name,
                values.get(position).getEmail(),
                mRequestQueue);
        Reviewer reviewer = values.get(position);
        if (DEBUG) {
            Log.d(TAG, new StringBuilder(0)
                    .append("Found Reviewer: ")
                    .append(reviewer.toString())
                    .append(" at position:")
                    .append(position)
                    .append('/')
                    .append(values.size()).toString());
        }
        setColoredApproval(reviewer.getValue(), approval);
        name.setText(reviewer.getName());
        return root;
    }

    private void setColoredApproval(String value, TextView approval) {
        int mGreen = this.getContext().getResources().getColor(R.color.text_green);
        int mRed = this.getContext().getResources().getColor(R.color.text_red);
        int plusStatus = 0;
        if (value == null) {
            value = "0";
        }
        try {
            plusStatus = Integer.parseInt(value);
            if (plusStatus >= 1) {
                approval.setText('+' + value);
                approval.setTextColor(mGreen);
            } else if (plusStatus <= -1) {
                approval.setText(value);
                approval.setTextColor(mRed);
            } else {
                approval.setText(Reviewer.NO_SCORE);
            }
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Failed to grab reviewers approval");
        }
    }
}