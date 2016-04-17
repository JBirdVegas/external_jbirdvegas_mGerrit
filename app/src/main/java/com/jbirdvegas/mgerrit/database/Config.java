package com.jbirdvegas.mgerrit.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.AnalyticsHelper;
import com.jbirdvegas.mgerrit.objects.ServerVersion;

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
public class Config extends DatabaseTable {

    // Table name
    public static final String TABLE = "Config";

    // --- Columns ---
    private static final String C_KEY = "key";
    private static final String C_VALUE = "value";

    public static final String[] PRIMARY_KEY = { C_KEY };

    public static final int ITEM_LIST = UriType.ConfigList.ordinal();
    public static final int ITEM_ID = UriType.ConfigID.ordinal();

    public static final Uri CONTENT_URI = Uri.parse(DatabaseFactory.BASE_URI + TABLE);

    public static final String CONTENT_TYPE = DatabaseFactory.BASE_MIME_LIST + TABLE;
    public static final String CONTENT_ITEM_TYPE = DatabaseFactory.BASE_MIME_ITEM + TABLE;

    private static Config mInstance = null;


    public static final String KEY_VERSION = "server_version";
    /* Gerrit version 2.8 introduced the 'get version' into the REST API, so we can
     *  use any value below this as a sentinel */
    public static final String VERSION_DEFAULT = "2.0";

    public static Config getInstance() {
        if (mInstance == null) mInstance = new Config();
        return mInstance;
    }

    @Override
    public void create(String tag, SQLiteDatabase db) {
        // Specify a conflict algorithm here so we don't have to worry about it later
        db.execSQL("create table " + TABLE + " ("
                + C_KEY + " text PRIMARY KEY ON CONFLICT REPLACE, "
                + C_VALUE + " text NOT NULL)");
    }

    public static void addURIMatches(UriMatcher _urim) {
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE, ITEM_LIST);
        _urim.addURI(DatabaseFactory.AUTHORITY, TABLE + "/#", ITEM_ID);
    }

    public static ServerVersion getServerVersion(Context context) {
        String version = getValue(context, KEY_VERSION);
        if (version == null || version.isEmpty()) return null;
        ServerVersion serverVersion = new ServerVersion(version);
        AnalyticsHelper.getInstance().setCustomString(context.getString(R.string.cr_server_version), serverVersion.toString());
        return serverVersion;
    }

    public static String getValue(Context context, String key) {
        String value = null;
        Cursor c = context.getContentResolver().query(CONTENT_URI,
                new String[] { C_VALUE }, C_KEY + " = ?", new String[] { key }, null);
        if (c.moveToFirst()) value = c.getString(0);
        c.close();
        return value;
    }

    public static void setValue(Context context, String key, String value) {
        ContentValues contentValues = new ContentValues(2);
        contentValues.put(C_KEY, key);
        contentValues.put(C_VALUE, value);
        context.getContentResolver().insert(CONTENT_URI, contentValues);
    }

    /**
     * @param context Context for accessing the database
     * @return Whether requesting diff info for a change is supported on the server.
     *  This was introduced in Gerrit v2.8.
     */
    public static boolean isDiffSupported(Context context) {
        ServerVersion serverVersion = getServerVersion(context);
        if (serverVersion == null) return false;
        return serverVersion.isFeatureSupported(ServerVersion.VERSION_DIFF);
    }
}
