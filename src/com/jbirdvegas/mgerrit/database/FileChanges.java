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
    public static final String C_CHANGE_ID = FileInfoTable.C_CHANGE_ID;

    // The patch set number.
    public static final String C_PATCH_SET_NUMBER = FileInfoTable.C_PATCH_SET_NUMBER;

    public static final String C_FILE_NAME = FileInfoTable.C_FILE_NAME;

    /* The status of the file ("A"=Added, "D"=Deleted, "R"=Renamed, "C"=Copied, "W"=Rewritten).
     * Not set if the file was Modified ("M"). optional. */
    public static final String C_STATUS = FileInfoTable.C_STATUS;

    // Whether the file is binary.
    public static final String C_ISBINARY = FileInfoTable.C_ISBINARY;

    // The old file path. Only set if the file was renamed or copied.
    public static final String C_OLDPATH = FileInfoTable.C_OLDPATH;

    // Number of inserted lines. Not set for binary files or if no lines were inserted.
    public static final String C_LINES_INSERTED = FileInfoTable.C_LINES_INSERTED;

    // Number of deleted lines. Not set for binary files or if no lines were deleted.
    public static final String C_LINES_DELETED = FileInfoTable.C_LINES_DELETED;

    // Whether the file is an image.
    public static final String C_ISIMAGE = FileInfoTable.C_ISIMAGE;

    // --- Columns (Changes table) ---

    // The legacy numeric ID of the change (used in the web address)
    public static final String C_COMMIT_NUMBER = Changes.C_COMMIT_NUMBER;


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
            C_ISBINARY, C_OLDPATH, C_ISIMAGE,
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
     * Get details on what files changed in a change
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

    /**
     * Get details for the files which we can show diff details for in the DiffViewer
     * @param context Context for database access
     * @param changeNumber The number of the change to get the file changes for
     * @return A CursorLoader
     */
    public static CursorLoader getDiffableFiles(Context context, Integer changeNumber) {
        String[] PROJECTION = new String[] {
                FileInfoTable.TABLE + ".rowid AS _id", C_FILE_NAME, C_ISBINARY, C_COMMIT_NUMBER,
                C_PATCH_SET_NUMBER, FileInfoTable.TABLE + "." + C_STATUS,
                C_LINES_INSERTED, C_LINES_DELETED};

        return new CursorLoader(context, CONTENT_URI, PROJECTION,
                Changes.TABLE + "." + C_COMMIT_NUMBER + " = ? AND "
                        + FileInfoTable.TABLE + "." + C_CHANGE_ID
                        + " = " + Changes.TABLE + "." + Changes.C_CHANGE_ID
                        + " AND (" + C_ISBINARY + " = 0 OR " + C_ISIMAGE + " = 1)",
                new String[] { String.valueOf(changeNumber) }, SORT_BY);
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