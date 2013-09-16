package com.jbirdvegas.mgerrit.tasks;

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

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jbirdvegas.mgerrit.database.SyncTime;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.StartingRequest;

/**
 * Base class that the GerritService expects as a contract to synchronise data from the
 *  server into the database. Comes with a decent dose of Generics
 * @param <T> The intermediary class where the JSON data is deserialized
 */
abstract class SyncProcessor<T> {
    protected final Context mContext;
    private final String mCurrentUrl;
    private final Response.Listener<T> listener = new Response.Listener<T>() {
        @Override
        public void onResponse(T s) {
            insert(s);
            new Finished(mContext, null, mCurrentUrl);
            SyncTime.setValue(mContext, SyncTime.PROJECTS_LIST_SYNC_TIME,
                    System.currentTimeMillis());
        }
    };

    private static Gson gson;
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Deserializers.addDeserializers(gsonBuilder);
        gson = gsonBuilder.create();
    }

    SyncProcessor(Context context, String url) {
        this.mContext = context;
        this.mCurrentUrl = url;
    }

    protected Context getContext() { return mContext; }

    /**
     * Inserts data into the database
     * @param data A collection of the deserialized data ready for insertion
     */
    abstract void insert(T data);

    /**
     * @return Whether it is necessary to contact the server
     */
    abstract boolean isSyncRequired();

    /**
     * @return T.class (the class of T). This is used for Volley Gson requests
     */
    abstract Class<T> getType();

    protected void fetchData() {

        // Won't be able to actually get JSON response back as it
        //  is improperly formed (junk at start), but requesting raw text and
        //  trimming it should be fine.

        RequestQueue queue = Volley.newRequestQueue(mContext);
        new StartingRequest(mContext, mCurrentUrl).sendUpdateMessage();

        GsonRequest request = new GsonRequest<T>(mCurrentUrl, gson, getType(), 5,
               listener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                new ErrorDuringConnection(mContext, volleyError, mCurrentUrl);
            }
        });
        queue.add(request);
    }
}
