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
import org.json.JSONException;
import org.json.JSONObject;

public class ChangedFile implements Parcelable {
    private String path;
    private int inserted;
    private int deleted;

    public ChangedFile(String draft) {
        path = draft;
        inserted = -1;
        deleted = -1;
    }

    private ChangedFile(String _path, JSONObject object) throws JSONException {
        path = _path;
        try {
            inserted = object.getInt(JSONCommit.KEY_INSERTED);
        } catch (JSONException noInserted) {
            inserted = Integer.MIN_VALUE;
        }
        try {
            deleted = object.getInt(JSONCommit.KEY_DELETED);
        } catch (JSONException noDeleted) {
            deleted = Integer.MIN_VALUE;
        }
    }

    public String getPath() {
        return this.path;
    }

    public int getInserted() {
        return this.inserted;
    }

    public int getDeleted() {
        return this.deleted;
    }

    public static ChangedFile parseFromJSONObject(String _path,
                                                  JSONObject object)
            throws JSONException {
        return new ChangedFile(_path, object);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ChangedFile");
        sb.append("{path='").append(path).append('\'');
        sb.append(", inserted=").append(inserted);
        sb.append(", deleted=").append(deleted);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(path);
        parcel.writeInt(inserted);
        parcel.writeInt(deleted);
    }

    public ChangedFile(Parcel parcel) {
        path = parcel.readString();
        inserted = parcel.readInt();
        deleted = parcel.readInt();
    }

    public static final Parcelable.Creator<ChangedFile> CREATOR
            = new Parcelable.Creator<ChangedFile>() {
        public ChangedFile createFromParcel(Parcel in) {
            return new ChangedFile(in);
        }

        public ChangedFile[] newArray(int size) {
            return new ChangedFile[size];
        }
    };
}