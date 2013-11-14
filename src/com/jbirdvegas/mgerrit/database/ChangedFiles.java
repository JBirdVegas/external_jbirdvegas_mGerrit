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
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.jbirdvegas.mgerrit.helpers.DBParams;
import com.jbirdvegas.mgerrit.objects.FileInfo;

import java.util.ArrayList;
import java.util.List;

public class ChangedFiles extends DatabaseTable {

    // Table name
    public static final String TABLE = "ChangedFiles";

    // --- Columns ---
    // The Change-Id of the change where these files were modified
    public static final String C_CHANGE_ID = "change_id";

    // The full pathname of the file that was changed
    public static final String C_FILENAME = "path";

    // The number of lines inserted
    public static final String C_INSERTED = "lines_inserted";

    // The number of lines deleted
    public static final String C_DELETED = "lines_deleted";

    // The status of this file
    public static final String C_FILE_STATUS = "status";

    // The whehter the file is binary or text
    public static final String C_BINARY = "is_binary";

    public static final String[] PRIMARY_KEY = { C_CHANGE_ID, C_FILENAME };

    public static final int ITEM_LIST = UriType.ChangedFilesList.ordinal();
    public static final int ITEM_ID = UriType.ChangedFilesID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // Sort by condition for querying results.
    public static final String SORT_BY = ChangedFiles.C_FILENAME + " ASC";

    private static ChangedFiles mInstance = null;

    public static ChangedFiles getInstance() {
        if (mInstance == null) mInstance = new ChangedFiles();
        return mInstance;
    }

    @Override
    public void create(String TAG, SQLiteDatabase db) {
        // Specify a conflict algorithm here so we don't have to worry about it later
        db.execSQL("create table " + TABLE + " ("
                + C_CHANGE_ID + " text NOT NULL, "
                + C_FILENAME + " text NOT NULL, "
                + C_INSERTED + " INTEGER, "
                + C_DELETED + " INTEGER, "
                + C_FILE_STATUS + " TEXT NOT NULL, "
                + C_BINARY + " INTEGER NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (" + C_CHANGE_ID + ", " + C_FILENAME + ") ON CONFLICT REPLACE, "
                + "FOREIGN KEY (" + C_CHANGE_ID + ") REFERENCES "
                + Changes.TABLE + "(" + Changes.C_CHANGE_ID + "))");
    }

    public static void addURIMatches(UriMatcher _urim)
    {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    public static int insertChangedFiles(Context context, String changeid, List<com.jbirdvegas.mgerrit.objects.FileInfo> diff) {

        List<ContentValues> values = new ArrayList<ContentValues>();

        for (FileInfo file : diff) {
            if (file == null) {
                continue;
            }
            ContentValues row = new ContentValues(6);
            row.put(C_CHANGE_ID, changeid);
            row.put(C_FILENAME, file.getPath());
            row.put(C_INSERTED, file.getInserted());
            row.put(C_DELETED, file.getDeleted());
            row.put(C_FILE_STATUS, file.getStatus());
            row.put(C_BINARY, file.isBinary());
            values.add(row);
        }

        Uri uri = DBParams.insertWithReplace(CONTENT_URI);
        ContentValues valuesArray[] = new ContentValues[values.size()];
        return context.getContentResolver().bulkInsert(uri, values.toArray(valuesArray));
    }
}
