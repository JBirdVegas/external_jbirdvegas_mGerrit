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

import com.google.gerrit.extensions.common.ApprovalInfo;
import com.google.gerrit.extensions.common.LabelInfo;
import com.jbirdvegas.mgerrit.helpers.DBParams;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Reviewers extends DatabaseTable {
    // Table name
    public static final String TABLE = "Reviewers";

    // Columns
    // The numeric ID of the account.
    public static final String C_USER = "user";

    public static final String C_CHANGE_ID = "change_id";

    public static final String C_CODE_REVIEW = "code_review";
    public static final String C_VERIFIED = "verified";

    public static final String[] PRIMARY_KEY = { C_USER, C_CHANGE_ID };

    public static final int ITEM_LIST = UriType.ReviewersList.ordinal();
    public static final int ITEM_ID = UriType.ReviewersID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // Sort by condition for querying results.
    public static final String SORT_BY = C_USER + " ASC";

    private static Reviewers mInstance = null;

    public static Reviewers getInstance() {
        if (mInstance == null) mInstance = new Reviewers();
        return mInstance;
    }

    @Override
    public void create(String tag, SQLiteDatabase db) {
        // Specify a conflict algorithm here so we don't have to worry about it later
        db.execSQL("create table " + TABLE + " ("
                + C_USER + " INTEGER, "
                + C_CHANGE_ID + " text, "
                + C_CODE_REVIEW + " text DEFAULT 0, "
                + C_VERIFIED + " text DEFAULT 0, "
                + "PRIMARY KEY (" + C_USER + ", " + C_CHANGE_ID + ") ON CONFLICT REPLACE)");
    }

    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    /** Insert the list of reviewers with their labels into the database **/
    public static int insertReviewers(Context context, String changeid, Map<String, LabelInfo> labels) {
        Uri uri = DBParams.insertWithIgnore(CONTENT_URI);
        List<ContentValues> values = new ArrayList<>();
        // Labels we can store in the database
        String[] permittedLabels = new String[] {"Code-Review", "Verified"};

        for (String label : permittedLabels) {
            LabelInfo labelInfo = labels.get(label);
            // We may not have any users who have given it this label or the server may not use this label
            if (labelInfo != null && labelInfo.all != null) {
                values.addAll(insertLabels(context, changeid, labelInfo.all, label));
            }
        }

        ContentValues valuesArray[] = new ContentValues[values.size()];
        return context.getContentResolver().bulkInsert(uri, values.toArray(valuesArray));
    }

    // Helper to get the list of users with a given label for insertion
    private static List<ContentValues> insertLabels(Context context, String changeId,
                                                    List<ApprovalInfo> approvers, String label)
    {
        List<ContentValues> values = new ArrayList<>();
        // Update each row with the verified/code-review status
        for (ApprovalInfo approver : approvers) {
            ContentValues row = new ContentValues();
            row.put(C_USER, approver._accountId);
            row.put(C_CHANGE_ID, changeId);

            if ("Code-Review".equals(label)) row.put(C_CODE_REVIEW, approver.value);
            else row.put(C_VERIFIED, approver.value);
            values.add(row);
        }
        return values;
    }
}
