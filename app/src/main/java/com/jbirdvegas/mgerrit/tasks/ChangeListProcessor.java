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
import android.content.Intent;
import android.util.Pair;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.Changes;
import com.jbirdvegas.mgerrit.database.CommitMarker;
import com.jbirdvegas.mgerrit.database.Config;
import com.jbirdvegas.mgerrit.database.DatabaseTable;
import com.jbirdvegas.mgerrit.database.MoreChanges;
import com.jbirdvegas.mgerrit.database.SyncTime;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.requestbuilders.ChangeEndpoints;
import com.jbirdvegas.mgerrit.requestbuilders.RequestBuilder;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.objects.ServerVersion;
import com.jbirdvegas.mgerrit.tasks.GerritService.Direction;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class ChangeListProcessor extends SyncProcessor<JSONCommit[]> {

    GerritService.Direction mDirection;

    ChangeListProcessor(Context context, Intent intent, RequestBuilder url) {
        super(context, intent, url);

        Direction direction = (Direction) getIntent().getSerializableExtra(GerritService.CHANGES_LIST_DIRECTION);
        if (direction != null) mDirection = direction;
        else mDirection = Direction.Newer;

        // If we are loading newer changes using an old Gerrit instance, set the sortkey
        if (mDirection == Direction.Newer) {
            ServerVersion version = Config.getServerVersion(context);
            if (version == null || !version.isFeatureSupported("2.8.1")) setResumableUrl();
        }
        attemptAuthenticatedRequest(url);
    }

    @Override
    int insert(JSONCommit[] commits) {
        if (commits.length > 0) {
            return UserChanges.insertCommits(getContext(), Arrays.asList(commits));
        }
        return 0;
    }

    @Override
    boolean isSyncRequired(Context context) {
        // Are we already fetching changes for this status?
        if (areFetchingChangesForStatus(getQuery())) return false;
        else if (getQuery() == null) return true; // If we have not specified a status we are doing a query on all past changes
        else if (mDirection == Direction.Older) return true;

        long syncInterval = context.getResources().getInteger(R.integer.changes_sync_interval);
        long lastSync = SyncTime.getValueForQuery(context, SyncTime.CHANGES_LIST_SYNC_TIME, getQuery());
        boolean sync = isInSyncInterval(syncInterval, lastSync);
        if (!sync) return true;

        // Better just make sure that there are changes in the database
        return DatabaseTable.isEmpty(context, Changes.CONTENT_URI);
    }

    @Override
    Class<JSONCommit[]> getType() {
        return JSONCommit[].class;
    }

    @Override
    void doPostProcess(JSONCommit[] data) {
        ChangeEndpoints originalURL = (ChangeEndpoints) getUrl();
        String status = originalURL.getStatus();
        boolean moreChanges = false;

        if (mDirection == Direction.Older) {
            if (data.length > 0) {
                moreChanges = data[data.length - 1].areMoreChanges();
            }
        } else {
            if (data.length > 0) {
                moreChanges = data[0].areMoreChanges();
            }

            SyncTime.setValue(mContext, SyncTime.CHANGES_LIST_SYNC_TIME,
                    System.currentTimeMillis(), getQuery());

            // Save our spot using the sortkey of the most recent change
            Pair<String, Integer> change = Changes.getMostRecentChange(mContext, status);
            if (change != null) {
                String changeID = change.first;
                if (changeID != null && !changeID.isEmpty()) {
                    JSONCommit commit = findCommit(data, changeID);
                    if (commit != null) {
                        CommitMarker.markCommit(mContext, commit);
                    }
                }
            }
        }
        MoreChanges.insert(mContext, status, mDirection, moreChanges);
    }

    /**
     * Check if we have a sortkey already stored for the current query, if so
     *  we can modify the url given to include that sortkey.
     */
    protected void setResumableUrl() {
        ChangeEndpoints originalURL = (ChangeEndpoints) getUrl();
        String sortKey = CommitMarker.getSortKeyForQuery(mContext, originalURL.getStatus());
        if (sortKey != null) {
            originalURL.setSortKey(sortKey);
            super.setUrl(originalURL);
        }
    }

    private JSONCommit findCommit(JSONCommit[] commits, @NotNull String changeID) {
        for (JSONCommit commit : commits) {
            if (changeID.equals(commit.getChangeId()))
                return commit;
        }
        return null;
    }

    private boolean areFetchingChangesForStatus(@NotNull String status) {
        Class<? extends SyncProcessor> clazz = ChangeListProcessor.class;
        HashMap<RequestBuilder, SyncProcessor> processors = GerritService.getRunningProcessors();

        for (Map.Entry<RequestBuilder, SyncProcessor> entry : processors.entrySet()) {
            if (entry.getValue().getClass().equals(clazz) && status.equals(entry.getKey().getQuery()))
                return true;
        }
        return false;
    }

    @Override
    int count(JSONCommit[] data) {
        if (data != null) return data.length;
        else return 0;
    }
}
