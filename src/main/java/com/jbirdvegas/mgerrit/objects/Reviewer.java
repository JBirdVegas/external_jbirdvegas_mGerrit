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

public class Reviewer {
    public static final String NO_SCORE = "No score";

    @SerializedName("value")
    private Integer mValue = 0;

    private CommitterObject mCommitter;

    @SerializedName("date")
    private final String mDate;

    public enum Label { CodeReview("Code-Review"), Verified("Verified");
        String name;

        Label(String s) {
            name = s;
        }

        public static Label getLabelFromString(String s) {
            if (s.equals(CodeReview.name)) return CodeReview;
            else return Verified;
        }
    } private Label mLabel;

    public Reviewer(Integer value, String name, String email) {
        mValue = value;
        mCommitter = CommitterObject.getInstance(name, email);
        mDate = null;
    }

    public CommitterObject getCommiterObject() {
        return mCommitter;
    }

    public Integer getValue() {
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
                "mValue=" + mValue +
                ", mCommitter=" + mCommitter +
                ", mDate='" + mDate + '\'' +
                ", mLabel='" + mLabel + '\'' +
                '}';
    }

    public Label getLabel() {
        return mLabel;
    }

    public void setLabel(Label label) {
        this.mLabel = label;
    }

    public void setLabel(String s) {
        this.mLabel = Label.getLabelFromString(s);
    }
}
