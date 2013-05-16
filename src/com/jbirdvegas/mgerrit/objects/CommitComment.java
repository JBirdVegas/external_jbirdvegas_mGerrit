package com.jbirdvegas.mgerrit.objects;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 5/13/13 12:59 AM
 */
public class CommitComment implements Parcelable {
    private static final String KEY_REVISION_NUMBER = "_revision_number";
    private static final boolean DEBUG = false;
    private static final String TAG = CommitComment.class.getSimpleName();
    private JSONObject mJsonObject;
    private int mRevisionNumber;
    private String mMessage;
    private String mDate;
    private CommitterObject mAuthorObject;
    private String mId;

    public CommitComment(JSONObject jsonObject) {
        mJsonObject = jsonObject;
        try {
            mId = jsonObject.getString(JSONCommit.KEY_ID);
            mAuthorObject = CommitterObject.getInstance(jsonObject.getJSONObject(JSONCommit.KEY_AUTHOR));
            mDate = jsonObject.getString(JSONCommit.KEY_DATE);
            mMessage = jsonObject.getString(JSONCommit.KEY_MESSAGE);
            mRevisionNumber = jsonObject.getInt(KEY_REVISION_NUMBER);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static CommitComment getInstance(JSONObject jsonObject) {
        if (DEBUG) {
            try {
                Log.d(TAG, "CommitComment RawJSON: " + jsonObject.toString(4));
            } catch (JSONException e) {
                Log.e(TAG, "DEBUG FAILED!", e);
            }
        }
        return new CommitComment(jsonObject);
    }

    public JSONObject getJsonObject() {
        return mJsonObject;
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

    public CommitComment(Parcel parcel) {
        try {
            mJsonObject = new JSONObject(parcel.readString());
            mId = parcel.readString();
            mAuthorObject = parcel.readParcelable(CommitterObject.class.getClassLoader());
            mDate = parcel.readString();
            mMessage = parcel.readString();
            mRevisionNumber = parcel.readInt();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create object from Parcel!", e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mJsonObject.toString());
        parcel.writeString(mId);
        parcel.writeParcelable(mAuthorObject, 0);
        parcel.writeString(mDate);
        parcel.writeString(mMessage);
        parcel.writeInt(mRevisionNumber);
    }
}
