package com.jbirdvegas.mgerrit.objects;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Jon Stanford (JBirdVegas), 2013
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

import org.json.JSONException;
import org.json.JSONObject;

public class CommitterObject {
    private final String mName;
    private final String mEmail;
    private final String mDate;
    private final String mTimezone;
    private final int mAccountId;

    private CommitterObject(String name,
                           String email,
                           String date,
                           String timezone) {
        mName = name;
        mEmail = email;
        mDate = date;
        mTimezone = timezone;
        mAccountId = -1;
    }

    private CommitterObject(String name, String email) {
        this(name, email, null, null);
    }

    public CommitterObject(String name, String email, int accountId) {
        mAccountId = accountId;
        mName = name;
        mEmail = email;
        mDate = null;
        mTimezone = null;
    }

    public static CommitterObject getInstance(String name,
                              String email,
                              String date,
                              String timezone) {
        return new CommitterObject(name, email, date, timezone);
    }

    public static CommitterObject getInstance(String name,
                                              String email) {
        return new CommitterObject(name, email);
    }

    public static CommitterObject getInstance(JSONObject jsonObject)
            throws JSONException {
        return new CommitterObject(
                jsonObject.getString(JSONCommit.KEY_NAME),
                jsonObject.getString(JSONCommit.KEY_EMAIL),
                jsonObject.getInt(JSONCommit.KEY_ACCOUNT_ID));
    }

    public String getName() {
        return mName;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getDate() {
        return mDate;
    }

    public String getTimezone() {
        return mTimezone;
    }

    public int getAccountId() {
        return mAccountId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CommitterObject{");
        sb.append("mName='").append(mName).append('\'');
        sb.append(", mEmail='").append(mEmail).append('\'');
        sb.append(", mDate='").append(mDate).append('\'');
        sb.append(", mTimezone='").append(mTimezone).append('\'');
        sb.append(", mAccountId=").append(mAccountId);
        sb.append('}');
        return sb.toString();
    }
}