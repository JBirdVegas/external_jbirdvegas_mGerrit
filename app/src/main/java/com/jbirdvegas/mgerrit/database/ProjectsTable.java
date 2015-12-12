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
import android.support.v4.content.CursorLoader;
import android.util.Pair;

import com.google.gerrit.extensions.common.ProjectInfo;
import com.jbirdvegas.mgerrit.helpers.DBParams;

import java.util.ArrayList;
import java.util.List;

public class ProjectsTable extends DatabaseTable {
    // Table name
    public static final String TABLE = "Projects";

    // Columns
    public static final String C_PATH = "path";
    public static final String C_ROOT = "base";
    public static final String C_SUBPROJECT = "project";
    public static final String C_DESCRIPTION = "desc";
    // Don't bother storing the kind - it is constant

    public static final String[] PRIMARY_KEY = { C_ROOT, C_SUBPROJECT };

    public static final int ITEM_LIST = UriType.ProjectsList.ordinal();
    public static final int ITEM_ID = UriType.ProjectsID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // Sort by condition for querying results.
    public static final String SORT_BY = C_ROOT + " ASC, " + C_SUBPROJECT + " ASC";

    public static final String SEPARATOR = "/";

    private static ProjectsTable mInstance = null;

    public static ProjectsTable getInstance() {
        if (mInstance == null) mInstance = new ProjectsTable();
        return mInstance;
    }

    @Override
    public void create(String tag, SQLiteDatabase db) {
        db.execSQL("create table " + TABLE + " ("
                + C_PATH + " text PRIMARY KEY ON CONFLICT IGNORE, "
                + C_ROOT + " text NOT NULL, "
                + C_SUBPROJECT + " text NOT NULL, "
                + C_DESCRIPTION + " text)" // This will probably be null
        );
    }

    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    /** Insert the list of projects into the database **/
    public static int insertProjects(Context context, List<ProjectInfo> projects) {

        List<ContentValues> projectValues = new ArrayList<>();

        for (ProjectInfo project : projects) {
            ContentValues projectRow = new ContentValues(2);

            String path = project.name;
            projectRow.put(C_PATH, path);
            Pair<String, String> proj = splitPath(path);
            projectRow.put(C_ROOT, proj.first);
            projectRow.put(C_SUBPROJECT, proj.second);

            projectValues.add(projectRow);
        }

        // We are only inserting PK columns so we should use the IGNORE resolution algorithm.
        Uri uri = DBParams.insertWithIgnore(CONTENT_URI);

        ContentValues valuesArray[] = new ContentValues[projectValues.size()];
        return context.getContentResolver().bulkInsert(uri, projectValues.toArray(valuesArray));
    }

    /**
     * Get a list of distinct columns in the database.
     * @param context Application context reference
     * @return A new CursorLoader object
     */
    public static CursorLoader getProjects(Context context, final String query) {

        String columns[] = { "rowid AS _id", C_ROOT};
        String newQuery, whereQuery[];
        if (query == null || query.length() < 1) newQuery = null;
        else newQuery = "%" + query + "%";

        StringBuilder where = new StringBuilder(0).append(C_ROOT).append(" <> ''");

        if (newQuery != null) {
            where.append(" AND ").append(C_PATH).append(" LIKE ?");
            whereQuery = new String[] { newQuery };
        }
        else {
            whereQuery = null;
        }

        Uri uri = DBParams.groupBy(CONTENT_URI, C_ROOT);
        return new CursorLoader(context, uri,
                columns, where.toString(), whereQuery,
                SORT_BY);
    }

    // Return a cursor containing all of the subprojects under one root project
    public static Cursor getSubprojects(Context context, final String rootProject, final String query) {

        String[] projection = new String[] { "rowid as _id", C_SUBPROJECT};
        String newQuery, whereQuery[];
        if (query == null || query.length() < 1) newQuery = null;
        else newQuery = "%" + query + "%";

        StringBuilder where = new StringBuilder(0);
        where.append(C_ROOT).append(" = ? AND ").append(C_SUBPROJECT).append(" <> ''");

        if (newQuery != null) {
            where.append(" AND ").append(C_PATH).append(" LIKE ?");
            whereQuery = new String[] { rootProject, newQuery };
        }
        else {
            whereQuery = new String[] { rootProject };
        }
        return context.getContentResolver().query(CONTENT_URI, projection,
                where.toString(), whereQuery,SORT_BY);
    }

    // Split a project's path (name) into its root and subproject
    private static Pair<String, String> splitPath(String projectPath) {
        String p[] = projectPath.split(SEPARATOR, 2);
        if (p.length < 2) return new Pair<>(p[0], "");
        else return new Pair<>(p[0], p[1]);
    }
}
