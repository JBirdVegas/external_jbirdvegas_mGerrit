package com.jbirdvegas.mgerrit.helpers;

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

import java.util.HashMap;
import java.util.Map;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * Helper class for content providers that operate on databases.
 */
public class DBParams {

    private DBParams() { } // Empty private constructor

    private static final String TAG_LIMIT = "limit";
    private static final String TAG_CONFLICT = "conflict";
    private static final String TAG_GROUP_BY = "group_by";

    public static Uri appendLimitParameter(Uri uri, int numRows) {
        return uri.buildUpon().appendQueryParameter(TAG_LIMIT, Integer.toString(numRows)).build();
    }

    /** Convenience method that is short-hand for appending a limit parameter of 1 **/
    public static Uri fetchOneRow(Uri uri) {
        return appendLimitParameter(uri, 1);
    }

    public static Uri insertWithReplace(Uri uri) {
        return uri.buildUpon().appendQueryParameter(TAG_CONFLICT, "REPLACE").build();
    }

    public static Uri insertWithIgnore(Uri uri) {
        return uri.buildUpon().appendQueryParameter(TAG_CONFLICT, "IGNORE").build();
    }

    public static Uri insertOrUpdate(Uri uri) {
        return uri.buildUpon().appendQueryParameter("duplicate", "UPDATE")
                .appendQueryParameter(TAG_CONFLICT, "IGNORE")
                .build();
    }

    public static Integer getConflictParameter(Uri uri) {
        String conflictAlgorithm = uri.getQueryParameter(TAG_CONFLICT);
        if (conflictAlgorithm == null) return null;
        else if (conflictAlgorithm.equals("REPLACE")) {
            return SQLiteDatabase.CONFLICT_REPLACE;
        } else if (conflictAlgorithm.equals("IGNORE")) {
            return SQLiteDatabase.CONFLICT_IGNORE;
        } else {
            Log.w(DBParams.class.getSimpleName(),
                    "The conflict algorithm '" + conflictAlgorithm + "' is not supported");
            return null;
        }
    }

    public static Integer getLimitParameter(Uri uri) {
        String limit = uri.getQueryParameter(TAG_LIMIT);
        if (limit == null || limit.isEmpty()) return null;
        return Integer.valueOf(limit);
    }

    public static Map<String, Integer> getParameters(Uri uri) {
        Map<String, Integer> params = new HashMap<String, Integer>();
        Integer conflict = getConflictParameter(uri);
        if (conflict != null) params.put(TAG_CONFLICT, conflict);

        Integer limit = getLimitParameter(uri);
        if (limit != null) params.put(TAG_LIMIT, limit);

        return params;
    }

    public static boolean updateOnDuplicateInsertion(Uri uri) {
        String dup = uri.getQueryParameter("duplicate");
        return dup != null && dup.equals("UPDATE");
    }

    public static Uri groupBy(Uri uri, String condition) {
        return uri.buildUpon().appendQueryParameter(TAG_GROUP_BY, condition).build();
    }

    public static String getGroupByCondition(Uri uri) {
        return uri.getQueryParameter(TAG_GROUP_BY);
    }
}
