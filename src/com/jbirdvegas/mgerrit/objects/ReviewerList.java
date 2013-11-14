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

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ReviewerList implements Parcelable {

    private List<Reviewer> mReviewers;

    public ReviewerList(List<Reviewer> reviewers) {
        this.mReviewers = reviewers;
    }

    public ReviewerList(Reviewer[] reviewers) {
        this.mReviewers = new ArrayList<>();
        mReviewers.addAll(Arrays.asList(reviewers));
    }

    public ReviewerList(Parcel in) {
        in.readArrayList(this.getClass().getClassLoader());
    }

    public Reviewer[] getReviewers() {
        Reviewer[] reviewers = new Reviewer[mReviewers.size()];
        return mReviewers.toArray(reviewers);
    }

    @Override
    public String toString() {
        return "ReviewerList{" +
                "mReviewers=" + mReviewers +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mReviewers);
    }
}
