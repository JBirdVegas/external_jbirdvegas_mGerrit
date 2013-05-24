package com.jbirdvegas.mgerrit.objects;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jbird on 5/24/13.
 */
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
