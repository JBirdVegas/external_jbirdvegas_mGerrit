package com.jbirdvegas.mgerrit.objects;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
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

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gerrit.extensions.common.ChangeInfo;

import java.util.ArrayList;
import java.util.List;

public class ChangeList extends ArrayList<ChangeInfo> implements Parcelable {

    public ChangeList(List<ChangeInfo> changes) {
        clear();
        addAll(changes);
    }

    protected ChangeList(Parcel in) {
       in.readSerializable();
    }

    public static final Creator<ChangeList> CREATOR = new Creator<ChangeList>() {
        @Override
        public ChangeList createFromParcel(Parcel in) {
            return new ChangeList(in);
        }

        @Override
        public ChangeList[] newArray(int size) {
            return new ChangeList[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(this);
    }
}
