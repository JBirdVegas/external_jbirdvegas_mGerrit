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
import android.content.Intent;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jbirdvegas.mgerrit.SigninActivity;
import com.jbirdvegas.mgerrit.database.Users;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.StartingRequest;
import com.jbirdvegas.mgerrit.objects.AccountInfo;
import com.jbirdvegas.mgerrit.objects.RequestBuilder;

import de.greenrobot.event.EventBus;

/**
 * Base class that the GerritService expects as a contract to synchronise data from the
 *  server into the database. Comes with a decent dose of Generics
 * @param <T> The intermediary class where the JSON data is deserialized
 */
abstract class SyncProcessor<T> {
    protected final Context mContext;
    private final EventBus mEventBus;
    private RequestBuilder mCurrentUrl;
    private ResponseHandler mResponseHandler;
    private final Intent mIntent;

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
     *  Note: subclasses using this constructor MUST override fetchData or setUrl
     * @param context Context for network access
     * @param intent The original intent to GerritService that started initiated
     *               this SyncProcessor.
     */
    SyncProcessor(Context context, Intent intent) {
        this(context, intent, null);
    }

    /**
     * Standard constructor to create a SyncProcessor
     * @param context Context for network access
     * @param intent The original intent to GerritService that started initiated
     *               this SyncProcessor.
     * @param url The Gerrit URL from which to retrieve the data from
     */
    SyncProcessor(Context context, Intent intent, RequestBuilder url) {
        this.mContext = context;
        this.mCurrentUrl = url;
        this.mIntent = intent;
        this.mEventBus = EventBus.getDefault();
    }

    protected Context getContext() { return mContext; }
    protected RequestBuilder getUrl() { return mCurrentUrl; }
    protected void setUrl(RequestBuilder url) { mCurrentUrl = url; }

    // Helper method to extract the relevant query portion of the URL
    protected String getQuery() {
        return getUrl().getQuery();
    }

    // Helper method to return the change status
    private String getStatus() {
        if (mCurrentUrl == null) return null;
        else return mCurrentUrl.getStatus();
    }

    public Intent getIntent() { return mIntent; }

    /**
     * Inserts data into the database
     * @param data A collection of the deserialized data ready for insertion
     */
    abstract int insert(T data);

    /**
     * @return Whether it is necessary to contact the server
     */
    abstract boolean isSyncRequired(Context context);

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
     * @param data A collection of the deserialized data
     * @return The number of items contained in data
     */
    abstract int count(T data);

    /**
     * Sends a request to the Gerrit server for the url set in the constructor
     *  SyncProcessor(Context, RequestBuilder). The default simply calls
     *  fetchData(String, RequestQueue).
     */
    protected void fetchData(RequestQueue queue) {
        fetchData(mCurrentUrl, queue);
    }

    /**
     * Send a request to the Gerrit server. Expects the response to be in
     *  Json format.
     * @param url The URL of the request
     * @param queue An instance of the Volley request queue.
     */
    protected void fetchData(final RequestBuilder requestBuilder, RequestQueue queue) {
        final String url = requestBuilder.toString();

        GsonRequest request = new GsonRequest<>(url, gson, getType(), 5,
                getListener(url), new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Tools.launchSignin(mContext);
                // We still want to post the exception
                // Make sure the sign in activity (if started above) will receive the ErrorDuringConnection message by making it sticky
                mEventBus.postSticky(new ErrorDuringConnection(mIntent, url, getStatus(), volleyError));
            }
        });

        setUsernamePasswordOnRequest(requestBuilder, request);

        fetchData(url, request, queue);
    }

    protected void fetchData(final String url, Authenticateable<T> request, RequestQueue queue) {
        if (queue == null) queue = Volley.newRequestQueue(getContext());

        // setUsernamePasswordOnRequest(requestBuilder, request); TODO: We need to set the username/password on the request if we have one!

        mEventBus.post(new StartingRequest(mIntent, url, getStatus()));
        queue.add(request);
    }


    protected boolean isInSyncInterval(long syncInterval, long lastSync) {
        if (lastSync == 0) return false; // Always sync if this is the first time
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
                getSimpleListener(data, url);
            }
        };
    }

    protected void getSimpleListener(T data, final String url) {
        /* Offload all the work to a separate thread so database activity is not
         * done on the main thread. */
        mResponseHandler = new ResponseHandler(data, url);
        mResponseHandler.start();
    }

    private boolean setUsernamePasswordOnRequest(RequestBuilder requestBuilder, GsonRequest request) {
        if (requestBuilder.isAuthenticating()) {
            String username = mIntent.getStringExtra(GerritService.HTTP_USERNAME);
            String password = mIntent.getStringExtra(GerritService.HTTP_PASSWORD);
            if (username == null || password == null) {
                AccountInfo ai = Users.getUser(mContext, null);
                if (ai != null) {
                    username = ai.username;
                    password = ai.password;
                }
            }
            if (username != null && password != null) {
                request.setHttpBasicAuth(username, password);
            } else {
                return false;
            }
        }
        return true;
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
            int numItems = 0;
            // Order is important here, as we need to insert the data first
            if (mData != null && count(mData) > 0) {
                numItems = insert(mData);
            }

            EventBus.getDefault().post(new Finished(mIntent, mUrl, getStatus(), numItems));
            if (mData != null) doPostProcess(mData);

            GerritService.finishedRequest(mCurrentUrl);
            // This thread has finished so the parent activity should no longer need it
            mResponseHandler = null;
        }
    }
}
