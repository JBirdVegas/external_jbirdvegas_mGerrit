package com.jbirdvegas.mgerrit.requestbuilders;

/*
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
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
import android.support.annotation.Nullable;

import com.jbirdvegas.mgerrit.database.Config;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.objects.ServerVersion;
import com.jbirdvegas.mgerrit.search.IsSearch;
import com.jbirdvegas.mgerrit.search.SearchKeyword;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation for querying /changes/ Gerrit API endpoints
 */
public class ChangeEndpoints extends RequestBuilder implements Parcelable {

    private String mStatus = "";
    private boolean mRequestDetailedAccounts = false;
    private String mSortkey = "";
    private int mChangeNo = 0;

    private Set<SearchKeyword> mSearchKeywords;

    public ChangeEndpoints() { }

    private enum ChangeDetailLevels {
        DISABLED, // Do not fetch change details
        LEGACY,   // Fetch change details and use legacy URL (Gerrit 2.7 or lower)
        ENABLED   // Fetch change details and use new change details endpoint (Gerrit 2.8+)
    }
    private ChangeDetailLevels mRequestChangeDetail = ChangeDetailLevels.DISABLED;

    public static final String DETAILED_ACCOUNTS_ARG = "&o=DETAILED_ACCOUNTS";
    // used to query commit message
    public static final String CURRENT_PATCHSET_ARGS = new StringBuilder(0)
            .append("?o=CURRENT_REVISION")
            .append("&o=CURRENT_COMMIT")
            .append("&o=CURRENT_FILES")
            .toString();
    public static final String OLD_CHANGE_DETAIL_ARGS = new StringBuilder(0)
            .append("&o=CURRENT_REVISION")
            .append("&o=CURRENT_COMMIT")
            .append("&o=CURRENT_FILES")
            .append("&o=DETAILED_LABELS")
            .append("&o=MESSAGES")
            .toString();


    public ChangeEndpoints(ChangeEndpoints url) {
        super(url);
        mStatus = url.mStatus;
        mRequestDetailedAccounts = url.mRequestDetailedAccounts;
        mChangeNo = url.mChangeNo;
        mRequestChangeDetail = url.mRequestChangeDetail;
    }

    public void addSearchKeyword(SearchKeyword keyword) {
        if (mSearchKeywords == null) {
            mSearchKeywords = new HashSet<>();
        }
        if (keyword.requiresAuthentication()) {
            setAuthenticating(true);
        }
        mSearchKeywords.add(keyword);
    }

    public void addSearchKeywords(Set<SearchKeyword> keywords) {
        if (keywords == null) return;
        for (SearchKeyword keyword : keywords) addSearchKeyword(keyword);
    }

    public static ChangeEndpoints starred() {
        ChangeEndpoints ce = new ChangeEndpoints();
        ce.addSearchKeyword(new IsSearch("starred"));
        ce.setAuthenticating(true);
        return ce;
    }

    public void setStatus(String status) {
        if (status == null) status = "";
        mStatus = status;
    }

    /**
     * DETAILED_ACCOUNTS: include _account_id and email fields when referencing accounts.
     * @param requestDetailedAccounts true if to include the additional fields in the response
     */
    public void setRequestDetailedAccounts(boolean requestDetailedAccounts) {
        mRequestDetailedAccounts = requestDetailedAccounts;
    }

    public void requestChangeDetail(boolean request, Boolean useLegacyUrl) {
        if (!request) {
            mRequestChangeDetail = ChangeDetailLevels.DISABLED;
        } else if (!useLegacyUrl) {
            mRequestChangeDetail = ChangeDetailLevels.ENABLED;
            mRequestDetailedAccounts = false;
        } else {
            mRequestChangeDetail = ChangeDetailLevels.LEGACY;
            mRequestDetailedAccounts = true;
        }
    }

