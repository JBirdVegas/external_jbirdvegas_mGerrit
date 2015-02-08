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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.jbirdvegas.mgerrit.objects.GerritURL;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class GerritService extends IntentService {

    public static final String TAG = "GerritService";

    public static final String URL_KEY = "Url";
    public static final String DATA_TYPE_KEY = "Type";

    /* These are for the change list - whether we are fetching newer or older changes than what
      * we have already */
    public enum Direction { Newer, Older }
    public static final String CHANGES_LIST_DIRECTION = "direction";

    public static enum DataType { Project, Commit, CommitDetails, GetVersion, LegacyCommitDetails }

    private static RequestQueue mRequestQueue;

    private GerritURL mCurrentUrl;

    // A list of the currently running sync processors
    private static HashMap<GerritURL, SyncProcessor> sRunningTasks;

    // This is required for the service to be started
    public GerritService() {
        super(TAG);
        sRunningTasks = new HashMap<>();
    }

    @Override
    protected void onHandleIntent(@NotNull Intent intent) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(this);
        }

        mCurrentUrl = intent.getParcelableExtra(URL_KEY);
        SyncProcessor processor;

        if (sRunningTasks.containsKey(mCurrentUrl)) return;

        // Determine which SyncProcessor to use here
        DataType dataType = (DataType) intent.getSerializableExtra(DATA_TYPE_KEY);
        if (dataType == DataType.Project) {
            processor = new ProjectListProcessor(this, intent);
        } else if (dataType == DataType.Commit) {
            processor = new ChangeListProcessor(this, intent, mCurrentUrl);
        } else if (dataType == DataType.CommitDetails) {
            processor = new CommitProcessor(this, intent, mCurrentUrl);
        } else if (dataType == DataType.LegacyCommitDetails) {
            processor = new LegacyCommitProcessor(this, intent, mCurrentUrl);
        } else if (dataType == DataType.GetVersion) {
            processor = new VersionProcessor(this, intent);
        } else {
            Log.w(TAG, "Don't know how to handle synchronization of type " + DATA_TYPE_KEY);
            return;
        }

        // Call the SyncProcessor to fetch the data if necessary
        boolean needsSync = processor.isSyncRequired(this);
        if (needsSync) {
            sRunningTasks.put(mCurrentUrl, processor);
            processor.fetchData(mRequestQueue);
        }
    }

    /**
     * Start the updater to check for an update if necessary
     */
    public static void sendRequest(Context context, DataType dataType, Bundle bundle) {
        Intent it = new Intent(context, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, dataType);
        it.putExtras(bundle);
        context.startService(it);
    }

    public static void sendRequest(Context context, DataType dataType, GerritURL url) {
        Bundle b = new Bundle();
        b.putParcelable(GerritService.URL_KEY, url);
        GerritService.sendRequest(context, dataType, b);
    }

    private static boolean isProcessorRunning(SyncProcessor processor) {
        Class<? extends SyncProcessor> clazz = processor.getClass();
        for (SyncProcessor next : sRunningTasks.values()) {
            if (next.getClass().equals(clazz)) return true;
        }
        return false;
    }

    protected static HashMap<GerritURL, SyncProcessor> getRunningProcessors() {
        return sRunningTasks;
    }

    protected static void finishedRequest(GerritURL url) {
        sRunningTasks.remove(url);
    }
}
