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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

public class CommitComment {
    private static final String KEY_REVISION_NUMBER = "_revision_number";
    public static final String KEY_DATE = "date";

    @SerializedName(CommitComment.KEY_REVISION_NUMBER)
    private int mRevisionNumber;

    @SerializedName(CommitInfo.KEY_MESSAGE)
    private String mMessage;

    @SerializedName(KEY_DATE)
    private String mDate;

    @SerializedName(CommitInfo.KEY_AUTHOR)
    private CommitterObject mAuthorObject;

    @SerializedName(JSONCommit.KEY_ID)
    private String mId;

    public static CommitComment getInstance(JSONObject jsonObject) {
        return new Gson().fromJson(jsonObject.toString(), CommitComment.class);
    }

    public int getRevisionNumber() {
        return mRevisionNumber;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getDate() {
        return mDate;
    }

    public CommitterObject getAuthorObject() {
        return mAuthorObject;
    }

    public String getId() {
        return mId;
    }
}
