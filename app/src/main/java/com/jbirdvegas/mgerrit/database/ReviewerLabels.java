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

/**
 * View to get the labels and what the user has rated a change with those labels
 */
public class ReviewerLabels extends DatabaseTable {

    // Table name
    public static final String TABLE = "ReviewerLabels";

    // Columns (Users table)
    public static final String C_ACCOUNT_ID = Users.C_ACCOUNT_ID;

    // --- Columns (Changes table) ---
    public static final String C_CHANGE_ID = Changes.C_CHANGE_ID;
    public static final String C_PROJECT = Changes.C_PROJECT;

    // --- Columns (Labels table) ---
    public static final String C_LABEL = Labels.C_NAME;
    public static final String C_VALUE = Labels.C_VALUE;
    public static final String C_DESCRIPTION = Labels.C_DESCRIPTION;
    public static final String C_IS_DEFAULT = Labels.C_IS_DEFAULT;
    private static final String C_LABEL_PROJECT = Labels.C_PROJECT;

    // --- Inferred column from Reviewers table ---
    // What rating the user gave for this label
    public static final String C_REVIEWED_VALUE = "reviewed_value";
    private static final String C_USER = Reviewers.C_USER;

    public static final int ITEM_LIST = UriType.ReviewerLabelsList.ordinal();
    public static final int ITEM_ID = UriType.ReviewerLabelsID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // Sort by condition for querying results.
    public static final String SORT_BY = FileInfoTable.SORT_BY;

    public static final String[] PROJECTION = new String[] {
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
        SELECT C.change_id, C.project, L.label, L.value, L.desc, L.is_default,
        U.account_id, CASE WHEN L.label = 'Code-Review' THEN code_review WHEN L.label = 'Verified' THEN verified END AS reviewed_value
        FROM Labels L, Users U, Changes C
        LEFT OUTER JOIN Reviewers R ON (C.change_id = R.change_id AND U.account_id = R.user)
        WHERE U.http_user IS NOT NULL AND U.http_password IS NOT NULL AND C.project = L.project;
         */
        // You cannot have a view without it being complex :)
        db.execSQL("CREATE VIEW IF NOT EXISTS " + TABLE + " AS SELECT "
                + "C." + C_CHANGE_ID + " AS " + C_CHANGE_ID + ", C." + C_PROJECT + " AS " + C_PROJECT
                + ", L." + C_LABEL + " AS " + C_LABEL + ", L." + C_VALUE + " AS " + C_VALUE
                + ", L." + C_DESCRIPTION + " AS " + C_DESCRIPTION + ", L." + C_IS_DEFAULT + " AS " + C_IS_DEFAULT
                + ", U." + C_ACCOUNT_ID + " AS " + C_ACCOUNT_ID
                + ", (CASE WHEN L." + C_LABEL + " = 'Code-Review' THEN code_review WHEN L." + C_LABEL + " = 'Verified' THEN verified END) AS " + C_REVIEWED_VALUE
                + " FROM `" + Changes.TABLE + "` C, " + Users.TABLE + " U, " + Labels.TABLE + " L "
                + "LEFT OUTER JOIN " + Reviewers.TABLE + " R ON "
                + "(C." + C_CHANGE_ID + " = R." + C_CHANGE_ID + " AND "
                + "U." + C_ACCOUNT_ID + " = R." + C_USER + ") "
                + "WHERE U.http_user IS NOT NULL AND U.http_password IS NOT NULL AND "
                + "C." + C_PROJECT + " = L." + C_LABEL_PROJECT + ";");
    }

    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    /**
     * @param context Application context from which to access the database
     * @param changeId The change ID to get the labels for
     * @return The last used reviewer labels for this change.
     */
    public static CursorLoader getReviewerLabels(Context context, String changeId) {
        // This is really simple since we did all the work in the view
        String[] args = new String[]{changeId};
        return new CursorLoader(context, CONTENT_URI, PROJECTION,
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