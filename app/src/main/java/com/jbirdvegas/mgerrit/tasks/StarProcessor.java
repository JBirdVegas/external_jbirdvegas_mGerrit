package com.jbirdvegas.mgerrit.tasks;

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

import android.content.Context;
import android.content.Intent;

import com.google.gerrit.extensions.api.accounts.AccountApi;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.Changes;
import com.jbirdvegas.mgerrit.database.Config;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.message.NotSupported;
import com.jbirdvegas.mgerrit.objects.EventQueue;
import com.jbirdvegas.mgerrit.objects.GerritMessage;
import com.jbirdvegas.mgerrit.objects.ServerVersion;
import com.urswolfer.gerrit.client.rest.GerritRestApi;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;

import org.jetbrains.annotations.NotNull;

public class StarProcessor extends SyncProcessor<String> {

    private final Intent mIntent;
    private boolean mIsStarring;
    private String mChangeId;

    StarProcessor(Context context, Intent intent) {
        super(context, intent);
        mIntent = intent;
        mIsStarring = mIntent.getBooleanExtra(GerritService.IS_STARRING, true);
        mChangeId = mIntent.getStringExtra(GerritService.CHANGE_ID);
    }

    @Override
    int insert(String responseCode) {
        int changeNumber = mIntent.getIntExtra(GerritService.CHANGE_NUMBER, 0);
        Changes.starChange(mContext, mChangeId, changeNumber, mIsStarring);
        return 1;
    }

    @Override
    boolean isSyncRequired(Context context) {
        ServerVersion version = Config.getServerVersion(context);
        if (version != null && version.isFeatureSupported(ServerVersion.VERSION_STAR)) {
            return true;
        } else {
            String gerrit = PrefsFragment.getCurrentGerritName(context);
            String msg = String.format(context.getString(R.string.star_change_not_supported), gerrit, ServerVersion.VERSION_STAR);
            GerritMessage ev = new NotSupported(mIntent, getQueueId(), msg);
            EventQueue.getInstance().enqueue(ev, false);
            return false;
        }

    }

    @Override
    int count(String responseCode) {
        return 1;
    }

    @Override
    protected String getData(GerritRestApi gerritApi) throws RestApiException {
        AccountApi self = gerritApi.accounts().self();
        try {
            if (mIsStarring) self.starChange(mChangeId);
            else self.unstarChange(mChangeId);
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 422 || e.getStatusCode() == 404) {
                // We may have a case where multiple changes have the same change id.
                int changeNumber = mIntent.getIntExtra(GerritService.CHANGE_NUMBER, 0);
                if (mIsStarring) self.starChange(String.valueOf(changeNumber));
                else self.unstarChange(String.valueOf(changeNumber));
            } else {
                throw e; // Don't know how to recover from this
            }
        }
        return "204";

    }

    @Override
    protected boolean doesProcessorConflict(@NotNull SyncProcessor processor) {
        if (!this.getClass().equals(processor.getClass())) return false;
        // Don't star the same changeid
        String changeId = processor.getIntent().getStringExtra(GerritService.CHANGE_ID);
        return mChangeId.equals(changeId);
    }
}
