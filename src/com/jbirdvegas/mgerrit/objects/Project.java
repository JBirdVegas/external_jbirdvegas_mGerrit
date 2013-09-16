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

public final class Project implements Comparable<Project> {

    @SerializedName(JSONCommit.KEY_PROJECT)
    private final String mPath;

    @SerializedName(JSONCommit.KEY_KIND)
    private final String mKind;

    @SerializedName(JSONCommit.KEY_ID)
    private final String mId;

    public Project(String path, String kind, String id) {
        this.mPath = path;
        this.mKind = kind;
        this.mId = id;
    }

    public String getmKind() {
        return mKind;
    }

    public String getmId() {
        return mId;
    }

    public String getmPath() {
        return mPath;
    }

    @Override
    public String toString() {
        return mPath;
    }

    @Override
    public int compareTo(Project project) {
        int i = mPath.compareTo(project.getmPath());
        if (i != 0) return i;

        i = mKind.compareTo(project.getmKind());
        if (i != 0) return i;

        i = mId.compareTo(project.getmId());
        return i;
    }
}
