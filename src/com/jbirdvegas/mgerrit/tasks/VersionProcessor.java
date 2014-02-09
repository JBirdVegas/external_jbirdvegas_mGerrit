package com.jbirdvegas.mgerrit.tasks;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.database.Config;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final String mUrl;

    VersionProcessor(Context context) {
        super(context);
        String url = Prefs.getCurrentGerrit(context);
        mUrl = url + "config/server/version";
    }

    @Override
    void insert(String data) {
        // Trim off the junk beginning and the quotes around the version number
        Pattern p = Pattern.compile("\"([^\"]+)\"");
        Matcher m = p.matcher(data);
        if (m.find()) {
            Config.setValue(getContext(), Config.KEY_VERSION, m.group(1));
        } else {
            Config.setValue(getContext(), Config.KEY_VERSION, data);
        }
    }

    @Override
    boolean isSyncRequired() {
        // Look up the database to see if we have previously saved the version
        return Config.getValue(getContext(), Config.KEY_VERSION) == null;
    }

    @Override
    Class<String> getType() {
        return String.class;
    }

    @Override
    protected void fetchData(RequestQueue queue) {
        Response.Listener<String> listener = getListener(mUrl);

        StringRequest request = new StringRequest(mUrl,
                listener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (volleyError == null || volleyError.networkResponse == null) {
                    new ErrorDuringConnection(mContext, volleyError, mUrl);
                } else if (volleyError.networkResponse.statusCode == 404) {
                    // Pretend we got a response
                    getListener(mUrl).onResponse(Config.VERSION_DEFAULT);
                } else {
                    new ErrorDuringConnection(mContext, volleyError, mUrl);
                }
            }
        });

        this.fetchData(mUrl, request, queue);
    }
}
