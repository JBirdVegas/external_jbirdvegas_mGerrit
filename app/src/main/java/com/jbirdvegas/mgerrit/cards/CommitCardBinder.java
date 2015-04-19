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
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.requestbuilders.AccountEndpoints;
import com.jbirdvegas.mgerrit.tasks.GerritService;

import java.util.TimeZone;

public class CommitCardBinder implements SimpleCursorAdapter.ViewBinder {
    private final RequestQueue mRequestQuery;
    private final Context mContext;

    private final int mOrange;
    private final int mGreen;
    private final int mRed;

    private final TimeZone mServerTimeZone;
    private final TimeZone mLocalTimeZone;

    // Cursor indices
    private Integer userEmailIndex;
    private Integer userIdIndex;
    private Integer changeIdIndex;
    private Integer changeNumberIndex;

    public CommitCardBinder(Context context, RequestQueue requestQueue) {
        this.mRequestQuery = requestQueue;
        this.mContext = context;

        this.mOrange = context.getResources().getColor(android.R.color.holo_orange_light);
        this.mGreen = context.getResources().getColor(R.color.text_green);
        this.mRed = context.getResources().getColor(R.color.text_red);

        mServerTimeZone = PrefsFragment.getServerTimeZone(context);
        mLocalTimeZone = PrefsFragment.getLocalTimeZone(context);
    }

    @Override
    public boolean setViewValue(View view, final Cursor cursor, final int columnIndex) {
        // These indicies will not change regardless of the view
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
            ImageView imageView = (ImageView) view;
            GravatarHelper.populateProfilePicture((ImageView) view, cursor.getString(userEmailIndex),
                    mRequestQuery);
            imageView.setTag(R.id.user, cursor.getInt(userIdIndex));
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PrefsFragment.setTrackingUser(mContext, (Integer) v.getTag(R.id.user));
                }
            });
        } else if (view.getId() == R.id.commit_card_project_name) {
            final TextView project = (TextView) view;
            project.setText(cursor.getString(columnIndex));
            project.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PrefsFragment.setCurrentProject(mContext, project.getText().toString());
                }
            });
        } else if (view.getId() == R.id.commit_card_commit_status) {
            String statusText = cursor.getString(columnIndex);
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
        } else if (view.getId() == R.id.commit_card_last_updated) {
            String lastUpdated = cursor.getString(columnIndex);
            TextView textView = (TextView) view;
            textView.setText(prettyPrintDate(mContext, lastUpdated));
        } else if (view.getId() == R.id.commit_card_starred) {
            final ImageView star = (ImageView) view;
            final int starred = cursor.getInt(columnIndex);

            setStarIcon(star, starred == 1);
            star.setTag(R.id.changeID, cursor.getString(changeIdIndex));
            star.setTag(R.id.changeNumber, cursor.getInt(changeNumberIndex));

            star.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String changeId = (String) star.getTag(R.id.changeID);
                    int changeNumber = (int) star.getTag(R.id.changeNumber);
                    onStarChange(changeId, changeNumber, starred != 1);
                    setStarIcon(star, starred == 1);
                }
            });
        } else {
            return false;
        }
        return true;
    }

    /**
     * PrettyPrint the Gerrit provided timestamp format into a more human readable format
     */
    @SuppressWarnings("SimpleDateFormatWithoutLocale")
    private String prettyPrintDate(Context context, String date) {
       return Tools.prettyPrintTime(context, date, mServerTimeZone, mLocalTimeZone);
    }

    // When the cursor changes, these may not be valid
    public void onCursorChanged() {
        userEmailIndex = null;
        userIdIndex = null;
        changeIdIndex = null;
        changeNumberIndex = null;
    }

    private void setStarIcon(ImageView view, boolean starred) {
        if (starred) {
            view.setImageResource(Tools.getResIdFromAttribute(mContext, R.attr.starredIcon));
        } else {
            view.setImageResource(Tools.getResIdFromAttribute(mContext, R.attr.unstarredIcon));
        }
    }

    private void onStarChange(String changeId, int changeNumber, boolean starred) {
        AccountEndpoints url = AccountEndpoints.starChange(changeId);
        Intent it = new Intent(mContext, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Star);
        it.putExtra(GerritService.URL_KEY, url);
        it.putExtra(GerritService.CHANGE_ID, changeId);
        it.putExtra(GerritService.CHANGE_NUMBER, changeNumber);
        it.putExtra(GerritService.IS_STARRING, starred);
        mContext.startService(it);
    }
}
