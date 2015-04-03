package com.jbirdvegas.mgerrit.database;

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

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.jbirdvegas.mgerrit.helpers.DBParams;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.ArrayList;
import java.util.List;

public class StarredChanges extends DatabaseTable {
    // Table name
    public static final String TABLE = "StarredChanges";

    // --- Columns ---
    // The Change-Id of the change.
    public static final String C_CHANGE_ID = "change_id";

    // The legacy numeric ID of the change (used in the web address)
    public static final String C_COMMIT_NUMBER = "_change_number";

    public static final String[] PRIMARY_KEY = { C_CHANGE_ID, C_COMMIT_NUMBER };

    public static final int ITEM_LIST = UriType.StarredChangesList.ordinal();
    public static final int ITEM_ID = UriType.StarredChangesID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // Sort by condition for querying results.
    public static final String SORT_BY = C_CHANGE_ID + " ASC";

    private static StarredChanges mInstance = null;

    public static StarredChanges getInstance() {
        if (mInstance == null) mInstance = new StarredChanges();
        return mInstance;
    }

    @Override
    public void create(String TAG, SQLiteDatabase db) {
        // Specify a conflict algorithm here so we don't have to worry about it later
        db.execSQL("create table " + TABLE + " ("
                + C_CHANGE_ID + " text NOT NULL, "
                + C_COMMIT_NUMBER + " INTEGER NOT NULL, "
                + "PRIMARY KEY (" + C_CHANGE_ID + ", " + C_COMMIT_NUMBER + ") ON CONFLICT REPLACE, "
                + "FOREIGN KEY (" + C_CHANGE_ID + ") REFERENCES "
                    + Changes.TABLE + "(" + Changes.C_CHANGE_ID + "), "
                + "FOREIGN KEY (" + C_COMMIT_NUMBER + ") REFERENCES "
                    + Changes.TABLE + "(" + Changes.C_COMMIT_NUMBER + "))");

        // We need at least one row in this table for querying changes to return results.
        //  So add an invalid row in (we will need to filter this out later)
        db.execSQL(String.format("INSERT INTO %s VALUES ('%s', %s)", TABLE, "INVALID", 0));
    }

    @SuppressWarnings("unused")
    public static void addURIMatches(UriMatcher _urim)
    {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    public static int insertChanges(Context context, List<JSONCommit> commits) {
        List<ContentValues> values = new ArrayList<>();
        for (JSONCommit commit : commits) {
            ContentValues row = new ContentValues(2);
            row.put(C_CHANGE_ID, commit.getChangeId());
            row.put(C_COMMIT_NUMBER, commit.getCommitNumber());
            values.add(row);
        }

        // Now insert the commits
        Uri uri = DBParams.insertWithReplace(Changes.CONTENT_URI);
        ContentValues valuesArray[] = new ContentValues[values.size()];
        return context.getContentResolver().bulkInsert(uri, values.toArray(valuesArray));
    }

    public static void starChange(Context context, String changeId, int changeNumber) {
        ContentValues values = new ContentValues(2);
        values.put(C_CHANGE_ID, changeId);
        values.put(C_COMMIT_NUMBER, changeNumber);
        context.getContentResolver().insert(CONTENT_URI, values);
    }

    public static boolean unstarChange(Context context, String changeId, int changeNumber) {
        String where = C_CHANGE_ID + " = ? AND " + C_COMMIT_NUMBER + " = ?";
        return context.getContentResolver().delete(CONTENT_URI, where, new String[] { changeId, String.valueOf(changeNumber)}) > 0;
    }

    public static boolean isChangeStarred(Context context, String changeId) {
        String where = C_CHANGE_ID + " = ?";
        Uri uri = DBParams.fetchOneRow(CONTENT_URI);
        Cursor c = context.getContentResolver().query(uri, new String[]{C_CHANGE_ID}, where, new String[]{changeId}, null);
        return c.moveToFirst();
    }

    public static boolean isChangeStarred(Context context, int changeNumber) {
        String where = C_COMMIT_NUMBER + " = ?";
        Uri uri = DBParams.fetchOneRow(CONTENT_URI);
        Cursor c = context.getContentResolver().query(uri, new String[]{C_COMMIT_NUMBER}, where, new String[]{String.valueOf(changeNumber)}, null);
        return c.moveToFirst();
    }
}
