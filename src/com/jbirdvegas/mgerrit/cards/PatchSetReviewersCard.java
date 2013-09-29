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
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.PatchSetViewerFragment;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.adapters.PatchSetReviewersAdapter;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.listeners.TrackingClickListener;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.objects.Reviewer;

import java.util.List;

public class PatchSetReviewersCard extends Card {
    private static final String TAG = PatchSetReviewersAdapter.class.getSimpleName();
    private static final boolean DEBUG = true;
    private final RequestQueue mRequestQueue;
    private final Context mContext;
    private JSONCommit mJSONCommit;
    private LayoutInflater mLayoutInflater;

    public PatchSetReviewersCard(JSONCommit commit,
                                 RequestQueue requestQueue,
                                 Context context) {
        mJSONCommit = commit;
        mRequestQueue = requestQueue;
        mContext = context;
    }

    @Override
    public View getCardContent(Context context) {
        mLayoutInflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout rootView =
                (LinearLayout) mLayoutInflater.inflate(R.layout.patchset_labels_card, null);
        LinearLayout codeReviewersContainer =
                (LinearLayout) rootView.findViewById(R.id.patchset_lables_reviewers);
        LinearLayout verifyReviewersContainer =
                (LinearLayout) rootView.findViewById(R.id.patchset_lables_verifier);
        TextView reviewerLabel = (TextView) rootView.findViewById(R.id.patchset_labels_code_reviewer_title);
        TextView verifierLabel = (TextView) rootView.findViewById(R.id.patchset_labels_verified_reviewer_title);
        List<Reviewer> reviewerList = mJSONCommit.getCodeReviewers();
        List<Reviewer> verifierList = mJSONCommit.getVerifiedReviewers();

        if (reviewerList != null) {
            for (Reviewer reviewer : mJSONCommit.getCodeReviewers()) {
                codeReviewersContainer.addView(getReviewerView(reviewer));
            }
        } else {
            codeReviewersContainer.setVisibility(View.GONE);
            reviewerLabel.setVisibility(View.GONE);
        }

        if (verifierList != null) {
            for (Reviewer reviewer : mJSONCommit.getVerifiedReviewers()) {
                verifyReviewersContainer.addView(getReviewerView(reviewer));
            }
        } else {
            verifyReviewersContainer.setVisibility(View.GONE);
            verifierLabel.setVisibility(View.GONE);
        }
        return rootView;
    }
    public View getReviewerView(Reviewer reviewer) {
        View root = mLayoutInflater.inflate(R.layout.patchset_labels_list_item, null);
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
}
