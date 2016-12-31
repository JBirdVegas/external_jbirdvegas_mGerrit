package com.jbirdvegas.mgerrit.cards;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2013
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
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.objects.CacheManager;
import com.jbirdvegas.mgerrit.tasks.GerritService;

import java.util.TimeZone;

public class CommitCardBinder implements SimpleCursorAdapter.ViewBinder, CardBinder {
    private final RequestQueue mRequestQuery;
    private final Context mContext;
    private final ImageLoader mImageLoader;

    private final int mOrange;
    private final int mGreen;

    private final int mRed;
    private final TimeZone mServerTimeZone;
    private final TimeZone mLocalTimeZone;
    private final LayoutInflater mInflater;
    private final boolean mIsChangeList;

    // Cursor indices
    private Integer userEmailIndex;
    private Integer userIdIndex;
    private Integer changeIdIndex;
    private Integer changeNumberIndex;
    private Integer userNameIndex;
    private Integer statusIndex;
    private Integer starredIndex;
    private Integer projectIndex;
    private Integer subjectIndex;

    public CommitCardBinder(Context context, RequestQueue requestQueue, boolean changeList) {
        this.mRequestQuery = requestQueue;
        this.mContext = context;

        this.mOrange = ContextCompat.getColor(context, R.color.text_orange);
        this.mGreen = ContextCompat.getColor(context, R.color.text_green);
        this.mRed = ContextCompat.getColor(context, R.color.text_red);

        mServerTimeZone = PrefsFragment.getServerTimeZone(context);
        mLocalTimeZone = PrefsFragment.getLocalTimeZone(context);
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mIsChangeList = changeList;

        mImageLoader = new ImageLoader(mRequestQuery, CacheManager.getImageCache());
    }

    // ViewBinder - For including in commit list
    @Override
    public boolean setViewValue(View view, final Cursor cursor, final int columnIndex) {
        // These indicies will not change regardless of the view
        setupIndicies(cursor);

        if (view.getId() == R.id.commit_card_commit_owner) {
            bindOwnerDetails(view, cursor, columnIndex);
        } else if (view.getId() == R.id.commit_card_committer_image) {
            bindOwnerDetails(view, cursor, columnIndex);
        } else if (view.getId() == R.id.commit_card_project_name) {
            ((TextView)view).setText(cursor.getString(columnIndex));
        } else if (view.getId() == R.id.commit_card_commit_status) {
            bindStatus(view, cursor.getString(columnIndex));
        } else if (view.getId() == R.id.commit_card_last_updated) {
            String lastUpdated = cursor.getString(columnIndex);
            TextView textView = (TextView) view;
            textView.setText(Tools.prettyPrintTime(mContext, lastUpdated, mServerTimeZone, mLocalTimeZone));
        } else if (view.getId() == R.id.commit_card_starred) {
            bindStarred((ImageView) view, cursor, cursor.getInt(columnIndex));
        } else {
            return false;
        }
        return true;
    }

