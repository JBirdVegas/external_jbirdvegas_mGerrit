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
import android.os.Handler;
import android.support.v4.content.CursorLoader;

import com.jbirdvegas.mgerrit.helpers.DBParams;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Virtual table combining both Users and Changes tables for join queries.
 * The content provider needs a URI to determine where to query which maps
 *  to a specific table. Since it does not make sense to favour one table
 *  over another when quering, a virtual/join table is used instead.
 *
 *  Note: Insertion or removal is not supported on this table, only queries
 */
public class UserChanges extends DatabaseTable {
    // Table name
    public static final String TABLE = "UserChanges";

    // --- Columns in Changes table---
    // The Change-Id of the change.
    public static final String C_CHANGE_ID = Changes.C_CHANGE_ID;

    //The subject of the change (header line of the commit message).
    public static final String C_SUBJECT = Changes.C_SUBJECT;

    //The status of the change (NEW, SUBMITTED, MERGED, ABANDONED, DRAFT).
    public static final String C_STATUS = Changes.C_STATUS;

    // The name of the project (References Project table)
    public static final String C_PROJECT = Changes.C_PROJECT;

    // The owner of the change (References User table)
    public static final String C_OWNER = Changes.C_OWNER;

    /* The timestamp of when the change was created.
     * Store as ISO8601 string ("YYYY-MM-DD HH:MM:SS.SSS"). */
    public static final String C_CREATED = Changes.C_CREATED;
    /* The timestamp of when the change was last updated.
     * Store as ISO8601 string ("YYYY-MM-DD HH:MM:SS.SSS"). */
    public static final String C_UPDATED = Changes.C_UPDATED;

    // The topic to which this change belongs.
    public static final String C_TOPIC = Changes.C_TOPIC;

    // The legacy numeric ID of the change (used in the web address)
    public static final String C_COMMIT_NUMBER = "_change_number";


    // --- Columns in Users table ---
    // The numeric ID of the account.
    public static final String C_USER_ID = Users.C_ACCOUNT_ID;

    /* The email address the user prefers to be contacted through.
     *  Although this appears unique, users can change their prefered email which may change this */
    public static final String C_EMAIL = Users.C_EMAIL;

    // The full name of the user.
    public static final String C_NAME = Users.C_NAME;


    // --- Content Provider stuff ---
    public static final int ITEM_LIST = UriType.UsersChangesList.ordinal();
    public static final int ITEM_ID = UriType.UsersChangesID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;


    // Sort by condition for querying results.
    public static final String SORT_BY = C_UPDATED + " DESC";

    private static final String[] CHANGE_LIST_PROJECTION = new String[] {
            C_CHANGE_ID, C_SUBJECT, C_PROJECT, C_UPDATED,
            C_STATUS, C_TOPIC, C_USER_ID, C_EMAIL, C_NAME,
            C_COMMIT_NUMBER };

    private static UserChanges mInstance = null;
    private MyObserver mObserver;

    public static UserChanges getInstance() {
        if (mInstance == null) mInstance = new UserChanges();
        return mInstance;
    }

    @Override
    public void create(String TAG, SQLiteDatabase db) {
        // This is not a real table (do nothing)
    }

