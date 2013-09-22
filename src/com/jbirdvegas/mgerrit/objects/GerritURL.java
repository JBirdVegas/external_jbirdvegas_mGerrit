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

import com.jbirdvegas.mgerrit.StaticWebAddress;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that helps to deconstruct Gerrit queries and assemble them
 *  when necessary. This allows for setting individual parts of a query
 *  without knowing other query parameters.
 */
public class GerritURL implements Parcelable
{
    private static String sGerritBase;
    private static String sProject = "";
    private String mStatus = "";
    private String mEmail = "";
    private String mCommitterState = "";
    private boolean mRequestDetailedAccounts = false;
    private boolean mListProjects = false;
    private String mSortkey = "";

    // Default constructor to facilitate instanciation
    public GerritURL() {
        super();
    }

    public static void setGerrit(String mGerritBase) {
        GerritURL.sGerritBase = mGerritBase;
    }

    public static void setProject(String project) {
        if (project == null) project = "";
        sProject = project;
    }

    public void setStatus(String status) {
        if (status == null) status = "";
        mStatus = status;
    }

    public void setEmail(String email) {
        if (email == null) email = "";
        mEmail = email;
    }

    public void setCommitterState(String committerState) {
        if (committerState == null) committerState = "";
        mCommitterState = committerState;
    }

    /**
     * DETAILED_ACCOUNTS: include _account_id and email fields when referencing accounts.
     * @param requestDetailedAccounts true if to include the additional fields in the response
     */
    public void setRequestDetailedAccounts(boolean requestDetailedAccounts) {
        mRequestDetailedAccounts = requestDetailedAccounts;
    }

    // Setting this will ignore all change related parts of the query URL
    public void listProjects() {
        mListProjects = true;
    }

    /**
     * Use the sortKey to resume a query from a given change. This is only valid
     *  for requesting change lists.
     * @param sortKey The sortkey of a given change.
     */
    public void setSortKey(String sortKey) {
        mSortkey = sortKey;
    }

    @Override
    public String toString()
    {
        boolean addPlus = false;

        // Sanity checking, this value REALLY should be set.
        if (sGerritBase == null) {
            throw new NullPointerException("Base Gerrit URL is null, did you forget to set one?");
        }

        if (mListProjects) {
            return new StringBuilder(0)
                    .append(sGerritBase)
                    .append("projects/?d")
                    .toString();
        }

        StringBuilder builder = new StringBuilder(0)
            .append(sGerritBase)
            .append(StaticWebAddress.getQuery());

        if (!"".equals(mStatus))
        {
            builder.append(JSONCommit.KEY_STATUS)
                    .append(":")
                    .append(mStatus);
            addPlus = true;
        }

        if (!"".equals(mCommitterState) && !"".equals(mEmail))
        {
            if (addPlus) builder.append('+');
            builder.append(mCommitterState)
                .append(':')
                .append(mEmail);
            addPlus = true;
        }

        try {
            if (!"".equals(sProject))
            {
                if (addPlus) builder.append('+');
                builder.append(JSONCommit.KEY_PROJECT)
                    .append(":")
                    .append(URLEncoder.encode(sProject, "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (!"".equals(mSortkey)) {
            builder.append("&P=").append(mSortkey);
        }

        if (mRequestDetailedAccounts) {
            builder.append(JSONCommit.DETAILED_ACCOUNTS_ARG);
        }

        return builder.toString();
    }

    public String getStatus() {
        return mStatus;
    }

    public String getQuery() {
        if (mStatus == null) {
            return null;
        } else {
            return new StringBuilder(0)
                    .append(JSONCommit.KEY_STATUS)
                    .append(":")
                    .append(mStatus).toString();
        }
    }

    /**
     * Get the status query portion of the string
     * @param str A gerrit query url, ideally the result of this class's toString method.
     * @return The status query in the form "status:xxx"
     */
    public static String getQuery(String str) {
        Pattern MY_PATTERN = Pattern.compile("(" + JSONCommit.KEY_STATUS + ":.*?)[+&]");
        Matcher m = MY_PATTERN.matcher(str);
        if (m.find()) return m.group(1);
        else return null;
    }

    public boolean equals(String str) {
        return this.toString().equals(str);
    }

    // --- Parcelable stuff so we can send this object through intents ---

    public static final Creator<GerritURL> CREATOR
            = new Creator<GerritURL>() {
        public GerritURL createFromParcel(Parcel in) {
            return new GerritURL(in);
        }

        @Override
        public GerritURL[] newArray(int size) {
            return new GerritURL[0];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sGerritBase);
        dest.writeString(sProject);
        dest.writeString(mStatus);
        dest.writeString(mEmail);
        dest.writeString(mCommitterState);
        dest.writeInt(mListProjects ? 1 : 0);
        dest.writeInt(mRequestDetailedAccounts ? 1 : 0);
        dest.writeString(mSortkey);
    }

    public GerritURL(Parcel in) {
        sGerritBase = in.readString();
        sProject = in.readString();
        mStatus = in.readString();
        mEmail = in.readString();
        mCommitterState = in.readString();
        mListProjects = in.readInt() == 1;
        mRequestDetailedAccounts = in.readInt() == 1;
        mSortkey = in.readString();
    }
}
