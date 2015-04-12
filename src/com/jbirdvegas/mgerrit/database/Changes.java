package com.jbirdvegas.mgerrit.database;

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

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Pair;

import com.jbirdvegas.mgerrit.helpers.DBParams;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

public class Changes extends DatabaseTable {
    // Table name
    public static final String TABLE = "Changes";

    // --- Columns ---
    // The Change-Id of the change.
    public static final String C_CHANGE_ID = "change_id";

    // The legacy numeric ID of the change (used in the web address)
    public static final String C_COMMIT_NUMBER = "_change_number";

    //The subject of the change (header line of the commit message).
    public static final String C_SUBJECT = "subject";

    //The status of the change (NEW, SUBMITTED, MERGED, ABANDONED, DRAFT).
    public static final String C_STATUS = "status";

    // The name of the project (References Project table)
    public static final String C_PROJECT = "project";

    // The owner of the change (References User table)
    public static final String C_OWNER = "owner";

    /* The timestamp of when the change was created.
     * Store as ISO8601 string ("YYYY-MM-DD HH:MM:SS.SSS"). */
    public static final String C_CREATED = "time_created";
    /* The timestamp of when the change was last updated.
     * Store as ISO8601 string ("YYYY-MM-DD HH:MM:SS.SSS"). */
    public static final String C_UPDATED = "time_modified";

    // The topic to which this change belongs.
    public static final String C_TOPIC = "topic";

    // The name of the target branch. The refs/heads/ prefix is omitted.
    public static final String C_BRANCH = "branch";

    public static final String C_IS_STARRED = "starred";

    public static final String[] PRIMARY_KEY = { C_CHANGE_ID };

    public static final int ITEM_LIST = UriType.ChangesList.ordinal();
    public static final int ITEM_ID = UriType.ChangesID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // Sort by condition for querying results.
    public static final String SORT_BY = C_UPDATED + " DESC";

    private static Changes mInstance = null;

    public static Changes getInstance() {
        if (mInstance == null) mInstance = new Changes();
        return mInstance;
    }

    @Override
    public void create(String TAG, SQLiteDatabase db) {
        // Specify a conflict algorithm here so we don't have to worry about it later
        db.execSQL("create table " + TABLE + " ("
                + C_CHANGE_ID + " text PRIMARY KEY ON CONFLICT REPLACE, "
                + C_SUBJECT + " text NOT NULL, "
                + C_CREATED + " INTEGER NOT NULL, "
                + C_UPDATED + " INTEGER NOT NULL ,"
                + C_OWNER + " INTEGER NOT NULL, "
                + C_PROJECT + " text NOT NULL, "
                + C_STATUS + " text DEFAULT '" + JSONCommit.KEY_STATUS_OPEN + "' NOT NULL, "
                + C_TOPIC + " text, "
                + C_BRANCH + " text, "
                + C_COMMIT_NUMBER + " INTEGER NOT NULL, "
                + C_IS_STARRED + " INTEGER NOT NULL DEFAULT 0, "
                + "FOREIGN KEY (" + C_OWNER + ") REFERENCES "
                    + Users.TABLE + "(" + Users.C_ACCOUNT_ID + "), "
                + "FOREIGN KEY (" + C_PROJECT + ") REFERENCES "
                    + ProjectsTable.TABLE + "(" + ProjectsTable.C_PATH + "))");
    }

    @SuppressWarnings("unused")
    public static void addURIMatches(UriMatcher _urim)
    {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    public static String getChangeStatus(Context context, String changeID) {
        Uri uri = DBParams.fetchOneRow(CONTENT_URI);
        String status = null;

        Cursor c = context.getContentResolver().query(uri,
                new String[] { C_STATUS },
                C_CHANGE_ID + " = ?",
                new String[] { changeID },
                null);
        if (c.moveToFirst()) status = c.getString(0);
        c.close();
        return status;
    }

    public static Pair<String, Integer> getMostRecentChange(Context context, String status) {
        Uri uri = DBParams.fetchOneRow(CONTENT_URI);
        Pair<String, Integer> pair = null;

        status = JSONCommit.Status.getStatusString(status);

        Cursor c = context.getContentResolver().query(uri,
                new String[] { C_CHANGE_ID, C_COMMIT_NUMBER },
                C_STATUS + " = ?",
                new String[] { status },
                SORT_BY);
        if (c.moveToFirst()) {
            pair = new Pair<>(c.getString(0), c.getInt(1));
        }
        c.close();
        return pair;
    }

    public static String getChangeUpdatedTime(Context context, String status, boolean newest) {
        Uri uri = DBParams.fetchOneRow(CONTENT_URI);
        String updated = null;

        status = JSONCommit.Status.getStatusString(status);

        String sort;
        if (newest) sort = SORT_BY;
        else sort = C_UPDATED + " ASC";

        Cursor c = context.getContentResolver().query(uri, new String[] { C_UPDATED },
                C_STATUS + " = ?", new String[] { status }, sort);
        if (c.moveToFirst()) updated = c.getString(0);
        if (updated != null && !updated.isEmpty()) {
            /* From the SQLite documentation, a time string will have only one space, which can
             *  be replaced with a 'T' to confirm to the ISO-8601 standard */
            updated = updated.replace(' ', 'T');
        }

        c.close();
        return updated;
    }

    public static String getNewestUpdatedTime(Context context, String status) {
        return getChangeUpdatedTime(context, status, true);
    }

    public static String getOldestUpdatedTime(Context context, String status) {
        return getChangeUpdatedTime(context, status, false);
    }

    public static Integer getChangeNumberForChange(Context context, String changeID) {
        Uri uri = DBParams.fetchOneRow(CONTENT_URI);
        Integer changeNo = null;

        Cursor c = context.getContentResolver().query(uri,
                new String[] { C_COMMIT_NUMBER },
                C_CHANGE_ID + " = ?",
                new String[] { changeID },
                null);
        if (c.moveToFirst()) changeNo = c.getInt(0);
        c.close();
        return changeNo;
    }

    public static void starChange(Context context, String changeId, int changeNumber, boolean isStarred) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(C_IS_STARRED, isStarred);
        Uri uri = DBParams.insertWithReplace(CONTENT_URI);
        context.getContentResolver().update(uri, contentValues, C_CHANGE_ID + " = ? AND " + C_COMMIT_NUMBER + " = ?",
                new String[]{changeId, String.valueOf(changeNumber)});
    }

    public static void unstarAllChanges(Context context) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(C_IS_STARRED, false);
        context.getContentResolver().update(CONTENT_URI, contentValues, C_IS_STARRED + " = 1", null);
    }
}
