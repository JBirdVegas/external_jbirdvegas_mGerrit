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
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ChangeLogRange implements Parcelable {
    private static final boolean CHATTY = false;
    private static final String TAG = ChangeLogRange.class.getSimpleName();
    public static String KEY = "changeLogRange";
    private final SimpleDateFormat mSimpleDateFormat;
    private final Date stopDate;
    private final Date startDate;
    private final Date commitDate;
    private long start;
    private long stop;
    private GooFileObject gooStart;
    private GooFileObject gooStop;
    public ChangeLogRange(GooFileObject _gooStart, GooFileObject _gooStop) {
        gooStart = _gooStart;
        gooStop = _gooStop;
        start = _gooStart.getModified();
        stop = _gooStop.getModified();
        mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        startDate = new Date();
        stopDate = new Date();
        commitDate = new Date();
    }

    public boolean isInRange(long time) {
        startDate.setTime(start);
        stopDate.setTime(stop);
        commitDate.setTime(time);
        if (CHATTY) {
            Log.d(TAG, String.format("min: %s max: %s finding: %s",
                    startDate, stopDate, commitDate));
        }
        if (startDate.before(commitDate)
                && commitDate.before(stopDate)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(gooStart, 0);
        parcel.writeParcelable(gooStop, 0);
    }

    public ChangeLogRange(Parcel parcel) {
        this((GooFileObject) parcel.readParcelable(GooFileObject.class.getClassLoader()),
                (GooFileObject) parcel.readParcelable(GooFileObject.class.getClassLoader()));
    }

    public static final Parcelable.Creator<ChangeLogRange> CREATOR
            = new Parcelable.Creator<ChangeLogRange>() {
        public ChangeLogRange createFromParcel(Parcel in) {
            return new ChangeLogRange(in);
        }

        public ChangeLogRange[] newArray(int size) {
            return new ChangeLogRange[size];
        }
    };

    public long startTime() {
        return start;
    }

    public long endTime() {
        return stop;
    }
}
