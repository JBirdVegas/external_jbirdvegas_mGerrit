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

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Reviewer implements Parcelable {
    public static final String NO_SCORE = "No score";
    public static final String CODE_REVIEW_PLUS_TWO = "Looks good to me, approved";
    public static final String CODE_REVIEW_PLUS_ONE = "Looks good to me, but someone else must approve";
    public static final String CODE_REVIEW_MINUS_ONE = "I would prefer that you didn\u0027t submit this";
    public static final String CODE_REVIEW_MINUS_TWO = "Do not submit";
    public static final String VERIFIED_PLUS_ONE = "Verified";
    public static final String VERIFIED_MINUS_ONE = "Fails";

    @SerializedName("value")
    private String mValue;
    private CommitterObject mCommitter;

    @SerializedName("date")
    private final String mDate;

    public Reviewer(String value, String name, String email) {
        mValue = value;
        mCommitter = CommitterObject.getInstance(name, email);
        mDate = null;
    }

    public Reviewer(Parcel parcel) {
        mValue = parcel.readString();
        mCommitter = new CommitterObject(parcel);
        mDate = null;
    }

    public CommitterObject getCommiterObject() {
        return mCommitter;
    }

    public String getValue() {
        return mValue;
    }

    public String getName() {
        return mCommitter.getName();
    }

    public String getEmail() {
        return mCommitter.getEmail();
    }

    public void setCommitter(CommitterObject committer) {
        mCommitter = committer;
    }

    @Override
    public String toString() {
        return "Reviewer{" +
                "value='" + mValue + '\'' +
                ", name='" + mCommitter.getName() + '\'' +
                ", email='" + mCommitter.getEmail() + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mValue);
        mCommitter.writeToParcel(parcel, i);
    }
}