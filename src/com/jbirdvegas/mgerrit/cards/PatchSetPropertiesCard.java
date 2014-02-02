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
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.helpers.Tools;

import org.jetbrains.annotations.NotNull;

public class PatchSetPropertiesCard implements CardBinder {
    private final RequestQueue mRequestQuery;
    private final Context mContext;
    private final FragmentActivity mActivity;
    private final LayoutInflater mInflater;

    // Colors
    private final int mOrange;
    private final int mGreen;
    private final int mRed;

    private Integer changeid_index;
    private Integer changenum_index;
    private Integer branch_index;
    private Integer subject_index;
    private Integer topic_index;
    private Integer updated_index;
    private Integer authorId_index;
    private Integer authorEmail_index;
    private Integer authorName_index;
    private Integer status_index;


    public PatchSetPropertiesCard(Context context, RequestQueue requestQueue) {
        this.mRequestQuery = requestQueue;
        this.mContext = context;
        this.mActivity = (FragmentActivity) mContext;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.mOrange = context.getResources().getColor(android.R.color.holo_orange_light);
        this.mGreen = context.getResources().getColor(R.color.text_green);
        this.mRed = context.getResources().getColor(R.color.text_red);
    }

    @Override
    public View setViewValue(Cursor cursor, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.properties_card, null);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (convertView.getTag() == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        setIndicies(cursor);

        String lastUpdated = cursor.getString(updated_index);
        viewHolder.updated.setText(Tools.prettyPrintDate(mContext, lastUpdated,
                Prefs.getServerTimeZone(mContext),
                Prefs.getLocalTimeZone(mContext)));


        viewHolder.subject.setText(cursor.getString(subject_index));
        viewHolder.branch.setText(cursor.getString(branch_index));

        setupUserDetails(viewHolder.author,
                cursor.getInt(authorId_index),
                cursor.getString(authorName_index),
                cursor.getString(authorName_index));

        String topic = cursor.getString(topic_index);
        if (topic != null && !topic.isEmpty()) {
            viewHolder.topic.setText(topic);
            viewHolder.topicContainer.setVisibility(View.VISIBLE);
        } else {
            viewHolder.topicContainer.setVisibility(View.GONE);
        }

        setClicksToActionViews(cursor, viewHolder.shareBtn, viewHolder.browserBtn);

        String statusText = cursor.getString(status_index);
        switch (statusText) {
            case "MERGED":
                viewHolder.status.setBackgroundColor(mGreen);
                break;
            case "ABANDONED":
                viewHolder.status.setBackgroundColor(mRed);
                break;
            default:
                viewHolder.status.setBackgroundColor(mOrange);
                break;
        }

        return convertView;
    }

   private void setClicksToActionViews(Cursor cursor, ImageView share, ImageView browser) {

        String webAddress = getWebAddress(cursor.getString(changenum_index));

        share.setTag(R.id.webAddress, webAddress);
        share.setTag(R.id.changeID, cursor.getString(changeid_index));
        browser.setTag(R.id.webAddress, webAddress);

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String changeId = (String) view.getTag(R.id.changeID);
                String webAddress = (String) view.getTag(R.id.webAddress);

                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.putExtra(Intent.EXTRA_SUBJECT,
                        String.format(view.getContext().getString(R.string.commit_shared_from_mgerrit),
                                changeId));
                intent.putExtra(Intent.EXTRA_TEXT, webAddress + " #mGerrit");
                view.getContext().startActivity(intent);
            }
        });
        browser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String webAddress = (String) view.getTag(R.id.webAddress);

                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(webAddress));
                view.getContext().startActivity(browserIntent);
            }
        });
    }

    private void setupUserDetails(final TextView view,
                                  final int id, final String email, final String name) {
        view.setTag(id);

        // attach owner's gravatar
        GravatarHelper.attachGravatarToTextView(view, email, mRequestQuery);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTrackingUser((Integer) view.getTag());
            }
        });

        setImageCaption(view, R.string.commit_owner, name);
    }

    private void setImageCaption(TextView textView, int resID, String authorName) {
        String title = mContext.getResources().getString(resID);
        if (title == null || authorName == null) return;

        SpannableString text = new SpannableString(title + "\n" + authorName);
        text.setSpan(new TextAppearanceSpan(mContext, R.style.CardText_CommitOwnerText),
                0, title.length(), 0);

        int end = title.length()+1;
        text.setSpan(new TextAppearanceSpan(mContext, R.style.CardText_CommitOwnerDetails),
                end, end+authorName.length(), 0);

        textView.setText(text, TextView.BufferType.SPANNABLE);
    }

    private void setTrackingUser(Integer user) {
        Prefs.setTrackingUser(mContext, user);
        if (!Prefs.isTabletMode(mContext)) mActivity.finish();
    }

    private String getWebAddress(String changeNum) {
        // Web address: Gerrit instance + commit number
        return Prefs.getCurrentGerrit(mContext) + changeNum;
    }

    private void setIndicies(@NotNull Cursor cursor) {
        // These indices will not change regardless of the view
        if (changeid_index == null) {
            changeid_index = cursor.getColumnIndex(UserChanges.C_CHANGE_ID);
        }
        if (changenum_index == null) {
            changenum_index = cursor.getColumnIndex(UserChanges.C_COMMIT_NUMBER);
        }
        if (branch_index == null) {
            branch_index = cursor.getColumnIndex(UserChanges.C_BRANCH);
        }
        if (subject_index == null) {
            subject_index = cursor.getColumnIndex(UserChanges.C_SUBJECT);
        }
        if (topic_index == null) {
            topic_index = cursor.getColumnIndex(UserChanges.C_TOPIC);
        }
        if (authorId_index == null) {
            authorId_index = cursor.getColumnIndex(UserChanges.C_USER_ID);
        }
        if (authorEmail_index == null) {
            authorEmail_index = cursor.getColumnIndex(UserChanges.C_EMAIL);
        }
        if (authorName_index == null) {
            authorName_index = cursor.getColumnIndex(UserChanges.C_NAME);
        }
        if (updated_index == null) {
            updated_index = cursor.getColumnIndex(UserChanges.C_UPDATED);
        }
        if (status_index == null) {
            status_index = cursor.getColumnIndex(UserChanges.C_STATUS);
        }
    }


    private static class ViewHolder {
        private final TextView subject;
        private final TextView author;
        private final TextView branch;
        private final View topicContainer;
        private final TextView topic;
        private final TextView updated;
        private final View status;

        private final ImageView shareBtn;
        private final ImageView browserBtn;

        ViewHolder(View view) {
            subject = (TextView) view.findViewById(R.id.prop_card_subject);
            author = (TextView) view.findViewById(R.id.prop_card_author);
            branch = (TextView) view.findViewById(R.id.prop_card_branch);
            topicContainer = view.findViewById(R.id.prop_card_topic_container);
            topic = (TextView) view.findViewById(R.id.prop_card_topic);
            updated = (TextView) view.findViewById(R.id.prop_card_last_update);
            status = view.findViewById(R.id.prop_card_status);

            shareBtn = (ImageView) view.findViewById(R.id.properties_card_share_info);
            browserBtn = (ImageView) view.findViewById(R.id.properties_card_view_in_browser);
        }
    }
}