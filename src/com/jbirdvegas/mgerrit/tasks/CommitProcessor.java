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

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.Changes;
import com.jbirdvegas.mgerrit.database.DatabaseTable;
import com.jbirdvegas.mgerrit.database.SyncTime;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.objects.GerritURL;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.Arrays;

class CommitProcessor extends SyncProcessor<JSONCommit[]> {

    CommitProcessor(Context context, String url) {
        super(context, url);
    }

    @Override
    void insert(JSONCommit[] projects) {
        UserChanges.insertCommits(getContext(), Arrays.asList(projects));
    }

    @Override
    boolean isSyncRequired() {
        Context context = getContext();
        long syncInterval = context.getResources().getInteger(R.integer.changes_sync_interval);
        long lastSync = SyncTime.getValueForQuery(context, SyncTime.PROJECTS_LIST_SYNC_TIME, getUrl());
        boolean sync = isInSyncInterval(syncInterval, lastSync);
        if (sync) return true;

        // Better just make sure that there are changes in the database
        return DatabaseTable.isEmpty(context, Changes.CONTENT_URI);
    }

    @Override
    Class<JSONCommit[]> getType() {
        return JSONCommit[].class;
    }

    @Override
    void doPostProcess(JSONCommit[] data) {
        SyncTime.setValue(mContext, SyncTime.CHANGES_LIST_SYNC_TIME,
                System.currentTimeMillis(), getUrl());
    }
}
