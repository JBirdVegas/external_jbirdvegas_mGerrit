/*
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2015
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

package com.jbirdvegas.mgerrit.database;

import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.CursorLoader;

import com.jbirdvegas.mgerrit.helpers.DBParams;

/**
 * View to get the labels and what the user has rated a change with those labels
 */
public class ReviewerLabels extends DatabaseTable {

    // Table name
    public static final String TABLE = "ReviewerLabels";

    // Columns (Users table)
    public static final String C_ACCOUNT_ID = Users.TABLE + "." + Users.C_ACCOUNT_ID;

    // --- Columns (Changes table) ---
    public static final String C_CHANGE_ID = Changes.TABLE + "." + Changes.C_CHANGE_ID;
    public static final String C_PROJECT = Changes.TABLE + "." + Changes.C_PROJECT;

    // --- Columns (Labels table) ---
    public static final String C_LABEL = Labels.TABLE + "." + Labels.C_NAME;
    public static final String C_VALUE = Labels.TABLE + "." + Labels.C_VALUE;
    public static final String C_DESCRIPTION = Labels.TABLE + "." + Labels.C_DESCRIPTION;
    public static final String C_IS_DEFAULT = Labels.TABLE + "." + Labels.C_IS_DEFAULT;
    private static final String C_LABEL_PROJECT = Labels.TABLE + "." + Labels.C_PROJECT;

    // --- Inferred column from Reviewers table ---
    // What rating the user gave for this label
    public static final String C_REVIEWED_VALUE = "reviewed_value";
    private static final String C_REVIEWERS_CHANGE_ID = Reviewers.TABLE + "." + Reviewers.C_CHANGE_ID;
    private static final String C_REVIEWERS_USER = Reviewers.TABLE + "." + Reviewers.C_USER;

    public static final int ITEM_LIST = UriType.ReviewerLabelsList.ordinal();
    public static final int ITEM_ID = UriType.ReviewerLabelsID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // Sort by condition for querying results.
    public static final String SORT_BY = FileInfoTable.SORT_BY;

    public static final String[] PROJECTION = new String[] {
            ReviewerLabels.TABLE + ".rowid AS _id",
            C_ACCOUNT_ID, C_CHANGE_ID, C_LABEL, C_VALUE, C_DESCRIPTION, C_IS_DEFAULT, C_REVIEWED_VALUE };


    private static ReviewerLabels mInstance = null;
    private MyObserver mObserver;

    public static ReviewerLabels getInstance() {
        if (mInstance == null) mInstance = new ReviewerLabels();
        return mInstance;
    }

    @Override
    public void create(String tag, SQLiteDatabase db) {
        /*
        SELECT Changes.change_id, Changes.project, label, value, desc, is_default, account_id,
        case when label = 'Code-Review' then code_review WHEN label = 'Verified' then verified end as reviewed_value FROM Labels, Users, Changes
        LEFT OUTER JOIN Reviewers ON (Changes.change_id = Reviewers.change_id AND Users.account_id = Reviewers.user)
        WHERE Users.http_user IS NOT NULL AND Users.http_password IS NOT NULL AND
        Changes.project = Labels.project

        SELECT Changes.change_id, Changes.project, Labels.label, Labels.value, Labels.desc, Labels.is_default,
        Users.account_id, CASE WHEN label = 'Code-Review' THEN code_review WHEN label = 'Verified' THEN verified END AS reviewed_value
        FROM Labels, Users, Changes
        LEFT OUTER JOIN Reviewers ON (Changes.change_id = Reviewers.change_id AND Users.account_id = Reviewers.user)
        WHERE Users.http_user IS NOT NULL AND Users.http_password IS NOT NULL AND Changes.project = Labels.project;

         */
        // You cannot have a view without it being complex :)
        db.execSQL("CREATE VIEW " + TABLE + " AS (" +
                "SELECT " + C_CHANGE_ID + ", " + C_PROJECT + ", " + C_LABEL + ", " + C_VALUE + ", " + C_DESCRIPTION + ", " +
                C_IS_DEFAULT + ", " + C_ACCOUNT_ID + ", " +
                "CASE WHEN label = 'Code-Review' THEN code_review WHEN label = 'Verified' THEN verified END AS " + C_REVIEWED_VALUE + " " +
                "FROM " + Labels.TABLE + ", " + Users.TABLE + ", " + Changes.TABLE + " " +
                "LEFT OUTER JOIN " + Reviewers.TABLE + " ON " +
                "(" + C_CHANGE_ID + " = " + C_REVIEWERS_CHANGE_ID + " AND " +
                C_ACCOUNT_ID + " = " + C_REVIEWERS_USER + ") " +
                "WHERE " + Users.TABLE + ".http_user IS NOT NULL AND " + Users.TABLE + ".http_password IS NOT NULL AND " +
                C_PROJECT + " = " + C_LABEL_PROJECT + ");");
    }

    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    /**
     * Get the last used reviewer labels
     * @param context
     * @param changeId
     * @return
     */
    public static CursorLoader getReviewerLabels(Context context, String changeId) {
        String[] args = new String[]{changeId};

        return new CursorLoader(context, DBParams.fetchOneRow(CONTENT_URI), PROJECTION,
                C_CHANGE_ID + " = ?", args, null);
    }

    @Override
    protected void registerContentObserver(Context context) {
        mObserver = new MyObserver(new Handler(), context, CONTENT_URI);
        context.getContentResolver().registerContentObserver(Users.CONTENT_URI, true,
                mObserver);
        context.getContentResolver().registerContentObserver(Reviewers.CONTENT_URI, true,
                mObserver);
        context.getContentResolver().registerContentObserver(Labels.CONTENT_URI, true,
                mObserver);
    }

    @Override
    protected void unRegisterContentObserver(Context context) {
        context.getContentResolver().unregisterContentObserver(mObserver);
    }
}