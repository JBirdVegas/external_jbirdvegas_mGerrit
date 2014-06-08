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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.EmoticonSupportHelper;

public class PatchSetMessageCard implements CardBinder {

    private final Context mContext;
    private final LayoutInflater mInflater;

    public PatchSetMessageCard(Context context) {
        mContext = context;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View setViewValue(Cursor cursor, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.patchset_message_card, parent, false);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (convertView.getTag() == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        // We are only getting one item from the cursor, so just get the first one
        String message = cursor.getString(0);
        if (message != null && !message.isEmpty()) {
            viewHolder.commitMessageTextView.setText(
                    EmoticonSupportHelper.getSmiledText(mContext, message));
        } else {
            viewHolder.commitMessageTextView.setText(
                    mContext.getString(R.string.current_revision_is_draft_message));
        }
        return convertView;
    }

    private static class ViewHolder {
        TextView commitMessageTextView;

        ViewHolder(View view) {
            commitMessageTextView = (TextView) view.findViewById(R.id.message_card_message);
        }
    }
}
