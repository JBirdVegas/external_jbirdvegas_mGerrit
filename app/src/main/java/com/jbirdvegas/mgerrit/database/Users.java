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
import android.util.Log;
import android.util.Pair;

import com.jbirdvegas.mgerrit.helpers.DBParams;
import com.jbirdvegas.mgerrit.objects.AccountInfo;
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

    // Login details for this user - HTTP username
    public static final String C_USENRAME = "http_user";

    // Login details for this user - HTTP password
    public static final String C_PASSWORD = "http_password";

    public static final String[] PRIMARY_KEY = { C_EMAIL };

    public static final int ITEM_LIST = UriType.UsersList.ordinal();
    public static final int ITEM_ID = UriType.UsersID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    // Sort by condition for querying results.
    public static final String SORT_BY = C_NAME + " ASC";

    public static final String[] ALL_PROJECTION = new String[] {
            C_ACCOUNT_ID, C_EMAIL, C_NAME, C_USENRAME, C_PASSWORD };

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
                + C_NAME + " text NOT NULL, "
                + C_USENRAME + " text, "
                + C_PASSWORD + " text)");
    }

    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    /** Insert the list of users into the database **/
    public static int insertUsers(Context context, CommitterObject[] users) {

        List<ContentValues> values = new ArrayList<>();

        AccountInfo self = getUser(context, null);

        for (CommitterObject user : users) {
            ContentValues row = new ContentValues();
            if (user == null) {
                continue;
            } else if (self != null && user.getAccountId() == self._account_id) {
                // If we find ourself in this list, add in the username and password otherwise the
                // user will be signed out.
                row.put(C_USENRAME, self.username);
                row.put(C_PASSWORD, self.password);
            }

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

    public static Uri insertUser(Context context, int id, String name, String email) {
        ContentValues userValues = new ContentValues();
        AccountInfo self = getUser(context, null);

        userValues.put(C_ACCOUNT_ID, id);
        userValues.put(C_EMAIL, email);
        userValues.put(C_NAME, name);

        if (self != null && id == self._account_id) {
            // If we find ourself in this list, add in the username and password otherwise the
            // user will be signed out.
            userValues.put(C_USENRAME, self.username);
            userValues.put(C_PASSWORD, self.password);
        }

        Uri uri = DBParams.insertWithReplace(CONTENT_URI);
        return context.getContentResolver().insert(uri, userValues);
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

    /**
     * Get the details for the user with the given account id
     * @param context Application context reference
     * @param userid A user id
     * @return A cursor object containing at most one row
     */
    public static AccountInfo getUser(Context context, Integer userid) {
        Uri uri = DBParams.fetchOneRow(CONTENT_URI);
        Cursor c;
        AccountInfo ai = null;

        if (userid == null || userid < 0) {
            // If the user id is null then we will treat this as a call to get self
            //  i.e. the currently logged in user
            c = context.getContentResolver().query(uri,
                    ALL_PROJECTION, C_USENRAME + " IS NOT NULL AND " + C_PASSWORD + " IS NOT NULL", null,
                    null);
        } else {
            c = context.getContentResolver().query(uri,
                    ALL_PROJECTION, C_ACCOUNT_ID + " = ?", new String[] { userid.toString() },
                    null);
        }
        if (c.moveToFirst()) {
            ai = new AccountInfo(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4));
        }
        c.close();
        return ai;
    }

    public static CursorLoader getSelf(Context context) {
        Uri uri = DBParams.fetchOneRow(CONTENT_URI);
        return new CursorLoader(context, uri, ALL_PROJECTION,
                C_USENRAME + " IS NOT NULL AND " + C_PASSWORD + " IS NOT NULL", null, null);
    }

    public static void setUserDetails(Context context, AccountInfo info) {
        ContentValues userValues = new ContentValues(5);
        userValues.put(C_ACCOUNT_ID, info._account_id);
        userValues.put(C_EMAIL, info.email);
        userValues.put(C_NAME, info.name);
        userValues.put(C_USENRAME, info.username);
        userValues.put(C_PASSWORD, info.password);
        Uri uri = DBParams.insertWithReplace(CONTENT_URI);
        context.getContentResolver().insert(uri, userValues);
    }

    public static void logout(Context context) {
        ContentValues userValues = new ContentValues(2);
        userValues.putNull(C_USENRAME);
        userValues.putNull(C_PASSWORD);
        context.getContentResolver().update(CONTENT_URI, userValues, C_USENRAME + " IS NOT NULL AND " + C_PASSWORD + " IS NOT NULL", null);
    }
}
