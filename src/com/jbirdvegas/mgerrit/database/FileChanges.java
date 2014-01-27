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

public class FileChanges extends DatabaseTable {

    // Table name
    public static final String TABLE = "FileChanges";

    // --- Columns (FileInfo table)---
    // The Change-Id of the change.
    public static final String C_CHANGE_ID = "change_id";

    // The patch set number.
    public static final String C_PATCH_SET_NUMBER = "psNumber";

    public static final String C_FILE_NAME = "filename";

    /* The status of the file ("A"=Added, "D"=Deleted, "R"=Renamed, "C"=Copied, "W"=Rewritten).
     * Not set if the file was Modified ("M"). optional. */
    public static final String C_STATUS = "status";

    // Whether the file is binary.
    public static final String C_ISBINARY = "binary";

    // The old file path. Only set if the file was renamed or copied.
    public static final String C_OLDPATH = "old_path";

    // Number of inserted lines. Not set for binary files or if no lines were inserted.
    public static final String C_LINES_INSERTED = "lines_inserted";

    // Number of deleted lines. Not set for binary files or if no lines were deleted.
    public static final String C_LINES_DELETED = "lines_deleted";

    // --- Columns (Changes table) ---

    // The legacy numeric ID of the change (used in the web address)
    public static final String C_COMMIT_NUMBER = "_change_number";


    public static final int ITEM_LIST = UriType.FileChangesList.ordinal();
    public static final int ITEM_ID = UriType.FileChangesID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // Sort by condition for querying results.
    public static final String SORT_BY = FileInfoTable.SORT_BY;

    public static final String[] PROJECTION = new String[] {
            FileInfoTable.TABLE + ".rowid AS _id",
            Changes.TABLE + "." + Changes.C_CHANGE_ID,
            Changes.TABLE + "." + Changes.C_COMMIT_NUMBER,
            C_PATCH_SET_NUMBER, C_FILE_NAME,
            FileInfoTable.TABLE + "." + FileInfoTable.C_STATUS,
            C_ISBINARY, C_OLDPATH,
            C_LINES_INSERTED, C_LINES_DELETED };

    private static FileChanges mInstance = null;
    private MyObserver mObserver;

    public static FileChanges getInstance() {
        if (mInstance == null) mInstance = new FileChanges();
        return mInstance;
    }

    @Override
    public void create(String TAG, SQLiteDatabase db) {
        // This is not a real table (do nothing)
    }

    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    /**
     * Get detals on what files changed in a change
     * @param context Context for database access
     * @param changeid The Change-Id of the change to get the file changes for
     * @return A CursorLoader
     */
    public static CursorLoader getFileChanges(Context context, String changeid) {
        return new CursorLoader(context, CONTENT_URI, PROJECTION,
                FileInfoTable.TABLE + "." + C_CHANGE_ID + " = ? AND "
                        + FileInfoTable.TABLE + "." + C_CHANGE_ID
                        + " = " + Changes.TABLE + "." + Changes.C_CHANGE_ID,
                new String[] { changeid }, SORT_BY);
    }

    @Override
    protected void registerContentObserver(Context context) {
        mObserver = new MyObserver(new Handler(), context, CONTENT_URI);
        context.getContentResolver().registerContentObserver(FileInfoTable.CONTENT_URI, true,
                mObserver);
        context.getContentResolver().registerContentObserver(Changes.CONTENT_URI, true,
                mObserver);
    }

    @Override
    protected void unRegisterContentObserver(Context context) {
        context.getContentResolver().unregisterContentObserver(mObserver);
    }
}
