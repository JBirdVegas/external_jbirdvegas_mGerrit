package com.jbirdvegas.mgerrit.database;

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
 */

import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.CursorLoader;

/*
 * Virtual table combining both Users and Changes tables for join queries.
 * The content provider needs a URI to determine where to query which maps
 *  to a specific table. Since it does not make sense to favour one table
 *  over another when querying, a virtual/join table is used instead.
 *
 *  Note: Insertion or removal is not supported on this table, only queries
 */
public class UserMessage extends DatabaseTable {
    // Table name
    public static final String TABLE = "UserMessage";

    // --- Columns in MessageInfo table---
    // The Change-Id of the change.
    public static final String C_CHANGE_ID = MessageInfo.C_CHANGE_ID;

    // The ID of the message.
    public static final String C_MESSAGE_ID = MessageInfo.C_MESSAGE_ID;

    /* Author of the message as an AccountInfo entity. References Users table*/
    public static final String C_AUTHOR = MessageInfo.C_AUTHOR;

    /* The timestamp this message was posted. */
    public static final String C_TIMESTAMP = MessageInfo.C_TIMESTAMP;

    /* The text left by the user. */
    public static final String C_MESSAGE = MessageInfo.C_MESSAGE;

    /* Which patchset (if any) generated this message. */
    public static final String C_REVISION_NUMBER = MessageInfo.C_REVISION_NUMBER;


    // --- Columns in Users table ---
    // The numeric ID of the account (Identical to UserChanges.C_OWNER)
    public static final String C_USER_ID = Users.C_ACCOUNT_ID;

    /* The email address the user prefers to be contacted through.
     *  Although this appears unique, users can change their preferred email which may change this */
    public static final String C_EMAIL = Users.C_EMAIL;

    // The full name of the user.
    public static final String C_NAME = Users.C_NAME;


    // --- Content Provider stuff ---
    public static final int ITEM_LIST = UriType.UserMessageList.ordinal();
    public static final int ITEM_ID = UriType.UserMessageID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;


    // Sort by condition for querying results.
    public static final String SORT_BY = MessageInfo.SORT_BY;

    public static final String[] PROJECTION = new String[] {
            MessageInfo.TABLE + ".rowid AS _id", C_CHANGE_ID, C_MESSAGE_ID, C_AUTHOR, C_TIMESTAMP,
            C_MESSAGE, C_REVISION_NUMBER, C_USER_ID, C_EMAIL, C_NAME };

    private static UserMessage mInstance = null;
    private MyObserver mObserver;

    public static UserMessage getInstance() {
        if (mInstance == null) mInstance = new UserMessage();
        return mInstance;
    }

    @Override
    public void create(String TAG, SQLiteDatabase db) {
        // This is not a real table (do nothing)
    }

    @SuppressWarnings("unused")
    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    /**
     * Get the commit comments for a change
     * @param context Context for database access
     * @param changeid The Change-Id of the change to get the comments for
     * @return A CursorLoader
     */
    public static CursorLoader getMessagesForChange(Context context, String changeid) {
        return new CursorLoader(context, CONTENT_URI, PROJECTION,
                C_CHANGE_ID + " = ? AND " + MessageInfo.TABLE + "." + C_AUTHOR
                        + " = " + Users.TABLE + "." + Users.C_ACCOUNT_ID,
                new String[] { changeid }, SORT_BY);
    }

    @Override
    protected void registerContentObserver(Context context) {
        mObserver = new MyObserver(new Handler(), context, CONTENT_URI);
        context.getContentResolver().registerContentObserver(Users.CONTENT_URI, true,
                mObserver);
        context.getContentResolver().registerContentObserver(MessageInfo.CONTENT_URI, true,
                mObserver);
    }

    @Override
    protected void unRegisterContentObserver(Context context) {
        context.getContentResolver().unregisterContentObserver(mObserver);
    }
}
