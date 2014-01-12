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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

public class GooFileObject implements Parcelable {

    // fields available from goo json api

    @SerializedName("id")
    private int mId;

    @SerializedName("filename")
    private String mFileName;

    @SerializedName("path")
    private String mPath;

    @SerializedName("folder")
    private String mFolder;

    @SerializedName("md5")
    private String mMd5;

    @SerializedName("type")
    private String mType;

    @SerializedName("description")
    private String mDescription;

    @SerializedName("is_flashable")
    private boolean mIsFlashable;

    @SerializedName("modified")
    private long mModified;

    @SerializedName("downloads")
    private long mDownloads;

    @SerializedName("status")
    private int mStatus;

    @SerializedName("additional_info")
    private String mAdditionalInfo;

    @SerializedName("short_url")
    private String mShortUrl;

    @SerializedName("developer_id")
    private int mDeveloperId;

    @SerializedName("ro_developerid")
    private String mRO_DeveloperId;

    @SerializedName("ro_board")
    private String mRO_Board;

    @SerializedName("ro_rom")
    private String mRO_Rom;

    @SerializedName("ro_version")
    private int mRO_Version;

    @SerializedName("gapps_package")
    private long mGappsPackage;

    @SerializedName("incremental_file")
    private int mIncrementalFile;

    @SerializedName("gapps_link")
    private String mGappsLink;

    @SerializedName("gapps_md5")
    private String mGappsMd5;

    public static GooFileObject getInstance(JSONObject jsonObject) throws JSONException {
        GooFileObject object = new Gson().fromJson(jsonObject.toString(), GooFileObject.class);
        if (object.mShortUrl == null || object.mShortUrl.isEmpty()) {
            object.mShortUrl = "http://goo.im" + object.mPath;
        }
        return object;
    }

    @Override
    public String toString() {
        return new StringBuilder(0)
                .append("GooFileObject{")
                .append("mId=").append(mId)
                .append(", mFileName='").append(mFileName).append('\'')
                .append(", mPath='").append(mPath).append('\'')
                .append(", mFolder='").append(mFolder).append('\'')
                .append(", mMd5='").append(mMd5).append('\'')
                .append(", mType='").append(mType).append('\'')
                .append(", mDescription='").append(mDescription).append('\'')
                .append(", mIsFlashable=").append(mIsFlashable)
                .append(", mModified=").append(mModified)
                .append(", mDownloads=").append(mDownloads)
                .append(", mStatus=").append(mStatus)
                .append(", mAdditionalInfo='").append(mAdditionalInfo).append('\'')
                .append(", mShortUrl='").append(mShortUrl).append('\'')
                .append(", mDeveloperId=").append(mDeveloperId)
                .append(", mRO_DeveloperId='").append(mRO_DeveloperId).append('\'')
                .append(", mRO_Board='").append(mRO_Board).append('\'')
                .append(", mRO_Rom='").append(mRO_Rom).append('\'')
                .append(", mRO_Version=").append(mRO_Version)
                .append(", mGappsPackage=").append(mGappsPackage)
                .append(", mIncrementalFile=").append(mIncrementalFile)
                .append(", mGappsLink='").append(mGappsLink).append('\'')
                .append(", mGappsMd5='").append(mGappsMd5).append('\'')
                .append('}').toString();
    }

    /**
     * Getters for all fields
     */
    public int getId() {
        return mId;
    }

    public String getFileName() {
        return mFileName;
    }

    public String getPath() {
        return mPath;
    }

    public String getFolder() {
        return mFolder;
    }

    public String getMd5() {
        return mMd5;
    }

    public String getType() {
        return mType;
    }

    public String getDescription() {
        return mDescription;
    }

    public boolean isIsFlashable() {
        return mIsFlashable;
    }

    public long getModified() {
        // goo provides this in seconds we want milliseconds
        return mModified * 1000;
    }

    public long getDownloads() {
        return mDownloads;
    }

    public int getStatus() {
        return mStatus;
    }

    public String getAdditionalInfo() {
        return mAdditionalInfo;
    }

    public String getShortUrl() {
        return mShortUrl;
    }

    public int getDeveloperId() {
        return mDeveloperId;
    }

    public String getRO_DeveloperId() {
        return mRO_DeveloperId;
    }

    public String getRO_Board() {
        return mRO_Board;
    }

    public String getRO_Rom() {
        return mRO_Rom;
    }

    public int getRO_Version() {
        return mRO_Version;
    }

    public long getGappsPackage() {
        return mGappsPackage;
    }

    public int getIncrementalFile() {
        return mIncrementalFile;
    }

    public String getGappsLink() {
        return mGappsLink;
    }

    public String getGappsMd5() {
        return mGappsMd5;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mId);
        parcel.writeString(mFileName);
        parcel.writeString(mPath);
        parcel.writeString(mFolder);
        parcel.writeString(mMd5);
        parcel.writeString(mType);
        parcel.writeString(mDescription);
        parcel.writeString(String.valueOf(mIsFlashable));
        parcel.writeLong(mModified);
        parcel.writeLong(mDownloads);
        parcel.writeInt(mStatus);
        parcel.writeString(mAdditionalInfo);
        parcel.writeString(mShortUrl);
        parcel.writeInt(mDeveloperId);
        parcel.writeString(mRO_DeveloperId);
        parcel.writeString(mRO_Board);
        parcel.writeString(mRO_Rom);
        parcel.writeInt(mRO_Version);
        parcel.writeLong(mGappsPackage);
        parcel.writeInt(mIncrementalFile);
        parcel.writeString(mGappsLink);
        parcel.writeString(mGappsMd5);
    }

    public GooFileObject(Parcel parcel) {
        mId = parcel.readInt();
        mFileName = parcel.readString();
        mPath = parcel.readString();
        mFolder = parcel.readString();
        mMd5 = parcel.readString();
        mType =  parcel.readString();
        mDescription =  parcel.readString();
        mIsFlashable = Boolean.valueOf(parcel.readString());
        mModified = parcel.readLong();
        mDownloads =  parcel.readLong();
        mStatus = parcel.readInt();
        mAdditionalInfo =  parcel.readString();
        mShortUrl = parcel.readString();
        mDeveloperId = parcel.readInt();
        mRO_DeveloperId = parcel.readString();
        mRO_Board = parcel.readString();
        mRO_Rom = parcel.readString();
        mRO_Version = parcel.readInt();
        mGappsPackage = parcel.readLong();
        mIncrementalFile = parcel.readInt();
        mGappsLink = parcel.readString();
        mGappsMd5 = parcel.readString();
    }

    public static final Parcelable.Creator<GooFileObject> CREATOR
            = new Parcelable.Creator<GooFileObject>() {
        public GooFileObject createFromParcel(Parcel in) {
            return new GooFileObject(in);
        }

        public GooFileObject[] newArray(int size) {
            return new GooFileObject[size];
        }
    };
}