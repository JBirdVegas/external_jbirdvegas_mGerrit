package com.jbirdvegas.mgerrit.cards;

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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.fima.cardsui.objects.RecyclableCard;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.listeners.TrackingClickListener;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.objects.Reviewer;

import java.util.List;

public class PatchSetReviewersCard extends RecyclableCard {
    private static final String TAG = "PatchSetReviewersCard";
    private static final boolean DEBUG = true;

    private final RequestQueue mRequestQueue;
    private final Context mContext;
    private JSONCommit mJSONCommit;

    public PatchSetReviewersCard(JSONCommit commit,
                                 RequestQueue requestQueue,
                                 Context context) {
        mJSONCommit = commit;
        mRequestQueue = requestQueue;
        mContext = context;
    }

    @Override
    protected void applyTo(View convertView) {
        // Locate the views if necessary (these views are constant)
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (convertView.getTag() == null) {
            viewHolder = new ViewHolder();
            viewHolder.reviewerLabel = (TextView) convertView.findViewById(R.id.patchset_labels_code_reviewer_title);
            viewHolder.verifierLabel = (TextView) convertView.findViewById(R.id.patchset_labels_verified_reviewer_title);
            viewHolder.reviewerList = (ViewGroup) convertView.findViewById(R.id.patchset_lables_reviewers);
            viewHolder.verifierList = (ViewGroup) convertView.findViewById(R.id.patchset_lables_verifier);
            convertView.setTag(viewHolder);
        }

        // Get the data
        List<Reviewer> reviewerList = mJSONCommit.getCodeReviewers();
        List<Reviewer> verifierList = mJSONCommit.getVerifiedReviewers();

        // Bind the data, inflating views as needed
        viewHolder.reviewerList.removeAllViews();
        if (reviewerList != null) {
            for (Reviewer reviewer : reviewerList) {
                viewHolder.reviewerList.addView(getReviewerView(reviewer));
            }
            viewHolder.reviewerLabel.setVisibility(View.VISIBLE);
        } else {
            viewHolder.reviewerLabel.setVisibility(View.GONE);
        }

        viewHolder.verifierList.removeAllViews();
        if (verifierList != null) {
            for (Reviewer reviewer : verifierList) {
                viewHolder.verifierList.addView(getReviewerView(reviewer));
            }
            viewHolder.verifierLabel.setVisibility(View.VISIBLE);
        } else {
            viewHolder.verifierLabel.setVisibility(View.GONE);
        }
    }

    public View getReviewerView(Reviewer reviewer) {
        // Cannot use the viewholder here, we need to inflate views as needed
        LayoutInflater inflater = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View root = inflater.inflate(R.layout.patchset_labels_list_item, null);
        TextView approval = (TextView) root.findViewById(R.id.labels_card_approval);
        TextView name = (TextView) root.findViewById(R.id.labels_card_reviewer_name);
        name.setOnClickListener(
                new TrackingClickListener(
                        mContext,
                        reviewer.getCommiterObject()));
        GravatarHelper.attachGravatarToTextView(name,
                reviewer.getEmail(),
                mRequestQueue);
        if (DEBUG) {
            Log.d(TAG, new StringBuilder(0)
                    .append("Found Reviewer: ")
                    .append(reviewer.toString()).toString());
        }
        setColoredApproval(reviewer.getValue(), approval);
        name.setText(reviewer.getName());
        return root;
    }

    private void setColoredApproval(String value, TextView approval) {
        int mGreen = mContext.getResources().getColor(R.color.text_green);
        int mRed = mContext.getResources().getColor(R.color.text_red);
        int plusStatus;
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

    @Override
    protected int getCardLayoutId() {
        return R.layout.patchset_labels_card;
    }

    private static class ViewHolder {
        ViewGroup reviewerList;
        ViewGroup verifierList;
        TextView reviewerLabel;
        TextView verifierLabel;
    }
}
