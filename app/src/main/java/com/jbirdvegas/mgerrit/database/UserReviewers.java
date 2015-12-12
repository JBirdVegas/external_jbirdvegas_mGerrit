package com.jbirdvegas.mgerrit.database;

/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2014
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
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.CursorLoader;

/*
 * Virtual table combining both Users and Changes tables for join queries.
 * The content provider needs a URI to determine where to query which maps
 *  to a specific table. Since it does not make sense to favour one table
 *  over another when querying, a virtual/join table is used instead.
 *
 *  Note: Insertion or removal is not supported on this table, only queries
 */
public class UserReviewers extends DatabaseTable {
    // Table name
    public static final String TABLE = "UserReviewers";

    // --- Columns in Reviewers table---
    public static final String C_CHANGE_ID = Reviewers.C_CHANGE_ID;
    public static final String C_CODE_REVIEW = Reviewers.C_CODE_REVIEW;
    public static final String C_VERIFIED = Reviewers.C_VERIFIED;

    // --- Columns in Users table ---
    // The numeric ID of the account (Identical to UserChanges.C_OWNER)
    public static final String C_REVIEWER_ID = "reviewerID";

    /* The email address the user prefers to be contacted through.
     *  Although this appears unique, users can change their preferred email which may change this */
    public static final String C_EMAIL = Users.C_EMAIL;

    // The full name of the user.
    public static final String C_NAME = Users.C_NAME;

    // --- Content Provider stuff ---
    public static final int ITEM_LIST = UriType.UserReviewersList.ordinal();
    public static final int ITEM_ID = UriType.UserReviewersID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // Sort by condition for querying results.
    public static final String SORT_BY = Reviewers.SORT_BY;

    public static final String[] PROJECTION = new String[] {
            Reviewers.TABLE + ".rowid AS _id", C_CODE_REVIEW, C_VERIFIED,
            Users.C_ACCOUNT_ID + " AS " + C_REVIEWER_ID, C_EMAIL, C_NAME };

    private static UserReviewers mInstance = null;
    private MyObserver mObserver;

    public static UserReviewers getInstance() {
        if (mInstance == null) mInstance = new UserReviewers();
        return mInstance;
    }

    @Override
    public void create(String tag, SQLiteDatabase db) {
        // This is not a real table (do nothing)
    }

    @SuppressWarnings("unused")
    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    /**
     * Get the reviewers for a change
     * @param context Context for database access
     * @param changeid The Change-Id of the change to get the reviewers for
     * @return A CursorLoader
     */
    public static CursorLoader getReviewersForChange(Context context, String changeId) {
        return new CursorLoader(context, CONTENT_URI, PROJECTION,
                C_CHANGE_ID + " = ? AND " + Reviewers.TABLE + "." + Reviewers.C_USER
                        + " = " + Users.TABLE + "." + Users.C_ACCOUNT_ID,
                new String[] { changeId }, SORT_BY);
    }

    @Override
    protected void registerContentObserver(Context context) {
        mObserver = new MyObserver(new Handler(), context, CONTENT_URI);
        context.getContentResolver().registerContentObserver(Users.CONTENT_URI, true,
                mObserver);
        context.getContentResolver().registerContentObserver(Reviewers.CONTENT_URI, true,
                mObserver);
    }

    @Override
    protected void unRegisterContentObserver(Context context) {
        context.getContentResolver().unregisterContentObserver(mObserver);
    }
}
