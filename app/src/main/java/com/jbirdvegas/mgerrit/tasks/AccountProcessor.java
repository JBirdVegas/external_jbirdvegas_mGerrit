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
import android.util.Log;

import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.database.Users;
import com.jbirdvegas.mgerrit.helpers.AnalyticsHelper;
import com.jbirdvegas.mgerrit.message.SigninCompleted;
import com.jbirdvegas.mgerrit.objects.UserAccountInfo;
import com.urswolfer.gerrit.client.rest.GerritRestApi;

import org.greenrobot.eventbus.EventBus;


public class AccountProcessor extends SyncProcessor<UserAccountInfo> {

    private final Intent mIntent;

    AccountProcessor(Context context, Intent intent) {
        super(context, intent);
        mIntent = intent;
    }

    @Override
    int insert(UserAccountInfo data) {
        // Password is not returned in the response, but we can get it from the original intent
        data.password = mIntent.getStringExtra(GerritService.HTTP_PASSWORD);
        Users.setUserDetails(mContext, data);
        Log.d(this.getClass().getName(), "You have successfully signed in: " + data.name + "(" + data.email + ")");
        EventBus.getDefault().post(new SigninCompleted(mIntent, getQueueId(), data.username, data.password));
        return 1;
    }

    @Override
    boolean isSyncRequired(Context context) {
        return true;
    }

    @Override
    int count(UserAccountInfo version) {
        return 1;
    }

    @Override
    UserAccountInfo getData(GerritRestApi gerritApi) throws RestApiException {
        return new UserAccountInfo(gerritApi.accounts().self().get());
    }

    @Override
    protected void fetchData() {
        GerritRestApi gerritApi = getGerritApiInstance(true);
        try {
            onResponse(getData(gerritApi));
        } catch (RestApiException e) {
            handleException(e);
        }
    }

    @Override
    protected void trackEvent(String currentGerrit) {
        AnalyticsHelper.sendAnalyticsEvent(mContext, AnalyticsHelper.GA_AUTHORISED_ACTION,
                AnalyticsHelper.EVENT_LOGGED_IN, currentGerrit, null);
    }
}
