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
import com.jbirdvegas.mgerrit.database.CommitMarker;
import com.jbirdvegas.mgerrit.database.DatabaseTable;
import com.jbirdvegas.mgerrit.database.SyncTime;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.objects.GerritURL;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.Arrays;

class CommitProcessor extends SyncProcessor<JSONCommit[]> {

    CommitProcessor(Context context, GerritURL url) {
        super(context, url);
        setResumableUrl();
    }

    @Override
    void insert(JSONCommit[] projects) {
        UserChanges.insertCommits(getContext(), Arrays.asList(projects));
    }

    @Override
    boolean isSyncRequired() {
        Context context = getContext();
        long syncInterval = context.getResources().getInteger(R.integer.changes_sync_interval);
        long lastSync = SyncTime.getValueForQuery(context, SyncTime.PROJECTS_LIST_SYNC_TIME, getQuery());
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
                System.currentTimeMillis(), getQuery());

        // Save our spot using the sortkey of the most recent change
        String changeID = Changes.getMostRecentChange(mContext, getUrl().getStatus());
        if (changeID != null) {
            JSONCommit commit = findCommit(data, changeID);
            if (commit != null) {
                CommitMarker.markCommit(mContext, commit);
            }
        }
    }

    /**
     * Check if we have a sortkey already stored for the current query, if so
     *  we can modify the url given to include that sortkey.
     */
    protected void setResumableUrl() {
        GerritURL originalURL = getUrl();
        String sortKey = CommitMarker.getSortKeyForQuery(mContext, getUrl().getStatus());
        if (sortKey != null) {
            originalURL.setSortKey(sortKey);
            super.setUrl(originalURL);
        }
    }

    private JSONCommit findCommit(JSONCommit[] commits, String changeID) {
        for (JSONCommit commit : commits) {
            if (changeID.equals(commit.getChangeId()))
                return commit;
        }
        return null;
    }
}
