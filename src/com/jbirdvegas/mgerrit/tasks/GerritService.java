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

import android.app.IntentService;
import android.content.Intent;

public class GerritService extends IntentService {

    public static final String TAG = "GerritService";

    public static final String URL_KEY = "Url";
    public static final String DATA_TYPE_KEY = "Type";

    public static enum DataTypes { Project }

    private String mCurrentUrl;

    // This is required for the service to be started
    public GerritService() { super(TAG); }

    @Override
    protected void onHandleIntent(Intent intent) {
        mCurrentUrl = intent.getStringExtra(URL_KEY);
        SyncProcessor processor = null;

        // Determine which SyncProcessor to use here
        int dataType = intent.getIntExtra(DATA_TYPE_KEY, 0);
        if (dataType == DataTypes.Project.ordinal()) {
            processor = new ProjectListProcessor(this, mCurrentUrl);
        }

        // Call the SyncProcessor to fetch the data if necessary
        if (processor != null && processor.isSyncRequired()) {
            processor.fetchData();
        }
    }
}
