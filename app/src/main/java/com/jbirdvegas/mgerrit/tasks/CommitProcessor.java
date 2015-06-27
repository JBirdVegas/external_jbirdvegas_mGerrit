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

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.database.MessageInfo;
import com.jbirdvegas.mgerrit.database.Revisions;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.objects.EventQueue;
import com.jbirdvegas.mgerrit.objects.GerritMessage;
import com.jbirdvegas.mgerrit.objects.Reviewer;
import com.jbirdvegas.mgerrit.requestbuilders.RequestBuilder;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

class CommitProcessor extends SyncProcessor<ChangeInfo> {

    private final Intent mIntent;
    private final String mUrl;
    private final String mChangeId;

    CommitProcessor(Context context, Intent intent, RequestBuilder url) {
        super(context, intent, url);
        mIntent = intent;
        mUrl = url.toString();
        mChangeId = intent.getStringExtra(GerritService.CHANGE_ID);
        //attemptAuthenticatedRequest(url);
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

    @Override
    protected void fetchData(RequestQueue queue) {
        Response.Listener<ChangeInfo> listener = getListener(mUrl);

        GerritApi gerritApi = getGerritApiInstance(true);
        
        try {
            ChangeInfo info = gerritApi.changes().id(mChangeId).get(queryOptions());
            listener.onResponse(info);
        } catch (RestApiException e) {
            if (((HttpStatusException) e).getStatusCode() == 502) {
                Tools.launchSignin(mContext);
            }

            // We still want to post the exception
            // Make sure the sign in activity (if started above) will receive the ErrorDuringConnection message by making it sticky
            GerritMessage ev = new ErrorDuringConnection(mIntent, mUrl, null, e);
            EventQueue.getInstance().enqueue(ev, true);
        }
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
