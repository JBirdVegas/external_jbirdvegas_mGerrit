/*
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2015
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

package com.jbirdvegas.mgerrit.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.MessageInfo;
import com.jbirdvegas.mgerrit.helpers.AnalyticsHelper;
import com.jbirdvegas.mgerrit.helpers.ApiHelper;
import com.urswolfer.gerrit.client.rest.GerritRestApi;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class ReviewProcessor extends SyncProcessor<ChangeInfo> {

    private String mMessage;
    private Bundle mLabels;
    private String mChangeId;

    ReviewProcessor(Context context, Intent intent) {
        super(context, intent);
        mChangeId = intent.getStringExtra(GerritService.CHANGE_ID);
        mMessage = intent.getStringExtra(GerritService.REVIEW_MESSAGE);
        mLabels = intent.getBundleExtra(GerritService.CHANGE_LABELS);
    }

    @Override
    int insert(ChangeInfo commit) {
        if (commit == null) return 0;
        String changeid = commit.changeId;
        MessageInfo.insertMessages(getContext(), changeid, commit.messages);
        return 1;
    }

    @Override
    boolean isSyncRequired(Context context) {
        // Can only add a review if we have a comment
        return (mMessage != null && !mMessage.isEmpty());
    }

    @Override
    int count(ChangeInfo commit) {
        return (commit == null) ? 0 : 1;
    }

    @Override
    protected ChangeInfo getData(GerritRestApi gerritApi) throws RestApiException {
        ReviewInput reviewInput = new ReviewInput();
        if (mMessage != null && !mMessage.isEmpty()) {
            reviewInput = reviewInput.message(mMessage);

            if (mLabels != null) {
                for (String label : mLabels.keySet()) {
                    reviewInput = reviewInput.label(label, mLabels.getInt(label));
                }
            }

            ChangeApi change = ApiHelper.fetchChange(mContext, gerritApi, mChangeId, null);
            change.current().review(reviewInput);

            // We need to look up the change again so we know what was set on the change
            return change.get(queryOptions());
        } else {
            return null;
        }
    }

    @Override
    protected boolean doesProcessorConflict(@NotNull SyncProcessor processor) {
        if (!this.getClass().equals(processor.getClass())) return false;
        // Don't comment on the same changeid
        String changeId = processor.getIntent().getStringExtra(GerritService.CHANGE_ID);
        return mChangeId.equals(changeId);
    }

    @Override
    protected void trackEvent(String currentGerrit) {
        AnalyticsHelper.getInstance().sendAnalyticsEvent(mContext, mContext.getString(R.string.ga_authorized_action),
                mContext.getString(R.string.ga_change_comment_added), currentGerrit, null);
    }

    private EnumSet<ListChangesOption> queryOptions() {
        EnumSet<ListChangesOption> options = EnumSet.noneOf(ListChangesOption.class);
        // Need the account id of the owner here to maintain FK db constraint
        options.add(ListChangesOption.DETAILED_ACCOUNTS);
        options.add(ListChangesOption.MESSAGES);
        options.add(ListChangesOption.DETAILED_LABELS);
        return options;
    }
}
