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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.helpers.DBParams;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** This class aims to manage and abstract which database is being accessed.
 *  This is a singleton class, we can have only one DBHelper (it is also a
 *   singleton) and we only need one instance of a wdb as we should only
 *   have one database file open at a time. */
public class DatabaseFactory extends ContentProvider {
    private static DBHelper dbHelper;
    private static SQLiteDatabase wdb;

    // The Authority of the content provider
    public static final String AUTHORITY = "com.jbirdvegas.provider.mgerrit";

    // All URIs inherit from this URI
    static final String BASE_URI = "content://" + AUTHORITY + "/";

    // MIME type of a cursor containing a list of rows
    static final String BASE_MIME_LIST = "vnd.android.cursor.dir/vnd" + AUTHORITY + ".";
    // MIME type of a cursor containing a single row
    static final String BASE_MIME_ITEM = "vnd.android.cursor.item/vnd" + AUTHORITY + ".";

    // Utility class to aid in matching URIs in content providers.
    private static final UriMatcher URI_MATCHER;

    /**
     * Store a list of the current content provider instances. These should be WeakReferences
     *   so as to not impact garbage collection. When the current Gerrit changes, we will want
     *   to notify ALL of these to refresh as we just changed the underlying database.
     *  We could make this class a singleton but being able to read and write from different content
     *   provider objects at once could have its advantages.
     */
    private static List<WeakReference<DatabaseFactory>> mInstances;

    // prepare the UriMatcher
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

