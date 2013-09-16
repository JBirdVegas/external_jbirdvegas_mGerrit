package com.jbirdvegas.mgerrit.objects;

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
import android.support.v4.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class GerritMessage {

    public static final String MESSAGE = "Gerrit Task Update";
    public static final String EXCEPTION = "Exception";
    public static final String URL = "URL";
    public static final String PROGRESS = "Progress";
    public static final String FILE_LENGTH = "File Length";

    private final Context mContext;
    public String mUrl;

    public GerritMessage(Context context, String url) {
        this.mContext = context;
        this.mUrl = url;
    }

    public abstract String getType();
    public abstract String getMessage();

    public String getUrl() { return mUrl; }
    public Context getContext() { return mContext; }

    public void sendUpdateMessage()
    {
        Map<String, String> m = new HashMap<String, String>();
        m.put(GerritMessage.MESSAGE, this.getMessage());
        Intent i = packMessage(m);
        sendMessage(i);
    }

    private void sendMessage(Intent intent) {
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    protected Intent packMessage(Map<String, String> map) {
        Intent intent = new Intent(getType());

        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            intent.putExtra(entry.getKey(), entry.getValue());
            intent.putExtra(URL, mUrl);
        }
        return intent;
    }
}
