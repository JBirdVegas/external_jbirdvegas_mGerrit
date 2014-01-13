package com.jbirdvegas.mgerrit.message;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.jbirdvegas.mgerrit.R;

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
public class StatusSelected {

    /* Note: Must have the type declared static and public so receivers can subscribe
     * to this type of message */
    public static final String ACTION = "Change Status Update";
    public static final String STATUS = "Change Status";
    private final String mStatus;
    private Context mContext;

    public StatusSelected(Context context, String status) {
        mContext = context;
        mStatus = status;
    }

    public String getType() {
        return ACTION;
    }

    public String getMessage() {
        return mContext.getString(R.string.change_status_update);
    }

    public void sendUpdateMessage() {
        Intent intent = new Intent(getType());
        intent.putExtra(STATUS, mStatus);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
}
