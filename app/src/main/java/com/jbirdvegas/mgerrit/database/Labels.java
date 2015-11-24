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

package com.jbirdvegas.mgerrit.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.google.gerrit.extensions.common.LabelInfo;
import com.jbirdvegas.mgerrit.helpers.DBParams;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Labels extends DatabaseTable {
    // Table name
    public static final String TABLE = "Labels";

    // Columns
    public static final String C_PROJECT = "project";
    public static final String C_NAME = "label";
    public static final String C_VALUE = "value";
    public static final String C_DESCRIPTION = "desc";
    public static final String C_PERMITTED = "permitted";
    public static final String C_IS_DEFAULT = "is_default";

    // Triple field composite primary key
    public static final String[] PRIMARY_KEY = { C_PROJECT, C_NAME, C_VALUE };

    public static final int ITEM_LIST = UriType.LabelsList.ordinal();
    public static final int ITEM_ID = UriType.LabelssID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // Sort by condition for querying results.
    public static final String SORT_BY = C_PROJECT + " DESC, " + C_NAME + " DESC";

    private static Labels mInstance = null;

    public static Labels getInstance() {
        if (mInstance == null) mInstance = new Labels();
        return mInstance;
    }

    @Override
    public void create(String TAG, SQLiteDatabase db) {
        db.execSQL("create table " + TABLE + " ("
                + C_PROJECT + " text NOT NULL, "
                + C_NAME + " text NOT NULL, "
                + C_VALUE + " INTEGER NOT NULL, "
                + C_DESCRIPTION + " text, "
                + C_PERMITTED + " INTEGER NOT NULL DEFAULT 0, "
                + C_IS_DEFAULT + " INTEGER NOT NULL DEFAULT 0, "
                + "FOREIGN KEY (" + C_PERMITTED + ") REFERENCES "
                        + ProjectsTable.TABLE + "(" + ProjectsTable.C_PATH + "), "
                + "PRIMARY KEY (" + C_PROJECT + ", " + C_NAME + ", " + C_VALUE + ") ON CONFLICT REPLACE)");
    }

    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    public static int insertLabels(Context context, String project,
                                   Map<String, LabelInfo> labels,
                                   Map<String, Collection<String>> permittedLabels) {

        List<ContentValues> rows = new ArrayList<>();

        for (Map.Entry<String, LabelInfo> infoEntry : labels.entrySet()) {
            ContentValues row = new ContentValues(5);

            String label = infoEntry.getKey();
            Map<String, String> labelValues = infoEntry.getValue().values;

            Collection<String> permittedValues = permittedLabels.get(label);

            for (Map.Entry<String, String> entry : labelValues.entrySet()) {
                boolean isPermitted = permittedValues != null && permittedValues.contains(entry.getKey());

                row.put(C_PROJECT, project);
                row.put(C_NAME, label);
                row.put(C_DESCRIPTION, entry.getValue());
                row.put(C_PERMITTED, isPermitted);

                try {
                    int value = Integer.parseInt(entry.getKey().trim());
                    row.put(C_VALUE, value);
                    row.put(C_IS_DEFAULT, infoEntry.getValue().defaultValue == value);
                } catch (NumberFormatException e) {
                    // Label may not be an int - unlikely but we can still handle it
                    row.put(C_VALUE, entry.getKey());
                    row.put(C_IS_DEFAULT, false);
                }
            }

            rows.add(row);
        }

        // We are only inserting PK columns so we should use the REPLACE resolution algorithm.
        Uri uri = DBParams.insertWithReplace(CONTENT_URI);

        ContentValues valuesArray[] = new ContentValues[rows.size()];
        return context.getContentResolver().bulkInsert(uri, rows.toArray(valuesArray));
    }
}
