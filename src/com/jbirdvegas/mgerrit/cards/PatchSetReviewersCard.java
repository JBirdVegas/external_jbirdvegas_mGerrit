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
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.UserReviewers;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.objects.Reviewer;

import org.jetbrains.annotations.NotNull;

public class PatchSetReviewersCard implements CardBinder {
    private static final boolean DEBUG = true;

    private final RequestQueue mRequestQueue;
    private final Context mContext;
    private final FragmentActivity mActivity;
    private final LayoutInflater mInflater;
    private Integer mCodeReview_index;
    private Integer mVerified_index;
    private Integer mReviewerId_index;
    private Integer mReviewerEmail_index;
    private Integer mReviewerName_index;

    public PatchSetReviewersCard(Context context, RequestQueue requestQueue) {
        mRequestQueue = requestQueue;
        mContext = context;
        mActivity = (FragmentActivity) mContext;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View setViewValue(Cursor cursor, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.patchset_labels_card, null);
        }

        // Locate the views if necessary (these views are constant)
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (convertView.getTag() == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        setIndicies(cursor);

        TextView reviewer = viewHolder.reviewer;
        reviewer.setTag(cursor.getInt(mReviewerId_index));
        viewHolder.reviewer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int reviewerId = (int) v.getTag();
                setTrackingUser(reviewerId);
            }
        });

        GravatarHelper.attachGravatarToTextView(reviewer,
                cursor.getString(mReviewerEmail_index), mRequestQueue);
        reviewer.setText(cursor.getString(mReviewerName_index));

        setColoredApproval(cursor.getInt(mCodeReview_index), viewHolder.codeReview,
                viewHolder.codeReviewLayout);
        setColoredApproval(cursor.getInt(mVerified_index), viewHolder.verified,
                viewHolder.verifiedLayout);

        return convertView;
    }

    private void setColoredApproval(Integer value, TextView approval, ViewGroup container) {
        int mGreen = mContext.getResources().getColor(R.color.text_green);
        int mRed = mContext.getResources().getColor(R.color.text_red);
        if (value == null) value = 0;
        if (value >= 1) {
            if (container != null) container.setVisibility(View.VISIBLE);
            approval.setText('+' + value.toString());
            approval.setTextColor(mGreen);
        } else if (value <= -1) {
            if (container != null) container.setVisibility(View.VISIBLE);
            approval.setText(value.toString());
            approval.setTextColor(mRed);
        } else if (container != null) {
            container.setVisibility(View.GONE);
        } else {
            approval.setText(Reviewer.NO_SCORE);
        }
    }

    private void setTrackingUser(Integer user) {
        Prefs.setTrackingUser(mContext, user);
        if (!Prefs.isTabletMode(mContext)) mActivity.finish();
    }

    private void setIndicies(@NotNull Cursor cursor) {
        // These indices will not change regardless of the view
        if (mReviewerId_index == null) {
            mReviewerId_index = cursor.getColumnIndex(UserReviewers.C_REVIEWER_ID);
        }
        if (mReviewerEmail_index == null) {
            mReviewerEmail_index = cursor.getColumnIndex(UserReviewers.C_EMAIL);
        }
        if (mReviewerName_index == null) {
            mReviewerName_index = cursor.getColumnIndex(UserReviewers.C_NAME);
        }
        if (mCodeReview_index == null) {
            mCodeReview_index = cursor.getColumnIndex(UserReviewers.C_CODE_REVIEW);
        }
        if (mVerified_index == null) {
            mVerified_index = cursor.getColumnIndex(UserReviewers.C_VERIFIED);
        }
    }


    private static class ViewHolder {
        TextView reviewer;
        ViewGroup codeReviewLayout;
        TextView codeReview;
        ViewGroup verifiedLayout;
        TextView verified;

        public ViewHolder(View view) {
            reviewer = (TextView) view.findViewById(R.id.labels_card_reviewer_name);
            codeReviewLayout = (ViewGroup) view.findViewById(R.id.labels_card_code_review_layout);
            codeReview = (TextView) view.findViewById(R.id.labels_card_code_review);
            verifiedLayout = (ViewGroup) view.findViewById(R.id.labels_card_verified_layout);
            verified = (TextView) view.findViewById(R.id.labels_card_verified);
        }
    }
}
