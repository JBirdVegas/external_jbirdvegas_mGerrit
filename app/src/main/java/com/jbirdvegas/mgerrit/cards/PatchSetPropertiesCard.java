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
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.helpers.Tools;

import org.jetbrains.annotations.NotNull;

public class PatchSetPropertiesCard implements CardBinder {
    private final Context mContext;
    private final LayoutInflater mInflater;

    private Integer changeid_index;
    private Integer changenum_index;
    private Integer branch_index;
    private Integer subject_index;
    private Integer topic_index;
    private Integer updated_index;
    private Integer created_index;


    public PatchSetPropertiesCard(Context context, RequestQueue requestQueue) {
        this.mContext = context;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View setViewValue(Cursor cursor, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.properties_card, parent, false);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (convertView.getTag() == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        setIndicies(cursor);
        viewHolder.changeId.setText(cursor.getString(changeid_index));

        String strTime = cursor.getString(updated_index);
        viewHolder.updated.setText(Tools.prettyPrintDateTime(mContext, strTime,
                PrefsFragment.getServerTimeZone(mContext),
                PrefsFragment.getLocalTimeZone(mContext)));

        strTime = cursor.getString(created_index);
        viewHolder.created.setText(Tools.prettyPrintDateTime(mContext, strTime,
                PrefsFragment.getServerTimeZone(mContext),
                PrefsFragment.getLocalTimeZone(mContext)));

        viewHolder.branch.setText(cursor.getString(branch_index));

        String topic = cursor.getString(topic_index);
        if (topic != null && !topic.isEmpty()) {
            viewHolder.topic.setText(topic);
            viewHolder.topic.setVisibility(View.VISIBLE);
            viewHolder.topicText.setVisibility(View.VISIBLE);
        } else {
            viewHolder.topic.setVisibility(View.GONE);
            viewHolder.topicText.setVisibility(View.GONE);
        }

        return convertView;
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
        if (updated_index == null) {
            updated_index = cursor.getColumnIndex(UserChanges.C_UPDATED);
        }
        if (created_index == null) {
            created_index = cursor.getColumnIndex(UserChanges.C_CREATED);
        }

    }


    private static class ViewHolder {
        private final TextView branch;
        private final TextView topic;
        private final TextView topicText;
        private final TextView updated;
        private final TextView created;
        private final TextView changeId;

        ViewHolder(View view) {
            changeId = (TextView) view.findViewById(R.id.prop_card_change_id);
            branch = (TextView) view.findViewById(R.id.prop_card_branch);
            topic = (TextView) view.findViewById(R.id.prop_card_topic);
            topicText = (TextView) view.findViewById(R.id.commit_topic_text);
            updated = (TextView) view.findViewById(R.id.prop_card_last_update);
            created = (TextView) view.findViewById(R.id.prop_card_created);
        }
    }
}