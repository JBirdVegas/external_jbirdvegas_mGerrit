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

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.jbirdvegas.mgerrit.helpers.DBParams;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.tasks.GerritService.Direction;

/**
 * Stores whether there are any older changes for a given status that have
 *  not been retrieved yet.
 */
public class MoreChanges extends DatabaseTable {
    // Table name
    public static final String TABLE = "_MoreChanges";

    // --- Columns ---
    // The query that was executed
    private static final String C_STATUS = "query";

    // Whether we are interested in older or newer changes
    private static final String C_DIRECTION = "direction";

    // Whether there are more changes
    private static final String C_MORE_CHANGES = "more_changes";

    private static final String[] PRIMARY_KEY = { C_STATUS };

    public static final int ITEM_LIST = UriType.MoreChangesList.ordinal();
    public static final int ITEM_ID = UriType.MoreChangesID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    private static MoreChanges mInstance = null;

    public static MoreChanges getInstance() {
        if (mInstance == null) mInstance = new MoreChanges();
        return mInstance;
    }

    @Override
    public void create(String TAG, SQLiteDatabase db) {
        db.execSQL("create table " + TABLE + " ("
                + C_STATUS + " text NOT NULL, "
                + C_DIRECTION + " text NOT NULL, "
                + C_MORE_CHANGES + " INTEGER DEFAULT 1 NOT NULL, "
                + "PRIMARY KEY (" + C_STATUS + ", " + C_DIRECTION + ") ON CONFLICT REPLACE)");
    }

    @SuppressWarnings("unused")
    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    /**
     * Returns whether there are older changes for a given status
     */
    public static boolean areOlderChanges(Context context, String status) {
        Uri uri = DBParams.fetchOneRow(CONTENT_URI);
        status = JSONCommit.Status.getStatusString(status);
        boolean olderChanges = true;

        Cursor c = context.getContentResolver().query(uri,
                new String[] { C_MORE_CHANGES },
                C_STATUS + " = ? AND " + C_DIRECTION + " = ?",
                new String[] { status, Direction.Older.toString() },
                null);
        if (c.moveToFirst()) olderChanges = c.getInt(0) != 0;
        c.close();
        return olderChanges;
    }

    public static void insert(Context context, String status, Direction direction,
                              boolean moreChanges) {
        ContentValues contentValues = new ContentValues(3);
        contentValues.put(C_STATUS, JSONCommit.Status.getStatusString(status));
        contentValues.put(C_DIRECTION, direction.toString());
        contentValues.put(C_MORE_CHANGES, moreChanges ? 1 : 0);

        Uri uri = DBParams.insertWithReplace(CONTENT_URI);
        context.getContentResolver().insert(uri, contentValues);
    }
}
