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

import com.google.gerrit.extensions.api.changes.Changes.QueryRequest;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.Changes;
import com.jbirdvegas.mgerrit.database.CommitMarker;
import com.jbirdvegas.mgerrit.database.Config;
import com.jbirdvegas.mgerrit.database.DatabaseTable;
import com.jbirdvegas.mgerrit.database.MoreChanges;
import com.jbirdvegas.mgerrit.database.SyncTime;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.objects.ServerVersion;
import com.jbirdvegas.mgerrit.search.SearchKeyword;
import com.jbirdvegas.mgerrit.tasks.GerritService.Direction;
import com.urswolfer.gerrit.client.rest.GerritRestApi;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.util.List;

class ChangeListProcessor extends SyncProcessor<List<ChangeInfo>> {

    GerritService.Direction mDirection;
    private final String mStatus;
    private final List<SearchKeyword> mSearchKeywords;
    private String mSortKey = null;

    /**
     * Standard constructor to create a SyncProcessor
     *
     * @param context Context for network access
     * @param intent  The original intent to GerritService that started initiated
     */
    ChangeListProcessor(Context context, @NotNull Intent intent) {
        super(context, intent);

        Direction direction = (Direction) intent.getSerializableExtra(GerritService.CHANGES_LIST_DIRECTION);
        if (direction != null) mDirection = direction;
        else mDirection = Direction.Newer;

        mStatus = getStatus();

        // If we are loading newer changes using an old Gerrit instance, set the sortkey
        if (mDirection == Direction.Newer) {
            ServerVersion version = Config.getServerVersion(context);
            if (version == null || !version.isFeatureSupported("2.8.1")) {
                mSortKey = CommitMarker.getSortKeyForQuery(mContext, mStatus);
            }
        }

        mSearchKeywords = (List<SearchKeyword>) intent.getSerializableExtra(GerritService.CHANGE_KEYWORDS);
    }

    @Override
    int insert(List<ChangeInfo> commits) {
        if (commits.size() > 0) {
            return UserChanges.insertCommits(getContext(), commits);
        }
        return 0;
    }

    @Override
    protected boolean doesProcessorConflict(@NotNull SyncProcessor processor) {
        if (!this.getClass().equals(processor.getClass())) return false;
        if (!mStatus.equals(processor.getStatus())) return false;
        // We are already fetching changes for this status
        return true;
    }

    @Override
    boolean isSyncRequired(Context context) {
        // Note that we have already checked if we are already fetching changes for this status
        if (mStatus == null) return true; // If we have not specified a status we are doing a query on all past changes
        else if (mDirection == Direction.Older) return true;

        long syncInterval = context.getResources().getInteger(R.integer.changes_sync_interval);
        long lastSync = SyncTime.getValueForQuery(context, SyncTime.CHANGES_LIST_SYNC_TIME, mStatus);
        boolean sync = isInSyncInterval(syncInterval, lastSync);
        if (!sync) return true;

        // Better just make sure that there are changes in the database
        return DatabaseTable.isEmpty(context, Changes.CONTENT_URI);
    }

    @Override
    void doPostProcess(List<ChangeInfo> data) {
        boolean moreChanges = false;

        if (data.size() > 0) {
            ChangeInfo fc = data.get(0);
            if (fc._moreChanges == null) {
                Boolean mc = data.get(data.size() - 1)._moreChanges;
                // _moreChanges may be null and null cannot be assigned to a (primitive) boolean
                moreChanges = !(mc == null || !mc);
            }
            else moreChanges = fc._moreChanges;
        }

        if (mDirection == Direction.Newer) {
            SyncTime.setValue(mContext, SyncTime.CHANGES_LIST_SYNC_TIME,
                    System.currentTimeMillis(), mStatus);

            // Save our spot using the sortkey of the most recent change
            Pair<String, Integer> change = Changes.getMostRecentChange(mContext, mStatus);
            if (change != null) {
                String changeID = change.first;
                if (changeID != null && !changeID.isEmpty()) {
                    ChangeInfo commit = findCommit(data, changeID);
                    if (commit != null) {
                        CommitMarker.markCommit(mContext, commit);
                    }
                }
            }
        }
        if (mStatus != null) {
            MoreChanges.insert(mContext, mStatus, mDirection, moreChanges);
        }
    }

    @Override
    protected List<ChangeInfo> getData(GerritRestApi gerritApi)
            throws RestApiException {
        QueryRequest info = gerritApi.changes().query()
                .withLimit(mContext.getResources().getInteger(R.integer.changes_limit))
                .withOption(ListChangesOption.DETAILED_ACCOUNTS);
        if (mSortKey != null) info = info.withSortkey(mSortKey);

        StringBuilder builder = null;
        try {
            builder = SearchKeyword.asQuery(mContext, mSearchKeywords);
        } catch (UnsupportedEncodingException e) {
            handleException(e);
        }

        if (builder.length() > 0) builder = builder.append('+');
        String query = builder.append(getQuery()).toString();
        if (query.length() > 0) info = info.withQuery(builder.toString());

        return info.get();
    }

    private ChangeInfo findCommit(List<ChangeInfo> commits, @NotNull String changeID) {
        for (ChangeInfo commit : commits) {
            if (changeID.equals(commit.changeId))
                return commit;
        }
        return null;
    }

    @Override
    int count(List<ChangeInfo> data) {
        if (data != null) return data.size();
        else return 0;
    }
}
