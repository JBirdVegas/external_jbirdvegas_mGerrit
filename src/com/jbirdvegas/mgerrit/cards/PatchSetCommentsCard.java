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
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.caches.BitmapLruCache;
import com.jbirdvegas.mgerrit.database.UserMessage;
import com.jbirdvegas.mgerrit.helpers.EmoticonSupportHelper;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.helpers.Tools;

import org.jetbrains.annotations.NotNull;

public class PatchSetCommentsCard implements CardBinder {

    private RequestQueue mRequestQuery;
    private Context mContext;
    private final FragmentActivity mActivity;
    private final LayoutInflater mInflater;

    // Cursor indices
    private Integer message_index;
    private Integer authorId_index;
    private Integer authorName_index;
    private Integer authorEmail_index;
    private Integer timestamp_index;


    public PatchSetCommentsCard(Context context, RequestQueue requestQueue) {
        mRequestQuery = requestQueue;
        mContext = context;
        mActivity = (FragmentActivity) context;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public View setViewValue(Cursor cursor, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.commit_comment, parent, false);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        setIndicies(cursor);

        TextView author = viewHolder.authorTextView;
        Integer authorNumber = cursor.getInt(authorId_index);
        author.setTag(authorNumber);
        author.setText(cursor.getString(authorName_index));
        author.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTrackingUser((Integer) v.getTag());
            }
        });

        String timestamp = cursor.getString(timestamp_index);
        if (timestamp != null) {
            viewHolder.timestamp.setText(Tools.prettyPrintDate(mContext, timestamp,
                    Prefs.getServerTimeZone(mContext),
                    Prefs.getLocalTimeZone(mContext)));
        }

        // setup styled comments
        // use Linkify to automatically linking http/email/addresses
        Linkify.addLinks(viewHolder.commentMessage, Linkify.ALL);
        // replace replace emoticons with drawables
        viewHolder.commentMessage.setText(EmoticonSupportHelper.getSmiledText(mContext,
                cursor.getString(message_index)));

        // set gravatar icon for commenter
        viewHolder.gravatar.setImageUrl(GravatarHelper.getGravatarUrl(
                cursor.getString(authorEmail_index)),
                new ImageLoader(mRequestQuery, new BitmapLruCache(mContext)));

        return convertView;
    }

    private void setTrackingUser(Integer user) {
        Prefs.setTrackingUser(mContext, user);
        if (!Prefs.isTabletMode(mContext)) mActivity.finish();
    }

    private void setIndicies(@NotNull Cursor cursor) {
        // These indices will not change regardless of the view
        if (message_index == null) {
            message_index = cursor.getColumnIndex(UserMessage.C_MESSAGE);
        }
        if (authorId_index == null) {
            authorId_index = cursor.getColumnIndex(UserMessage.C_AUTHOR);
        }
        if (authorName_index == null) {
            authorName_index = cursor.getColumnIndex(UserMessage.C_NAME);
        }
        if (authorEmail_index == null) {
            authorEmail_index = cursor.getColumnIndex(UserMessage.C_EMAIL);
        }
        if (timestamp_index == null) {
            timestamp_index = cursor.getColumnIndex(UserMessage.C_TIMESTAMP);
        }
    }


    private static class ViewHolder {
        TextView authorTextView;
        TextView commentMessage;
        NetworkImageView gravatar;
        TextView timestamp;

        private ViewHolder(View view) {
            authorTextView = (TextView) view.findViewById(R.id.comment_author_name);
            commentMessage = (TextView) view.findViewById(R.id.comment_message);
            gravatar = (NetworkImageView) view.findViewById(R.id.comment_gravatar);
            timestamp = (TextView) view.findViewById(R.id.comment_timestamp);
        }
    }
}
