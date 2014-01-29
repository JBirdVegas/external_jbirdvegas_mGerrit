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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.StartingRequest;
import com.jbirdvegas.mgerrit.objects.GerritURL;

/**
 * Base class that the GerritService expects as a contract to synchronise data from the
 *  server into the database. Comes with a decent dose of Generics
 * @param <T> The intermediary class where the JSON data is deserialized
 */
abstract class SyncProcessor<T> {
    protected final Context mContext;
    private GerritURL mCurrentUrl;
    private ResponseHandler mResponseHandler;

    private static Gson gson;
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Deserializers.addDeserializers(gsonBuilder);
        gson = gsonBuilder.create();
    }

    /**
     * Alternate constructor where the url is not dynamic and can be determined from the
     *  current Gerrit instance.
     *
     *  Note: subclasses using this constructor MUST override fetchData
     * @param context Contect for network access
     */
    SyncProcessor(Context context) {
        this.mContext = context;
    }

    SyncProcessor(Context context, GerritURL url) {
        this.mContext = context;
        this.mCurrentUrl = url;
    }

    protected Context getContext() { return mContext; }
    protected GerritURL getUrl() { return mCurrentUrl; }

    protected void setUrl(GerritURL url) { mCurrentUrl = url; }

    // Helper method to extract the relevant query portion of the URL
    protected String getQuery() {
        return getUrl().getQuery();
    }

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

    /**
     * Do some additional work after the data has been processed.
     * @param data The data that was just processed passed here for convenience
     */
    void doPostProcess(T data) {
        // Default to doing nothing - subclasses can override this
    }

    /**
     * Sends a request to the Gerrit server for the url set in the constructor
     *  SyncProcessor(Context, GerritURL). The default simply calls
     *  fetchData(String).
     */
    protected void fetchData(RequestQueue queue) {
        fetchData(getUrl().toString(), queue);
    }

    protected void fetchData(final String url, RequestQueue queue) {
        GsonRequest request = new GsonRequest<>(url, gson, getType(), 5,
                getListener(url), new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                new ErrorDuringConnection(mContext, volleyError, url);
            }
        });

        fetchData(url, request, queue);
    }

    protected void fetchData(final String url, Request<T> request, RequestQueue queue) {
        if (queue == null) queue = Volley.newRequestQueue(getContext());

        new StartingRequest(mContext, url).sendUpdateMessage();
        queue.add(request);
    }


    protected boolean isInSyncInterval(long syncInterval, long lastSync) {
        long timeNow = System.currentTimeMillis();
        return ((timeNow - lastSync) < syncInterval);
    }

    public void cancelOperation() {
        if (mResponseHandler == null) return;
        mResponseHandler.interrupt();
        mResponseHandler = null;
    }

    protected Response.Listener<T> getListener(final String url) {
        return new Response.Listener<T>() {
            @Override
            public void onResponse(T data) {
            /* Offload all the work to a seperate thread so database activity is not
             * done on the main thread. */
                mResponseHandler = new ResponseHandler(data, url);
                mResponseHandler.start();
            }
        };
    }

    class ResponseHandler extends Thread {
        private final T mData;
        private final String mUrl;

        ResponseHandler(T data, String url) {
            this.mData = data;
            this.mUrl = url;
        }

        @Override
        public void run() {
            insert(mData);
            new Finished(mContext, null, mUrl).sendUpdateMessage();
            doPostProcess(mData);
            // This thread has finished so the parent activity should no longer need it
            mResponseHandler = null;
        }
    }
}
