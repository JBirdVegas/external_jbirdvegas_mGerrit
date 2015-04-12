package com.jbirdvegas.mgerrit.tasks;

/*
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2015
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

import com.android.volley.AuthFailureError;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.Changes;
import com.jbirdvegas.mgerrit.database.Config;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.NotSupported;
import com.jbirdvegas.mgerrit.objects.EventQueue;
import com.jbirdvegas.mgerrit.objects.GerritMessage;
import com.jbirdvegas.mgerrit.objects.ServerVersion;
import com.jbirdvegas.mgerrit.requestbuilders.AccountEndpoints;

public class StarProcessor extends SyncProcessor<String> {

    private final Intent mIntent;
    private final AccountEndpoints mUrl;
    private boolean mIsStarring;
    private Authenticateable<String> request;

    StarProcessor(Context context, Intent intent, AccountEndpoints url) {
        super(context, intent, url);
        mIntent = intent;
        mUrl = url;
        mIsStarring = mIntent.getBooleanExtra(GerritService.IS_STARRING, true);
    }

    @Override
    int insert(String responseCode) {
        String changeId = mIntent.getStringExtra(GerritService.CHANGE_ID);
        int changeNumber = mIntent.getIntExtra(GerritService.CHANGE_NUMBER, 0);
        Changes.starChange(mContext, changeId, changeNumber, mIsStarring);
        return 1;
    }

    @Override
    boolean isSyncRequired(Context context) {
        ServerVersion version = Config.getServerVersion(context);
        if (version.isFeatureSupported(ServerVersion.VERSION_STAR)) {
            return true;
        } else {
            String gerrit = Prefs.getCurrentGerritName(context);
            String msg = String.format(context.getString(R.string.star_change_not_supported), gerrit, ServerVersion.VERSION_STAR);
            GerritMessage ev = new NotSupported(mIntent, mUrl.toString(), msg);
            EventQueue.getInstance().enqueue(ev, false);
            return false;
        }

    }

    @Override
    Class<String> getType() {
        return String.class;
    }

    @Override
    int count(String responseCode) {
        return 1;
    }

    @Override
    protected void fetchData(RequestQueue queue) {
        final String url = mUrl.toString();

        request = new TextRequest(
                mIsStarring ? Method.PUT : Method.DELETE, url,
                getListener(url), new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof AuthFailureError) {
                    Tools.launchSignin(mContext);
                }
                // We still want to post the exception
                // Make sure the sign in activity (if started above) will receive the ErrorDuringConnection message by making it sticky
                GerritMessage ev = new ErrorDuringConnection(mIntent, url, null, error);
                EventQueue.getInstance().enqueue(ev, true);
            }
        });

        this.fetchData(mUrl, request, queue);
    }
}
