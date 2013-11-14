package com.jbirdvegas.mgerrit.objects;

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
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.jbirdvegas.mgerrit.R;

public class CommitInfo implements Parcelable {

    @SerializedName(JSONCommit.KEY_AUTHOR)
    private CommitterObject mAuthorObject;

    @SerializedName(JSONCommit.KEY_COMMITTER)
    private CommitterObject mCommitterObject;

    @SerializedName(JSONCommit.KEY_MESSAGE)
    private String mMessage;

    @SerializedName(JSONCommit.KEY_SUBJECT)
    private String mSubject;

    private String mPatchSetNumber;

    public CommitInfo(Parcel in) {
        mAuthorObject = in.readParcelable(CommitterObject.class.getClassLoader());
        mCommitterObject = in.readParcelable(CommitterObject.class.getClassLoader());
        mMessage = in.readString();
        mSubject = in.readString();
        mPatchSetNumber = in.readString();
    }

    public String getPatchSetNumber() {
        return mPatchSetNumber;
    }

    public CommitterObject getAuthorObject() {
        return mAuthorObject;
    }

    public CommitterObject getCommitterObject() {
        return mCommitterObject;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getSubject() {
        return mSubject;
    }

    protected void setMessage(Context context) {
        if (mMessage == null) {
            this.mMessage = context.getString(R.string.current_revision_is_draft_message);
        }
    }

    @Override
    public String toString() {
        return "CommitInfo{" +
                "mAuthorObject=" + mAuthorObject +
                ", mCommitterObject=" + mCommitterObject +
                ", mMessage='" + mMessage + '\'' +
                ", mSubject='" + mSubject + '\'' +
                ", mPatchSetNumber='" + mPatchSetNumber + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mAuthorObject, 0);
        dest.writeParcelable(mCommitterObject, 0);
        dest.writeString(mMessage);
        dest.writeString(mSubject);
        dest.writeString(mPatchSetNumber);
    }
}
