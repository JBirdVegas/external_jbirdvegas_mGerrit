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
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

class MyObserver extends ContentObserver {

    private final Uri mContentUri;
    private final Context mContext;

    /**
     * Construct a content observer that intercepts Uri notifications and
     *  re-broadcasts them for a different Uri
     * @param handler The handler to run onChange(boolean) on, or null if none.
     * @param context The context from which to receive the content resolver
     * @param uri The uri to rebroadcast Uri notifications for
     */
    public MyObserver(Handler handler, Context context, Uri uri) {
        super(handler);
        mContext = context;
        mContentUri = uri;
    }

    @Override
    public void onChange(boolean selfChange) {
        this.onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        mContext.getContentResolver().notifyChange(mContentUri, this);
    }
}