    // CardBinder - For including in change details list
    @Override
    public View setViewValue(Cursor cursor, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.commit_details_card, parent, false);
        }
        setupIndicies(cursor);

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (convertView.getTag() == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        viewHolder.subject.setText(cursor.getString(subjectIndex));

        bindOwnerDetails(viewHolder.owner, cursor, userNameIndex);
        bindOwnerDetails(viewHolder.committerImage, cursor, userIdIndex);
        bindStatus(viewHolder.status, cursor.getString(statusIndex));
        bindProject(viewHolder.project, cursor.getString(projectIndex));
        bindStarred(viewHolder.starred, cursor, cursor.getInt(starredIndex));

        // We are viewing this change so it will be selected
        CommitCard commitCard = (CommitCard) convertView;
        commitCard.setChangeSelected(true);

        return convertView;
    }

    private void setupIndicies(Cursor cursor) {
        if (userNameIndex == null) {
            userNameIndex = cursor.getColumnIndex(UserChanges.C_NAME);
        }
        if (userEmailIndex == null) {
            userEmailIndex = cursor.getColumnIndex(UserChanges.C_EMAIL);
        }
        if (userIdIndex == null) {
            userIdIndex = cursor.getColumnIndex(UserChanges.C_USER_ID);
        }
        if (changeIdIndex == null) {
            changeIdIndex = cursor.getColumnIndex(UserChanges.C_CHANGE_ID);
        }
        if (changeNumberIndex == null) {
            changeNumberIndex = cursor.getColumnIndex(UserChanges.C_COMMIT_NUMBER);
        }
        if (statusIndex == null) {
            statusIndex = cursor.getColumnIndex(UserChanges.C_STATUS);
        }
        if (starredIndex == null) {
            starredIndex = cursor.getColumnIndex(UserChanges.C_STARRED);
        }
        if (projectIndex == null) {
            projectIndex = cursor.getColumnIndex(UserChanges.C_PROJECT);
        }
        if (subjectIndex == null) {
            subjectIndex = cursor.getColumnIndex(UserChanges.C_SUBJECT);
        }
    }

    private void bindOwnerDetails(View view, Cursor cursor, int columnIndex) {
        if (view.getId() == R.id.commit_card_commit_owner) {
            TextView owner = (TextView) view;
            owner.setText(cursor.getString(columnIndex));
            // Set the user so we can get it in the onClickListener
            owner.setTag(cursor.getInt(userIdIndex));
            owner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PrefsFragment.setTrackingUser(mContext, (Integer) v.getTag());
                }
            });
        } else if (view.getId() == R.id.commit_card_committer_image) {
            NetworkImageView imageView = (NetworkImageView) view;
            imageView.setImageUrl(GravatarHelper.getGravatarUrl(cursor.getString(userEmailIndex)), mImageLoader);
            imageView.setDefaultImageResId(R.drawable.gravatar);
            imageView.setTag(R.id.user, cursor.getInt(userIdIndex));
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PrefsFragment.setTrackingUser(mContext, (Integer) v.getTag(R.id.user));
                }
            });
        }
    }

    private void bindStatus(View view, String statusText) {
        switch (statusText) {
            case "MERGED":
                view.setBackgroundColor(mGreen);
                break;
            case "ABANDONED":
                view.setBackgroundColor(mRed);
                break;
            default:
                view.setBackgroundColor(mOrange);
                break;
        }
    }

    private void bindProject(final TextView view, String project) {
        view.setText(project);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrefsFragment.setCurrentProject(mContext, view.getText().toString());
            }
        });
    }

    private void bindStarred(final ImageView view, Cursor cursor, final int starred) {
        setStarIcon(view, starred == 1);
        view.setTag(R.id.changeID, cursor.getString(changeIdIndex));
        view.setTag(R.id.changeNumber, cursor.getInt(changeNumberIndex));

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String changeId = (String) view.getTag(R.id.changeID);
                int changeNumber = (int) view.getTag(R.id.changeNumber);
                onStarChange(changeId, changeNumber, starred != 1);
                setStarIcon(view, starred == 1);
            }
        });
    }

    /**
     * To be called when the cursor changes or is invalidated.
     */
    public void onCursorChanged() {
        // When the cursor changes, these may not be valid
        userEmailIndex = null;
        userIdIndex = null;
        changeIdIndex = null;
        changeNumberIndex = null;
        userNameIndex = null;
        statusIndex = null;
        starredIndex = null;
        projectIndex = null;
        subjectIndex = null;
    }

    private void setStarIcon(ImageView view, boolean starred) {
        if (starred) {
            if (mIsChangeList) {
                view.setVisibility(View.VISIBLE);
            }
            view.setImageResource(Tools.getResIdFromAttribute(mContext, R.attr.starredIcon));
        } else {
            if (mIsChangeList) {
                view.setVisibility(View.GONE);
            } else {
                view.setImageResource(Tools.getResIdFromAttribute(mContext, R.attr.unstarredIcon));
            }
        }
    }

     private void onStarChange(String changeId, int changeNumber, boolean starred) {
         Intent it = new Intent(mContext, GerritService.class);
         it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Star);
         it.putExtra(GerritService.CHANGE_ID, changeId);
         it.putExtra(GerritService.CHANGE_NUMBER, changeNumber);
         it.putExtra(GerritService.IS_STARRING, starred);
         mContext.startService(it);
     }

    private static class ViewHolder {
        private final TextView subject;
        private final TextView owner;
        private final ImageView committerImage;
        private final View status;
        private final TextView project;
        private final ImageView starred;

        ViewHolder(View view) {
            subject = (TextView) view.findViewById(R.id.commit_card_title);
            owner = (TextView) view.findViewById(R.id.commit_card_commit_owner);
            committerImage = (ImageView) view.findViewById(R.id.commit_card_committer_image);
            status = view.findViewById(R.id.commit_card_commit_status);
            project = (TextView) view.findViewById(R.id.commit_card_project_name);
            starred = (ImageView) view.findViewById(R.id.commit_card_starred);
        }
    }
}