    public void setChangeNumber(int changeNumber) {
        mChangeNo = changeNumber;
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
    public String getStatus() {
        return mStatus;
    }

    @Nullable
    @Override
    public String getQuery() {
        if (mStatus == null) return null;
        else {
            return JSONCommit.KEY_STATUS + ":" + mStatus;
        }
    }

    private boolean appendStatus(StringBuilder builder, boolean addSeperator) {
        if (mStatus != null && !mStatus.isEmpty()) {
            if (addSeperator) builder.append('+');
            builder.append(JSONCommit.KEY_STATUS)
                    .append(":")
                    .append(mStatus);
            return true;
        }
        return false;
    }

    private boolean appendSearchKeywords(StringBuilder builder, boolean addSeperator)
            throws UnsupportedEncodingException {
        ServerVersion version = Config.getServerVersion(sContext);
        if (mSearchKeywords != null && !mSearchKeywords.isEmpty()) {
            for (SearchKeyword keyword : mSearchKeywords) {
                String operator =  URLEncoder.encode(keyword.getGerritQuery(version), "UTF-8");
                if (operator != null && !operator.isEmpty()) {
                    if (addSeperator) {
                        builder.append('+');
                    }
                    builder.append(operator);
                    addSeperator = true;
                }
            }
        }
        return addSeperator;
    }

    private void appendArgs(StringBuilder builder) {
        if (mSortkey != null && !mSortkey.isEmpty()) {
            builder.append("&P=").append(mSortkey);
        }
        if (mRequestChangeDetail == ChangeDetailLevels.LEGACY) {
            builder.append(ChangeEndpoints.OLD_CHANGE_DETAIL_ARGS);
        }
        if (mRequestDetailedAccounts) {
            builder.append(ChangeEndpoints.DETAILED_ACCOUNTS_ARG);
        }

        int limit = getLimit();
        if (limit > 0) {
            builder.append("&n=").append(limit);
        }
    }

    public String getPath() {
        boolean addSeperator;
        StringBuilder builder = new StringBuilder(0).append("changes/");

        if (mRequestChangeDetail == ChangeDetailLevels.ENABLED) {
            if (mChangeNo > 0) {
                builder.append(mChangeNo).append("/detail/")
                        .append(ChangeEndpoints.CURRENT_PATCHSET_ARGS);
            }
            // Cannot request change detail without a change number.
            else return "";
        } else {
            builder.append("?q=");
            addSeperator = appendStatus(builder, false);
            try {
                appendSearchKeywords(builder, addSeperator);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        appendArgs(builder);
        return builder.toString();
    }

    // --- Parcelable stuff so we can send this object through intents ---
    public static final Parcelable.Creator<ChangeEndpoints> CREATOR
            = new Parcelable.Creator<ChangeEndpoints>() {
        public ChangeEndpoints createFromParcel(Parcel in) {
            return new ChangeEndpoints(in);
        }

        @Override
        public ChangeEndpoints[] newArray(int size) {
            return new ChangeEndpoints[0];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getLimit());
        dest.writeInt(mChangeNo);
        dest.writeString(mStatus);
        dest.writeInt(mRequestDetailedAccounts ? 1 : 0);
        dest.writeString(mSortkey);
        dest.writeString(mRequestChangeDetail.name());
        dest.writeInt(isAuthenticating() ? 1 : 0);

        int size;
        if (mSearchKeywords == null) size = 0;
        else size = mSearchKeywords.size();
        dest.writeInt(size);

        if (size > 0) {
            SearchKeyword[] keywords = new SearchKeyword[size];
            dest.writeTypedArray(mSearchKeywords.toArray(keywords), flags);
        }
    }

    public ChangeEndpoints(Parcel in) {
        setLimit(in.readInt());
        mChangeNo = in.readInt();
        mStatus = in.readString();
        mRequestDetailedAccounts = in.readInt() == 1;
        mSortkey = in.readString();
        mRequestChangeDetail = ChangeDetailLevels.valueOf(in.readString());
        setAuthenticating(in.readInt() == 1);

        int size = in.readInt();
        if (size > 0) {
            SearchKeyword[] keywords = new SearchKeyword[size];
            in.readTypedArray(keywords, SearchKeyword.CREATOR);
            mSearchKeywords = new HashSet<>(Arrays.asList(keywords));
        }
    }

}
