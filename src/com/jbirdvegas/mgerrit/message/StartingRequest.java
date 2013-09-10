package com.jbirdvegas.mgerrit.message;

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

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.GerritMessage;

import java.util.Iterator;
import java.util.Map;

public class StartingRequest extends GerritMessage {

    /* Note: Must have the type declared static and public so receivers can subscribe
     * to this type of message */
    public static final String TYPE = "Establishing Connection";

    public StartingRequest(Context context, String url) {
        super(context, url);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getMessage() {
        return getContext().getString(R.string.connection_starting);
    }

    @Override
    protected Intent packMessage(Map<String, String> map) {
        Intent intent = new Intent(getType());

        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        return intent;
    }
}
