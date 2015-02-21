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

import com.android.volley.RequestQueue;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.accounts.AccountApi;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import org.apache.http.impl.client.DefaultHttpClient;

import de.greenrobot.event.EventBus;

/**
 * Created by Evan on 21/02/2015.
 */
public class AccountProcessor extends SyncProcessor<AccountInfo> {

    private final String mCurrentGerritUrl, mUrl;
    private final EventBus mEventBus;
    private final String username, password;
    private final Intent mIntent;

    AccountProcessor(Context context, Intent intent) {
        super(context, intent);
        mCurrentGerritUrl = Prefs.getCurrentGerrit(context);
        mEventBus = EventBus.getDefault();
        username = intent.getStringExtra(GerritService.HTTP_USERNAME);
        password = intent.getStringExtra(GerritService.HTTP_PASSWORD);
        mUrl = intent.getStringExtra(GerritService.URL_KEY);
        mIntent = intent;
    }

    @Override
    int insert(AccountInfo data) {

        return 1;
    }

    @Override
    boolean isSyncRequired(Context context) {
        return true;
    }

    @Override
    Class<AccountInfo> getType() {
        return AccountInfo.class;
    }

    @Override
    int count(AccountInfo version) {
        return 1;
    }

    @Override
    protected void fetchData(RequestQueue queue) {
        String url = mCurrentGerritUrl + "accounts/self";

        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        GerritAuthData.Basic authData = new GerritAuthData.Basic(mCurrentGerritUrl, username, password);
        GerritApi gerritApi = gerritRestApiFactory.create(authData);
        try {
            AccountInfo self = gerritApi.accounts().self().get();
            super.getSimpleListener(self, url);
        } catch (RestApiException e) {
            e.printStackTrace();
            mEventBus.post(new ErrorDuringConnection(mIntent, url, null, e));
        }
    }
}
