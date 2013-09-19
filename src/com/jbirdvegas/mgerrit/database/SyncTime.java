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

import com.jbirdvegas.mgerrit.helpers.DBParams;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

public class SyncTime extends DatabaseTable {
    // Table name
    public static final String TABLE = "_Times";

    // --- Columns ---
    private static final String C_KEY = "sync_type";
    private static final String C_VALUE = "value";
    private static final String C_QUERY = "query";

    private static final String[] PRIMARY_KEY = { C_KEY };

    public static final int ITEM_LIST = UriType.SyncTimeList.ordinal();
    public static final int ITEM_ID = UriType.SyncTimeID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // --- Keys ---
    public static final String PROJECTS_LIST_SYNC_TIME = "projects_list";
    public static final String CHANGES_LIST_SYNC_TIME = "changes_list";

    private static SyncTime mInstance = null;

    public static SyncTime getInstance() {
        if (mInstance == null) mInstance = new SyncTime();
        return mInstance;
    }

    @Override
    public void create(String TAG, SQLiteDatabase db) {
        // Specify a conflict algorithm here so we don't have to worry about it later
        db.execSQL("create table " + TABLE + " ("
                + C_KEY + " text, "
                + C_QUERY + " text, "
                + C_VALUE + " INTEGER NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (" + C_KEY + ", " + C_QUERY + ") ON CONFLICT REPLACE)");
    }

    public static void addURIMatches(UriMatcher _urim)
    {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    public static long getValueForQuery(Context context, String key, String query) {
        StringBuilder where = new StringBuilder().append(C_KEY).append(" = ?");
        where.append(" AND ").append(C_QUERY).append(" LIKE ?");

        Cursor c = context.getContentResolver().query(CONTENT_URI,
                new String[] { C_VALUE },
                where.toString(),
                new String[] { key, query + "%" },
                null);
        if (!c.moveToFirst()) return 0;
        else return c.getLong(0);
    }

    public static void setValue(Context context, String key, long value, String query) {
        ContentValues contentValues = new ContentValues(2);
        contentValues.put(C_KEY, key);
        contentValues.put(C_QUERY, query);
        contentValues.put(C_VALUE, value);
        context.getContentResolver().insert(CONTENT_URI, contentValues);
    }
}
