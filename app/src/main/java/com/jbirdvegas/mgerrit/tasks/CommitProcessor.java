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

import com.crashlytics.android.Crashlytics;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.Changes;
import com.jbirdvegas.mgerrit.database.Labels;
import com.jbirdvegas.mgerrit.database.MessageInfo;
import com.jbirdvegas.mgerrit.database.Reviewers;
import com.jbirdvegas.mgerrit.database.Revisions;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.helpers.AnalyticsHelper;
import com.jbirdvegas.mgerrit.helpers.ApiHelper;
import com.urswolfer.gerrit.client.rest.GerritRestApi;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.NoSuchElementException;

class CommitProcessor extends SyncProcessor<ChangeInfo> {

    private final String mChangeId;
    private final int mChangeNumber;

    CommitProcessor(Context context, Intent intent) {
        super(context, intent);
        mChangeId = intent.getStringExtra(GerritService.CHANGE_ID);
        mChangeNumber = intent.getIntExtra(GerritService.CHANGE_NUMBER, 0);
    }

    @Override
    int insert(ChangeInfo commit) {
        return doInsert(getContext(), commit);
    }

    @Override
    boolean isSyncRequired(Context context) {
        return true;
    }

    @Override
    int count(ChangeInfo data) {
        return data == null ? 0 : 1;
    }

    @Override
    ChangeInfo getData(GerritRestApi gerritApi) throws RestApiException {
        EnumSet<ListChangesOption> options = queryOptions();
        ChangeApi change;
        int changeNumber = mChangeNumber;
        try {
            change = ApiHelper.fetchChange(mContext, gerritApi, mChangeId, changeNumber);
            return change.get(options);
        } catch (IllegalArgumentException | RestApiException | NoSuchElementException e1) {
            /* We may have a situation where multiple changes have the same change id
             * this usually occurs when cherry-picking or reverting a commit.
             * We have to fallback to the legacy change number
             * See: http://review.cyanogenmod.org/#/q/change:I6c7a14a9ab4090b4aabf5de7663f5de51bdc4615 */
            if (changeNumber < 1) {
                changeNumber = Changes.getChangeNumberForChange(mContext, mChangeId);
            }
            try {
                if (changeNumber > 0) {
                    change = gerritApi.changes().id(changeNumber);
                } else {
                    throw new RestApiException(e1.getMessage(), e1);
                }

                try {
                    return change.get(options);
                } catch (IllegalArgumentException | RestApiException | NoSuchElementException e2) {
                    // The server may have errored out again on this change
                    throw fetchCommitFailureHelper(e2);
                }
            } catch (RestApiException e2) {
                throw fetchCommitFailureHelper(e2);
            }
        }
    }

    @Override
    protected boolean doesProcessorConflict(@NotNull SyncProcessor processor) {
        if (!this.getClass().equals(processor.getClass())) return false;
        // Don't fetch the same changeid
        String changeId = processor.getIntent().getStringExtra(GerritService.CHANGE_ID);
        return mChangeId.equals(changeId);
    }

    protected static int doInsert(Context context, ChangeInfo commit) {
        if (commit == null) return 0;

        String changeid = commit.changeId;

        Reviewers.insertReviewers(context, changeid, commit.labels);
        Revisions.insertRevision(context, changeid, commit.revisions.get(commit.currentRevision));
        MessageInfo.insertMessages(context, changeid, commit.messages);

        UserChanges.updateChange(context, commit);

        if (!commit.permittedLabels.isEmpty()) {
            Labels.insertLabels(context, commit.project, commit.labels, commit.permittedLabels);
        }

        return 1;
    }

    private EnumSet<ListChangesOption> queryOptions() {
        EnumSet<ListChangesOption> options = EnumSet.noneOf(ListChangesOption.class);
        options.add(ListChangesOption.CURRENT_REVISION);
        options.add(ListChangesOption.CURRENT_COMMIT);
        options.add(ListChangesOption.CURRENT_FILES);
        // Need the account id of the owner here to maintain FK db constraint
        options.add(ListChangesOption.DETAILED_ACCOUNTS);
        options.add(ListChangesOption.MESSAGES);
        options.add(ListChangesOption.DETAILED_LABELS);
        return options;
    }

    private RestApiException fetchCommitFailureHelper(Exception e) {
        // We don't have anything we can use to uniquely identify the change we are trying to fetch
        RestApiException exception = new RestApiException("Cannot fetch change " + mChangeId + " as it is not unique", e);
        AnalyticsHelper.setCustomString(AnalyticsHelper.C_CHANGE_ID, mChangeId);
        AnalyticsHelper.setCustomInt(AnalyticsHelper.C_CHANGE_ID, mChangeNumber);
        Crashlytics.logException(exception);
        return exception;
    }
}