    @SuppressWarnings("unused")
    public static void addURIMatches(UriMatcher _urim)
    {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    /**
     * List the commits for a given change status
     * @param context Context for database access
     * @param status The change status to search for
     * @param committerID The email of the committer
     * @param project The full path of the project to search for
     * @return A CursorLoader
     */
    public static CursorLoader listCommits(Context context, String status, Integer committerID,
                                           String project) {
        StringBuilder builder = new StringBuilder();
        List<String> bindArgs = new ArrayList<String>();
        boolean addAnd = false;

        if (committerID != null) {
            builder.append(C_USER_ID).append(" = ?");
            bindArgs.add(committerID.toString());
            addAnd = true;
        }

        if (project != null) {
            if (addAnd) builder.append(" AND ");
            builder.append(C_PROJECT).append(" = ?");
            bindArgs.add(project);
        }

        return findCommits(context, status, builder, bindArgs);
    }

    public static CursorLoader listCommits(Context context, String status, CommitterObject committer,
                                           String project) {
        Integer accountID = null;
        if (committer != null)  accountID = committer.getAccountId();
        return listCommits(context, status, accountID, project);
    }

    /** Insert the list of commits into the database **/
    public static int insertCommits(Context context, List<JSONCommit> commits) {

        List<ContentValues> values = new ArrayList<ContentValues>();
        Set<CommitterObject> committers = new HashSet<CommitterObject>();

        for (JSONCommit commit : commits) {
            ContentValues row = new ContentValues(9);

            row.put(C_CHANGE_ID, commit.getChangeId());
            row.put(C_SUBJECT, commit.getSubject());
            row.put(C_COMMIT_NUMBER, commit.getCommitNumber());
            row.put(C_CREATED, trimDate(commit.getCreatedDate()));
            row.put(C_UPDATED, trimDate(commit.getLastUpdatedDate()));
            row.put(C_OWNER, commit.getOwnerObject().getAccountId());
            row.put(C_PROJECT, commit.getProject());
            row.put(C_STATUS, commit.getStatus().toString());
            row.put(C_TOPIC, commit.getTopic());
            values.add(row);

            committers.add(commit.getOwnerObject());
        }

        // Insert the list of users into the database as well.
        CommitterObject usersArray[] = new CommitterObject[committers.size()];
        Users.insertUsers(context, committers.toArray(usersArray));

        // Now insert the commits
        Uri uri = DBParams.insertWithReplace(Changes.CONTENT_URI);
        ContentValues valuesArray[] = new ContentValues[values.size()];
        return context.getContentResolver().bulkInsert(uri, values.toArray(valuesArray));
    }

    /**
     * List the commits for a given change status and subject
     * @param context Context for database access
     * @param status The change status to search for
     * @param subject A full or partial commit message string to search for
     * @return A CursorLoader
     */
    public static CursorLoader findCommitsWithSubject(Context context, String status,
                                                      String subject) {
        StringBuilder builder = new StringBuilder();
        List<String> bindArgs = new ArrayList<String>();

        if (subject != null) {
            builder.append(" AND ").append(C_PROJECT).append(" LIKE ?");
            bindArgs.add("%" + subject + "%");
        }
        return findCommits(context, status, builder, bindArgs);
    }

    /**
     * List the commits that start with the given change id
     * @param context Context for database access
     * @param changeID
     * @return
     */
    public static CursorLoader findCommitWithChangeID(Context context, String changeID) {

        if (changeID == null) return null;

        StringBuilder where = new StringBuilder().append(Changes.TABLE).append(".").append(C_OWNER)
                .append(" = ").append(Users.TABLE).append(".").append(C_USER_ID)
                .append(" AND ").append(C_CHANGE_ID).append(" LIKE ?");

        return new CursorLoader(context, CONTENT_URI, CHANGE_LIST_PROJECTION,
                where.toString(), new String[] { changeID + "%" }, SORT_BY);
    }

    // Removes the extraneous 0s off the milliseconds in server timestamps
    private static String trimDate(String date) {
        return date.substring(0, date.length() - 6);
    }

    /**
     * List the commits for a given change status and subject
     * @param context Context for database access
     * @param status The change status to search for
     * @param query A constructed where query string
     * @param args Any bind arguments to be bound to the SQL query
     * @return A CursorLoader
     */
    public static CursorLoader findCommits(Context context, String status,
                                           String query, List<String> args) {
        StringBuilder builder = new StringBuilder(query);
        return findCommits(context, status, builder, args);
    }

    /**
     * Helper method for change list search queries
     * @param context Context for database access
     * @param status The change status to search for
     * @param builder A string builder to help form the where query
     * @param bindArgs Any bind arguments to be bound to the SQL query
     * @return A CursorLoader
     */
    private static CursorLoader findCommits(Context context, String status,
                                           StringBuilder builder, List<String> bindArgs) {
        if (builder.length() > 0) builder.append(" AND ");

        StringBuilder where = builder.append(C_STATUS).append(" = ?").append(" AND ")
                .append(Changes.TABLE).append(".").append(C_OWNER)
                .append(" = ").append(Users.TABLE).append(".").append(C_USER_ID);

        status = JSONCommit.Status.getStatusString(status);
        bindArgs.add(status);

        String valuesArray[] = new String[bindArgs.size()];

        return new CursorLoader(context, CONTENT_URI, CHANGE_LIST_PROJECTION,
                where.toString(), bindArgs.toArray(valuesArray), SORT_BY);
    }

    @Override
    protected void registerContentObserver(Context context) {
        mObserver = new MyObserver(new Handler(), context, CONTENT_URI);
        context.getContentResolver().registerContentObserver(Users.CONTENT_URI, true,
                mObserver);
        context.getContentResolver().registerContentObserver(Changes.CONTENT_URI, true,
                mObserver);
    }

    @Override
    protected void unRegisterContentObserver(Context context) {
        context.getContentResolver().unregisterContentObserver(mObserver);
    }
}
