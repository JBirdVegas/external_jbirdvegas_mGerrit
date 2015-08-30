package com.jbirdvegas.mgerrit.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.v4.content.CursorLoader;

import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.common.GitPerson;
import com.google.gerrit.extensions.common.RevisionInfo;
import com.jbirdvegas.mgerrit.helpers.DBParams;
import com.jbirdvegas.mgerrit.objects.ChangedFileInfo;

import java.util.ArrayList;
import java.util.Map;

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
 *
 *  Contains information about a mRevision.
 */
public class Revisions extends DatabaseTable {

    // Table name
    public static final String TABLE = "Revisions";

    // --- Columns ---
    // The Change-Id of the change, extracted from the commit message.
    public static final String C_CHANGE_ID = "change_id";

    // The patch set number.
    public static final String C_PATCH_SET_NUMBER = "psNumber";

    // The commit ID, used by Git.
    public static final String C_COMMIT = "commitId";

    /* The author's email of this commit. This information is directly linked to Git so no
     Gerrit username or account id are included. */
    public static final String C_AUTHOR = "author";

    /* The committer's email of this commit. This information is directly linked to Git so no
     Gerrit username or account id are included. */
    public static final String C_COMMITTER = "committer";

    // The commit message
    public static final String C_MESSAGE = "message";

    public static final String[] PRIMARY_KEY = { C_CHANGE_ID };

    public static final int ITEM_LIST = DatabaseTable.UriType.RevisionList.ordinal();
    public static final int ITEM_ID = DatabaseTable.UriType.RevisionID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    private static Revisions mInstance = null;

    public static Revisions getInstance() {
        if (mInstance == null) mInstance = new Revisions();
        return mInstance;
    }

    @Override
    public void create(String TAG, SQLiteDatabase db) {
        // Specify a conflict algorithm here so we don't have to worry about it later
        db.execSQL("create table " + TABLE + " ("
                + C_CHANGE_ID + " text PRIMARY KEY ON CONFLICT REPLACE, "
                + C_PATCH_SET_NUMBER + " INTEGER NOT NULL, "
                + C_COMMIT + " text, "
                + C_AUTHOR + " TEXT ,"
                + C_COMMITTER + " TEXT, "
                + C_MESSAGE + " text, "
                + "FOREIGN KEY (" + C_CHANGE_ID + ") REFERENCES "
                + Changes.TABLE + "(" + Changes.C_CHANGE_ID + "), "
                + "FOREIGN KEY (" + C_AUTHOR + ") REFERENCES "
                + Users.TABLE + "(" + Users.C_ACCOUNT_ID + "), "
                + "FOREIGN KEY (" + C_COMMITTER + ") REFERENCES "
                + Users.TABLE + "(" + Users.C_ACCOUNT_ID + "))");
    }

    @SuppressWarnings("unused")
    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    /**
     * Get the commit message for a change
     * @param context Context for database access
     * @param changeid The ID of the change to get the commit message for
     * @return A CursorLoader
     */
    public static CursorLoader getCommitMessage(Context context, String changeid) {
        Uri uri = DBParams.fetchOneRow(CONTENT_URI);
        return new CursorLoader(context, uri, new String[] { C_MESSAGE },
                C_CHANGE_ID + " = ?", new String[] { changeid }, null);
    }

    public static void insertRevision(Context context, String changeId, RevisionInfo patchSet) {
        if (patchSet == null) {
            return; // We cannot do anything if we don't have any information
        }

        int ps = patchSet._number;
        ContentValues row = new ContentValues(9);

        row.put(C_CHANGE_ID, changeId);
        row.put(C_PATCH_SET_NUMBER, ps);
        row.put(C_COMMIT, patchSet.commit.commit);
        GitPerson author = patchSet.commit.author;
        if (author != null) {
            row.put(C_AUTHOR, author.email);
        }
        GitPerson committer = patchSet.commit.committer;
        if (committer != null) {
            row.put(C_COMMITTER, committer.email);
        }
        row.put(C_MESSAGE, patchSet.commit.message);

        Uri uri = DBParams.insertWithReplace(CONTENT_URI);
        context.getContentResolver().insert(uri, row);

        ArrayList files = new ArrayList(patchSet.files.size());
        for (Map.Entry<String, FileInfo> entry : patchSet.files.entrySet()) {
            files.add(new ChangedFileInfo(entry.getKey(), entry.getValue()));
        }

        // Insert the changed files into the FileInfoTable
        FileInfoTable.insertChangedFiles(context, changeId, ps, files);
    }
}
