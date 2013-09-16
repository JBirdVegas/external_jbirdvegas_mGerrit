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

import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.jbirdvegas.mgerrit.objects.JSONCommit;

public class Changes extends DatabaseTable {
    // Table name
    public static final String TABLE = "Changes";

    // --- Columns ---
    // The Change-Id of the change.
    public static final String C_CHANGE_ID = "change_id";

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
                + "FOREIGN KEY (" + C_OWNER + ") REFERENCES "
                    + Users.TABLE + "(" + Users.C_EMAIL + "), "
                + "FOREIGN KEY (" + C_PROJECT + ") REFERENCES "
                    + ProjectsTable.TABLE + "(" + ProjectsTable.C_PATH + "))");
    }

    @SuppressWarnings("unused")
    public static void addURIMatches(UriMatcher _urim)
    {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }
}
