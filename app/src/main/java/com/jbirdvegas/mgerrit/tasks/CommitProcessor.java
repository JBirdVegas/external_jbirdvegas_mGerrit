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

import com.android.volley.Response;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.database.MessageInfo;
import com.jbirdvegas.mgerrit.database.Revisions;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.objects.Reviewer;
import com.jbirdvegas.mgerrit.requestbuilders.RequestBuilder;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

class CommitProcessor extends SyncProcessor<ChangeInfo> {

    private final Intent mIntent;
    private final String mUrl;
    private final String mChangeId;

    CommitProcessor(Context context, Intent intent, RequestBuilder url) {
        super(context, intent, url);
        mIntent = intent;
        mUrl = url.toString();
        mChangeId = intent.getStringExtra(GerritService.CHANGE_ID);
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
    Class<ChangeInfo> getType() {
        return ChangeInfo.class;
    }

    @Override
    int count(ChangeInfo data) {
        return data == null ? 0 : 1;
    }

    @Override
    ChangeInfo getData(GerritApi gerritApi) throws RestApiException {
        return gerritApi.changes().id(mChangeId).get(queryOptions());
    }

    @Nullable
    protected static Reviewer[] reviewersToArray(ChangeInfo commit) {
        /*List<Reviewer> rs = commit.getReviewers();
        if (rs == null) return null;
        return rs.toArray(new Reviewer[rs.size()]);*/
        return null;
    }

    protected static int doInsert(Context context, ChangeInfo commit) {
        if (commit == null) return 0;

        String changeid = commit.changeId;

        //Reviewer[] reviewers = reviewersToArray(commit);
        //Reviewers.insertReviewers(context, changeid, commit.removableReviewers);
        Revisions.insertRevision(context, changeid, commit.revisions.get(commit.currentRevision));
        MessageInfo.insertMessages(context, changeid, commit.messages);

        UserChanges.updateChange(context, commit);

        return 1;
    }

    private EnumSet<ListChangesOption> queryOptions() {
        EnumSet options = EnumSet.noneOf(ListChangesOption.class);
        options.add(ListChangesOption.CURRENT_REVISION);
        options.add(ListChangesOption.CURRENT_COMMIT);
        options.add(ListChangesOption.CURRENT_FILES);
        options.add(ListChangesOption.DETAILED_ACCOUNTS);
        options.add(ListChangesOption.MESSAGES);
        return options;
    }
}
