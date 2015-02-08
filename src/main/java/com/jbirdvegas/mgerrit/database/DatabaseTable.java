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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.SparseArray;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Base class using reflection to get fields out of the database.
 *  This class defines a contract that implementing classes must obey. Note that there are
 *  (constant) static fields that implementing classes MUST provide (see below).
 */
public abstract class DatabaseTable {

    // Some helpful error messages for not obeying contracts
    protected static final String NO_TABLE_CONST = "Database table must provide static constant 'TABLE'.";
    protected static final String PRIVATE_TABLE_CONST = "'TABLE' constant must not be private.";

    // Static (constant) fields implementing concrete classes MUST define:
    //  public static final String TABLE
    //  public static final int ITEM_LIST
    //  public static final int ITEM_ID
    //  public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);
    //  public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    //  public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    /**
     * Executes an SQL script to instanciate its database table
     * @param TAG For logging purposes
     * @param db An open writable database instance
     */
    public abstract void create(String TAG, SQLiteDatabase db);

    /* Each subclass MUST implement this static method to facilitate construction
     *  of its database table. Obviously it cannot be declared abstract as it
     *  needs to be declared static */
    //public static ProjectsTable getInstance();

    /* Add an element for the List and ID MIME types for each table.
     *  These should be used to define the ITEM_LIST and ITEM_ID constants in each table
     */
    enum UriType {
        ChangesList, ChangesID,
        CommitMarkerList, CommitMarkerID,
        CommitPropertiesList, CommitPropertiesID,
        ConfigList, ConfigID,
        FileChangesList, FileChangesID,
        FileInfoList, FileInfoID,
        MessageInfoList, MessageInfoID,
        MoreChangesList, MoreChangesID,
        ProjectsList, ProjectsID,
        ReviewersList, ReviewersID,
        RevisionList, RevisionID,
        SelectedChangeList, SelectedChangeID,
        SyncTimeList, SyncTimeID,
        UserChangesList, UserChangesID,
        UserMessageList, UserMessageID,
        UserReviewersList, UserReviewersID,
        UsersList, UsersID,
    }

    // Add each DatabaseTable class here, we need to add the virtual tables as well
    public static ArrayList<Class<? extends DatabaseTable>> tables;
    static {
        tables = new ArrayList<>();
        tables.add(Changes.class);
        tables.add(CommitMarker.class);
        tables.add(Config.class);
        tables.add(FileChanges.class);
        tables.add(FileInfoTable.class);
        tables.add(MessageInfo.class);
        tables.add(MoreChanges.class);
        tables.add(ProjectsTable.class);
        tables.add(Reviewers.class);
        tables.add(Revisions.class);
        tables.add(SelectedChange.class);
        tables.add(SyncTime.class);
        tables.add(UserChanges.class);
        tables.add(UserMessage.class);
        tables.add(UserReviewers.class);
        tables.add(Users.class);
    }

    /* Gathers information from the DatabaseTable classes to help implement the
     *  ContentProvider's getType method, which handles requests for the
     *  MIME type of the data at the given URI.
     */
    public static final SparseArray<String> sContentTypeMap;
    static {
        sContentTypeMap = new SparseArray<>();

        for (Class<? extends DatabaseTable> table : DatabaseTable.tables) {
            try {
                // Pass nulls in as these are static fields
                int key = table.getField("ITEM_LIST").getInt(null);
                String value = (String) table.getField("CONTENT_TYPE").get(null);
                sContentTypeMap.append(key, value);

                key = table.getField("ITEM_ID").getInt(null);
                value = (String) table.getField("CONTENT_ITEM_TYPE").get(null);
                sContentTypeMap.append(key, value);

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Table does not provide sufficient MIME type information", e);
            }
        }
    }

    /**
     * Gathers the data so it can be determined what URI corresponds to what table.
     */
    public static final SparseArray<String> sTableMap;
    static {
        sTableMap = new SparseArray<>();

        for (Class<? extends DatabaseTable> table : DatabaseTable.tables) {
            try {
                // Pass nulls in as these are static fields
                int key = table.getField("ITEM_LIST").getInt(null);
                String value = (String) table.getField("TABLE").get(null);
                sTableMap.append(key, value);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Unable to initialise table mappings due to inaccessible field", e);
            }
        }
    }

    /**
     * @param context Context for database access
     * @param uri The Uri of the table to search
     * @return whether there are any rows in the given table
     */
    public static boolean isEmpty(@NotNull Context context, @NotNull Uri uri) {
        boolean empty = false;

        Cursor cursor = context.getContentResolver().query(uri, new String[] { "count(*)" },
                null, null, null);
        if (cursor == null) empty = true;
        cursor.moveToFirst(); // IMPORTANT
        if (!empty) empty = (cursor.getInt(0) <= 0);
        cursor.close();
        return empty;
    }

    protected void registerContentObserver(Context context) {
        // Do nothing by default
    }

    protected void unRegisterContentObserver(Context context) {
        // Do nothing by default
    }
}
