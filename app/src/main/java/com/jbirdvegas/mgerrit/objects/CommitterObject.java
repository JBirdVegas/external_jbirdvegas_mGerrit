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

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Generic data container for a user. This combines both the AccountInfo
 *  and GitPerson info objects into one. As a result, some of these fields will
 *  be null.
 */
public class CommitterObject {
    private static final String OWNER = "owner";

    @SerializedName(JSONCommit.KEY_NAME)
    private final String mName;

    @SerializedName(JSONCommit.KEY_EMAIL)
    private final String mEmail;

    @SerializedName("date")
    private final String mDate;

    @SerializedName("tz")
    private final String mTimezone;

    @SerializedName(JSONCommit.KEY_ACCOUNT_ID)
    private final int mAccountId;
    // used when object is passed while looking for author specific
    // commits mState=[owner/author/committer/reviewer];
    private String mState;

    private CommitterObject(String name,
                            String email,
                            String date,
                            String timezone) {
        mName = name;
        mEmail = email;
        mDate = date;
        mTimezone = timezone;
        mAccountId = -1;
        mState = OWNER;
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
        mState = OWNER;
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

    public CommitterObject setState(String state) {
        this.mState = state;
        return this;
    }

    public String getState() {
        return mState;
    }

    public int getAccountId() {
        return mAccountId;
    }

    @Override
    public String toString() {
        return "CommitterObject{" +
                "name='" + mName + '\'' +
                ", email='" + mEmail + '\'' +
                ", date='" + mDate + '\'' +
                ", timezone='" + mTimezone + '\'' +
                ", accountId=" + mAccountId +
                ", state='" + mState + '\'' +
                '}';
    }

    /*
     * No two users should share the same account ID. This should be the same as the
     *  primary key constraint in the database.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        else if (o == null || getClass() != o.getClass()) return false;

        CommitterObject that = (CommitterObject) o;

        return mAccountId == that.mAccountId;

    }

    @Override
    public int hashCode() {
        return mAccountId;
    }
}