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

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.database.Users;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.StartingRequest;
import com.jbirdvegas.mgerrit.objects.EventQueue;
import com.jbirdvegas.mgerrit.objects.GerritMessage;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.objects.UserAccountInfo;
import com.jbirdvegas.mgerrit.requestbuilders.RequestBuilder;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;

import org.jetbrains.annotations.Nullable;

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

    // Helper method to extract the relevant query portion of the URL
    @Nullable
    public String getQuery() {
        String status = mIntent.getStringExtra(GerritService.CHANGE_STATUS);
        if (status == null) return null;
        else {
            return JSONCommit.KEY_STATUS + ":" + status;
        }
    }

    // Helper method to return the change status
    protected String getStatus() {
        return mIntent.getStringExtra(GerritService.CHANGE_STATUS);
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
     *
     * @param gerritApi An instance of the Gerrit Rest API library with the host and
     *                  authentication set up
     * @return The data to be queried for the given request
     * @throws RestApiException If something goes wrong
     */
    abstract T getData(GerritApi gerritApi) throws RestApiException;

    protected void fetchData() {
        GerritApi gerritApi = getGerritApiInstance(true);
        try {
            mEventBus.post(new StartingRequest(mIntent, getStatus()));
            onResponse(getData(gerritApi));
        } catch (RestApiException e) {
            handleRestApiException(e);
        }
    }

    protected GerritApi getGerritApiInstance(boolean isAuthenticating) {
        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        GerritAuthData.Basic authData;
        String username = null, password = null;

        if (isAuthenticating) {
            username = mIntent.getStringExtra(GerritService.HTTP_USERNAME);
            password = mIntent.getStringExtra(GerritService.HTTP_PASSWORD);
            if (username == null || password == null) {
                UserAccountInfo ai = Users.getUser(mContext, null);
                if (ai != null) {
                    username = ai.username;
                    password = ai.password;
                }
            }
        }

        String host = PrefsFragment.getCurrentGerrit(mContext);
        if (username != null && password != null) {
            authData = new GerritAuthData.Basic(host, username, password);
        } else {
            authData = new GerritAuthData.Basic(host);
        }

        return gerritRestApiFactory.create(authData);
    }

    protected void handleRestApiException(RestApiException e) {
        if (((HttpStatusException) e).getStatusCode() == 502) {
            Tools.launchSignin(mContext);
        }
        handleException(e);
    }

    protected void handleException(Exception e) {
        // We still want to post the exception
        // Make sure the sign in activity (if started above) will receive the ErrorDuringConnection message by making it sticky
        GerritMessage ev = new ErrorDuringConnection(mIntent, null, e);
        EventQueue.getInstance().enqueue(ev, true);
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

    protected void onResponse(T data) {
        /* Offload all the work to a separate thread so database activity is not
         * done on the main thread. */
        mResponseHandler = new ResponseHandler(data);
        mResponseHandler.start();
    }

    class ResponseHandler extends Thread {
        private final T mData;

        ResponseHandler(T data) {
            this.mData = data;
        }

        @Override
        public void run() {
            int numItems = 0;
            // Order is important here, as we need to insert the data first
            if (mData != null && count(mData) > 0) {
                numItems = insert(mData);
            }

            EventBus.getDefault().post(new Finished(mIntent, getStatus(), numItems));
            if (mData != null) doPostProcess(mData);

            GerritService.finishedRequest(mCurrentUrl);
            // This thread has finished so the parent activity should no longer need it
            mResponseHandler = null;
        }
    }
}