        for (Class<? extends DatabaseTable> table : DatabaseTable.tables) {
            try {
                Method method = table.getDeclaredMethod("addURIMatches", UriMatcher.class);
                method.invoke(null, URI_MATCHER);
            } catch (Exception e) {
                throw new RuntimeException("Unable to add URI matches for class " + table.getSimpleName(), e);
            }
        }
    }

    private boolean mLocked;

    public DatabaseFactory() {
        super();
        if (mInstances == null) mInstances = new ArrayList<>();
        mInstances.add(new WeakReference<>(this));
    }

    // Not static as this ensures getDatabase was called
    public SQLiteOpenHelper getDatabaseHelper() {
        return dbHelper;
    }

    /**
     * Close the current database. We can always check if the database is open by
     *  looking for one of the results of this method (e.g. dbHelper == null)
     */
    public void closeDatabase() {
        if (mLocked) {
            dbHelper.shutdown();
            wdb = null;
            dbHelper = null;
        }
    }

    /** Locking methods **/
    private void lock() { mLocked = true; }
    private synchronized void unlock() {
        mLocked = false;
        this.notify();
    }

    /**
     * Checks if the database is currently in use (i.e. a CRUD operation is being undertaken)
     *  If this is the case, we want it to proceed before trying to change the database out
     *  from under it. Otherwise, we are free to switch the database.
     */
    void waitUntilUnlocked() {
        new Thread() {
            @Override
            public void run() {
                while (mLocked) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        // Interupted. Stop waiting and try one final time to close the database
                        break;
                    }
                }
                closeDatabase();
            }
        }.start();
    }

    /** This should be called when the Gerrit source changes to modify all database references to
     *  use the new database source.
     */
    public static void changeGerrit(@NotNull Context context, String newGerrit) {
        Log.d("DatabaseFactory", "Switching Gerrit instance to: " + newGerrit);

        /* Currently all the members of this class are static so it is only relevant
         *  for the first instance */
        for (WeakReference<DatabaseFactory> key : mInstances) {
            DatabaseFactory instance = key.get();
            if (instance == null) {
                mInstances.remove(key);
                continue;
            }

            // Close the database file
            instance.waitUntilUnlocked();
        }

        // Reopen the new database for all the instances
        DatabaseFactory.getDatabase(context, newGerrit);
    }

    public static void getDatabase(@NotNull Context context, @NotNull String gerrit) {
        String dbName = DBHelper.getDatabaseName(gerrit);
        DatabaseFactory.dbHelper = new DBHelper(context, dbName);
        // Ensure the database is open and we have a reference to it before
        //  trying to perform any queries using it.
        DatabaseFactory.wdb = dbHelper.getWritableDatabase();

        // Notify ALL content providers that their data has changed. This should force a refresh
        //  of every loader's data
        context.getContentResolver().notifyChange(Uri.parse(DatabaseFactory.BASE_URI), null);
    }

    /** This the actual constructor **/
    @Override
    public boolean onCreate() {

        Context context = getContext();
        String gerrit = Prefs.getCurrentGerrit(context);
        getDatabase(context, gerrit);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (!isUriList(uri))
            selection = handleID(uri, selection);

        String table = getUriTable(uri);

        Integer limit = DBParams.getLimitParameter(uri);
        String sLimit = (limit == null ? null : limit.toString());
        String groupby = DBParams.getGroupByCondition(uri);

        lock();
        Cursor c = wdb.query(table, projection, selection, selectionArgs,
                groupby, null, sortOrder, sLimit);
        unlock();

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override @Contract("null -> fail")
    public String getType(Uri uri) {
        int result = URI_MATCHER.match(uri);

        String retval = DatabaseTable.sContentTypeMap.get(result);
        if (retval != null) return retval;
        else throw new IllegalArgumentException("Unsupported URI: " + uri);
    }

    @Override  @Contract("null -> fail")
    public Uri insert(Uri uri, ContentValues values) {
        long id;

        if (!isUriList(uri))
            throw new IllegalArgumentException("Unsupported URI for insertion: " + uri);

        String table = getUriTable(uri);
        Integer conflictAlgorithm = DBParams.getConflictParameter(uri);

        lock();
        if (conflictAlgorithm == null) id = wdb.insert(table, null, values);
        else {
            id = wdb.insertWithOnConflict(table, null, values, conflictAlgorithm);
        }
        unlock();

        if (id > 0) {
            // notify all listeners of changes and return itemUri:
            Uri itemUri = ContentUris.withAppendedId(uri, id);
            getContext().getContentResolver().notifyChange(itemUri, null);
            return itemUri;
        }

        return null;
    }

    @Override  @Contract("null -> fail")
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (!isUriList(uri))
            selection = handleID(uri, selection);

        String table = getUriTable(uri);
        lock();
        int rows = wdb.delete(table, selection, selectionArgs);
        unlock();

        if (rows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rows;
    }

    @Override  @Contract("null -> fail")
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int updateCount = 0, result = URI_MATCHER.match(uri);

        if (!isUriList(uri)) selection = handleID(uri, selection);

        String tableName = getUriTable(uri);
        lock();
        updateCount = wdb.update(tableName, values, selection, selectionArgs);
        unlock();

        if (updateCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return updateCount;
    }

    @Override  @Contract("null -> fail")
    public int bulkInsert(Uri uri, @NotNull ContentValues[] values) {
        String table = getUriTable(uri);

        Integer conflictAlgorithm = DBParams.getConflictParameter(uri);
        boolean update = DBParams.updateOnDuplicateInsertion(uri);
        int numInserted = 0;

        lock();
        wdb.beginTransaction();
        try {
            for (ContentValues cv : values) {
                numInserted = (insert(table, cv, conflictAlgorithm, update)) ?
                        numInserted + 1 : numInserted;
            }
            wdb.setTransactionSuccessful();
        } finally {
            wdb.endTransaction();
        }
        unlock();

        getContext().getContentResolver().notifyChange(uri, null);
        return numInserted;
    }

    /**
     * Insert method where the table has already been defined.
     * @param table the table to insert the row into
     * @param values A set of column_name/value pairs to add to the database. This must not be null.
     * @param conflictAlgorithm for insert conflict resolver
     * @param updateOnDuplicate
     * @return The URI for the newly inserted item.
     */
    public boolean insert(String table, ContentValues values,
                          Integer conflictAlgorithm, boolean updateOnDuplicate) {
        long id = -1;
        if (table == null) return false;

        lock();
        if (conflictAlgorithm == null) {
            id = wdb.insert(table, null, values);
        }
        else {
            id = wdb.insertWithOnConflict(table, null, values, conflictAlgorithm);
        }
        unlock();

        return id > 0;
    }

    /**
     * Get the internal database table name for a given URI
     * @param uri Resource identifier of a database table
     * @return The internal name of the table
     * @throws IllegalArgumentException When the uri does not match a valid table
     */
    @Contract("null -> fail")
    private static String getUriTable(Uri uri) throws IllegalArgumentException {
        int result = URI_MATCHER.match(uri);

        String tableName = DatabaseTable.sTableMap.get(result);

        if (tableName.equals(UserChanges.TABLE)) return Users.TABLE + ", " + Changes.TABLE;
        else if (tableName.equals(UserMessage.TABLE)) return Users.TABLE + ", " + MessageInfo.TABLE;
        else if (tableName.equals(FileChanges.TABLE)) return FileInfoTable.TABLE + ", " + Changes.TABLE;
        else if (tableName.equals(UserReviewers.TABLE)) return Users.TABLE + ", " + Reviewers.TABLE;

        else if (tableName != null) return tableName;
        else {
            throw new IllegalArgumentException("Could not resolve URI data location: " + uri);
        }
    }

    private boolean isUriList(Uri uri) {
        String type = getType(uri);
        return isUriList(type);
    }

    private boolean isUriList(String type) {
        return type != null && type.contains("vnd.android.cursor.dir/");
    }

    private String handleID(Uri _uri, String _selection) {
        if (!TextUtils.isEmpty(_selection)) {
            return _selection + " AND ROWID = " + _uri.getLastPathSegment();
        }
        else return "ROWID = " + _uri.getLastPathSegment();
    }
}
