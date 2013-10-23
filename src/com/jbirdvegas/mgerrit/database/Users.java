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
import android.util.Log;

import com.jbirdvegas.mgerrit.helpers.DBParams;
import com.jbirdvegas.mgerrit.objects.CommitterObject;

import java.util.ArrayList;
import java.util.List;

public class Users extends DatabaseTable {
    // Table name
    public static final String TABLE = "Users";

    // Columns
    // The email address the user prefers to be contacted through. This appears to be unique
    public static final String C_EMAIL = "email";

    // The full name of the user.
    public static final String C_NAME = "name";

    // The numeric ID of the account.
    public static final String C_ACCOUNT_ID = "account_id";

    public static final String[] PRIMARY_KEY = { C_EMAIL };

    public static final int ITEM_LIST = UriType.UsersList.ordinal();
    public static final int ITEM_ID = UriType.UsersID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // Sort by condition for querying results.
    public static final String SORT_BY = C_NAME + " ASC";

    private static Users mInstance = null;

    public static Users getInstance() {
        if (mInstance == null) mInstance = new Users();
        return mInstance;
    }

    @Override
    public void create(String TAG, SQLiteDatabase db) {
        // Specify a conflict algorithm here so we don't have to worry about it later
        db.execSQL("create table " + TABLE + " ("
                + C_ACCOUNT_ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE, "
                + C_EMAIL + " text, "
                + C_NAME + " text NOT NULL)");
    }

    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    /** Insert the list of users into the database **/
    public static int insertUsers(Context context, CommitterObject[] users) {

        List<ContentValues> values = new ArrayList<ContentValues>();

        for (CommitterObject user : users) {
            if (user == null) {
                continue;
            }
            ContentValues row = new ContentValues();
            row.put(C_ACCOUNT_ID, user.getAccountId());
            row.put(C_EMAIL, user.getEmail());
            String name = user.getName();
            if (name != null && name.length() > 0) {
                row.put(C_NAME, name);
            } else {
                Log.w(TABLE, String.format("User with account id %d has no name.", user.getAccountId()));
                row.put(C_NAME, "Unknown");
            }
            values.add(row);
        }

        Uri uri = DBParams.insertWithReplace(CONTENT_URI);

        ContentValues valuesArray[] = new ContentValues[values.size()];
        return context.getContentResolver().bulkInsert(uri, values.toArray(valuesArray));
    }

    public static Uri insertUser(Context context, String name, String email) {
        ContentValues userValues = new ContentValues(2);
        userValues.put(C_EMAIL, email);
        userValues.put(C_NAME, name);
        return context.getContentResolver().insert(CONTENT_URI, userValues);
    }

    /**
     * Get the details for all the users matching name
     * @param context Application context reference
     * @param name The name to search for
     * @return A cursor object
     */
    public static Cursor getUserEmail(Context context, String name) {
        String columns[] = { C_NAME, C_EMAIL};

        return context.getContentResolver().query(CONTENT_URI,
                columns, C_NAME + " = ?", new String[] {name},
                SORT_BY);
    }

    /**
     * Get the name for the user with the given email address
     * @param context Application context reference
     * @param email The email address to search for
     * @return A cursor object
     */
    public static Cursor getUserName(Context context, String email) {
        String columns[] = { "rowid AS _id", C_NAME};

        return context.getContentResolver().query(CONTENT_URI,
                columns, C_EMAIL + " = ?", new String[] {email},
                null);
    }
}
