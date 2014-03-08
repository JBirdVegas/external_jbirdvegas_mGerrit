package com.jbirdvegas.mgerrit.message;

import android.content.Context;
import android.content.Intent;

import com.jbirdvegas.mgerrit.objects.GerritMessage;
import com.jbirdvegas.mgerrit.tasks.GerritService;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
public class Finished extends GerritMessage {

    /* Note: Must have the type declared static and public so receivers can subscribe
     * to this type of message */
    public static final String TYPE = "Finished";

    // The number of items that were fetched
    public static final String ITEMS_FETCHED_KEY = "num_items";

    // The original intent that has been processed
    public static final String INTENT_KEY = "intent";

    private final int mItems;
    private final Intent mIntent;
    String mMessage;


    public Finished(Context context, String message, @NotNull Intent intent, int items) {
        super(context, intent.getStringExtra(GerritService.URL_KEY));
        this.mMessage = message;
        this.mItems = items;
        this.mIntent = intent;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getMessage() {
        return mMessage;
    }

    @Override
    protected Intent packMessage(Map<String, String> map) {
        Intent intent = super.packMessage(map);
        intent.putExtra(ITEMS_FETCHED_KEY, mItems);
        intent.putExtra(INTENT_KEY, mIntent);
        return intent;
    }
}
