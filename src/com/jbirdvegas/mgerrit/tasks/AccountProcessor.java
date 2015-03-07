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

import com.android.volley.RequestQueue;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.database.Users;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.SigninCompleted;
import com.jbirdvegas.mgerrit.objects.AccountEndpoints;
import com.jbirdvegas.mgerrit.objects.AccountInfo;


import de.greenrobot.event.EventBus;

public class AccountProcessor extends SyncProcessor<AccountInfo> {

    private final Intent mIntent;
    private final String mUrl;

    AccountProcessor(Context context, Intent intent, AccountEndpoints url) {
        super(context, intent, url);
        mIntent = intent;
        mUrl = url.toString();
    }

    @Override
    int insert(AccountInfo data) {
        // Password is not returned in the response, but we can get it from the original intent
        data.password = mIntent.getStringExtra(GerritService.HTTP_PASSWORD);
        Users.setUserDetails(mContext, data);
        Log.d(this.getClass().getName(), "You have successfully signed in: " + data.name + "(" + data.email + ")");
        EventBus.getDefault().post(new SigninCompleted(mIntent, mUrl));
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
}
