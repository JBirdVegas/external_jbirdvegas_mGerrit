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

import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

import java.util.ArrayList;

/**
 * Base class using reflection to get fields out of the database
 */
public abstract class DatabaseTable {

    // Some helpful error messages for not obeying contracts
    protected static final String NO_TABLE_CONST = "Database table must provide static constant 'TABLE'.";
    protected static final String PRIVATE_TABLE_CONST = "'TABLE' constant must not be private.";

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

    // Add an element for the List and ID MIME types for each table
    enum UriType {
        ProjectsList, ProjectsID,
        UsersList, UsersID,
        ChangesList, ChangesID,
        FileInfoList, FileInfoID,
        MessageInfoList, MessageInfoID,
        SyncTimeList, SyncTimeID,
        UsersChangesList, UsersChangesID
    };

    // Add each DatabaseTable class here, we need to add the virtual tables as well
    public static ArrayList<Class<? extends DatabaseTable>> tables;
    static {
        tables = new ArrayList<Class<? extends DatabaseTable>>();
        tables.add(ProjectsTable.class);
        tables.add(Users.class);
        tables.add(Changes.class);
        tables.add(FileInfo.class);
        tables.add(MessageInfo.class);
        tables.add(SyncTime.class);
        tables.add(UserChanges.class);
    };

    // Each subclass MUST declare the following constant in order to facilitate table creation:
    // public static final String TABLE;

    /* Gathers information from the DatabaseTable classes
     *  to help implement the ContentProvider's getType method, which handles requests for the
     *  MIME type of the data at the given URI.
     */
    public static final SparseArray<String> sContentTypeMap;
    static {
        sContentTypeMap = new SparseArray<String>();

        for (Class<? extends DatabaseTable> table : DatabaseTable.tables) {
            try {
                // Pass nulls in as these are static fields
                int key = table.getField("ITEM_LIST").getInt(null);
                String value = (String) table.getField("CONTENT_TYPE").get(null);
                sContentTypeMap.append(key, value);

                key = table.getField("ITEM_ID").getInt(null);
                value = (String) table.getField("CONTENT_ITEM_TYPE").get(null);
                sContentTypeMap.append(key, value);

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gathers the data so it can be determined what URI corresponds to what table.
     */
    public static final SparseArray<String> sTableMap;
    static {
        sTableMap = new SparseArray<String>();

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
}
