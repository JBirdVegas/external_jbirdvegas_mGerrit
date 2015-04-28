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
import android.support.annotation.Nullable;

import com.jbirdvegas.mgerrit.database.MessageInfo;
import com.jbirdvegas.mgerrit.database.Reviewers;
import com.jbirdvegas.mgerrit.database.Revisions;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.objects.Reviewer;
import com.jbirdvegas.mgerrit.requestbuilders.RequestBuilder;

import java.util.List;

class CommitProcessor extends SyncProcessor<JSONCommit> {

    CommitProcessor(Context context, Intent intent, RequestBuilder url) {
        super(context, intent, url);
        attemptAuthenticatedRequest(url);
    }

    @Override
    int insert(JSONCommit commit) {
        return doInsert(getContext(), commit);
    }

    @Override
    boolean isSyncRequired(Context context) {
        return true;
    }

    @Override
    Class<JSONCommit> getType() {
        return JSONCommit.class;
    }

    @Override
    int count(JSONCommit data) {
        return data == null ? 0 : 1;
    }

    @Nullable
    protected static Reviewer[] reviewersToArray(JSONCommit commit) {
        List<Reviewer> rs = commit.getReviewers();
        if (rs == null) return null;
        return rs.toArray(new Reviewer[rs.size()]);
    }

    protected static int doInsert(Context context, JSONCommit commit) {
        if (commit == null) return 0;

        String changeid = commit.getChangeId();

        Reviewer[] reviewers = reviewersToArray(commit);
        Reviewers.insertReviewers(context, changeid, reviewers);
        Revisions.insertRevision(context, commit.getPatchSet());
        MessageInfo.insertMessages(context, changeid, commit.getMessagesList());

        UserChanges.updateChange(context, commit);

        return 1;
    }
}
