package com.jbirdvegas.mgerrit.tasks;

import android.content.Context;
import android.content.Intent;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.SigninActivity;
import com.jbirdvegas.mgerrit.database.Config;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.objects.ConfigEndpoints;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;

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
public class VersionProcessor extends SyncProcessor<String> {

    private final ConfigEndpoints mUrl;
    private final EventBus mEventBus;
    private final Context mContext;
    private final Intent mIntent;

    VersionProcessor(Context context, Intent intent) {
        super(context, intent);
        mUrl = ConfigEndpoints.server_verion();
        mEventBus = EventBus.getDefault();
        mContext = context;
        mIntent = intent;
    }

    @Override
    int insert(String data) {
        // Trim off the junk beginning and the quotes around the version number
        Pattern p = Pattern.compile("\"([^\"]+)\"");
        Matcher m = p.matcher(data);
        if (m.find()) {
            Config.setValue(getContext(), Config.KEY_VERSION, m.group(1));
        } else {
            Config.setValue(getContext(), Config.KEY_VERSION, data);
        }
        return 1;
    }

    @Override
    boolean isSyncRequired(Context context) {
        // Look up the database to see if we have previously saved the version
        return Config.getValue(context, Config.KEY_VERSION) == null;
    }

    @Override
    Class<String> getType() {
        return String.class;
    }

    @Override
    int count(String version) {
        if (version != null) return 1;
        else return 0;
    }

    @Override
    protected void fetchData(RequestQueue queue) {
        final String url = mUrl.toString();
        Response.Listener<String> listener = getListener(url);

        Authenticateable<String> request = new TextRequest(url,
                listener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (volleyError == null || volleyError.networkResponse == null) {
                    mEventBus.postSticky(new ErrorDuringConnection(mIntent, url, null, volleyError));
                // Authentication handling here and in StringRequest in case the Gerrit url requires authentication
                } else if (volleyError.networkResponse.statusCode == 401) {
                    Tools.launchSignin(mContext);

                    // We still want to post the exception
                    // Make sure the sign in activity (if started above) will receive the ErrorDuringConnection message by making it sticky
                    mEventBus.postSticky(new ErrorDuringConnection(mIntent, url, null, volleyError));
                } else if (volleyError.networkResponse.statusCode == 404) {
                    // Pretend we got a response
                    getListener(url).onResponse(Config.VERSION_DEFAULT);
                } else {
                    mEventBus.postSticky(new ErrorDuringConnection(getIntent(), url, null, volleyError));
                }
            }
        });

        this.fetchData(mUrl, request, queue);
    }
}
