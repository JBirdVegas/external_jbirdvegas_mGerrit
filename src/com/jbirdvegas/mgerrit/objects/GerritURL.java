package com.jbirdvegas.mgerrit.objects;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.jbirdvegas.mgerrit.Prefs;

import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * A class that helps to deconstruct Gerrit queries and assemble them
 *  when necessary. This allows for setting individual parts of a query
 *  without knowing other query parameters.
 */
public class GerritURL implements Parcelable
{
    private static Context sContext;
    private static String sProject = "";
    private String mStatus = "";
    private String mEmail = "";
    private String mCommitterState = "";
    private boolean mRequestDetailedAccounts = false;
    private String mSortkey = "";
    private String mChangeID = "";
    private int mChangeNo = 0;

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


    // Default constructor to facilitate instantiation
    public GerritURL() { }

    public static void setContext(Context context) {
        GerritURL.sContext = context;
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

    public void setChangeID(String changeID) {
        if (changeID == null) changeID = "";
        mChangeID = changeID;
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
    @Nullable
    public String toString()
    {
        boolean addSeperator = false;

        StringBuilder builder = new StringBuilder(0).append(Prefs.getCurrentGerrit(sContext));
        builder.append("changes/");

        if (mRequestChangeDetail == ChangeDetailLevels.ENABLED) {
            if (mChangeNo > 0) {
                builder.append(mChangeNo).append("/detail/")
                        .append(GerritURL.CURRENT_PATCHSET_ARGS);
            }
            // Cannot request change detail without a change number.
            else return "";
        } else {
            builder.append("?q=");
            addSeperator = appendChangeID(builder, addSeperator);
        }

        addSeperator = appendStatus(builder, addSeperator);
        addSeperator = appendOwner(builder, addSeperator);
        appendProject(builder, addSeperator);
        appendArgs(builder);
        return builder.toString();
    }

    public String getStatus() {
        return mStatus;
    }

    @Nullable
    public String getQuery() {
        if (mStatus == null) return null;
        else {
            return JSONCommit.KEY_STATUS + ":" + mStatus;
        }
    }

    public boolean equals(String str) {
        return this.toString().equals(str);
    }

    private boolean appendChangeID(StringBuilder builder, boolean addSeperator) {
        if (addSeperator) builder.append('+');
        if (mChangeID != null && !mChangeID.isEmpty()) {
            builder.append(mChangeID);
            return true;
        }
        return false;
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

    private boolean appendOwner(StringBuilder builder, boolean addSeperator) {
        if (mCommitterState != null && !mCommitterState.isEmpty() &&
                mEmail != null && !mEmail.isEmpty()) {
            if (addSeperator) builder.append('+');
            builder.append(mCommitterState)
                    .append(':')
                    .append(mEmail);
            return true;
        }
        return false;
    }

    private boolean appendProject(StringBuilder builder, boolean addSeperator) {
        if (sProject != null && !sProject.isEmpty()) {
            if (addSeperator) builder.append('+');
            try {
                builder.append(JSONCommit.KEY_PROJECT)
                        .append(":")
                        .append(URLEncoder.encode(sProject, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private void appendArgs(StringBuilder builder) {
        if (mSortkey != null && !mSortkey.isEmpty()) {
            builder.append("&P=").append(mSortkey);
        }
        if (mRequestChangeDetail == ChangeDetailLevels.LEGACY) {
            builder.append(GerritURL.OLD_CHANGE_DETAIL_ARGS);
        }
        if (mRequestDetailedAccounts) {
            builder.append(GerritURL.DETAILED_ACCOUNTS_ARG);
        }
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
        dest.writeString(sProject);
        dest.writeString(mChangeID);
        dest.writeInt(mChangeNo);
        dest.writeString(mStatus);
        dest.writeString(mEmail);
        dest.writeString(mCommitterState);
        dest.writeInt(mRequestDetailedAccounts ? 1 : 0);
        dest.writeString(mSortkey);
        dest.writeString(mRequestChangeDetail.name());
    }

    public GerritURL(Parcel in) {
        sProject = in.readString();
        mChangeID = in.readString();
        mChangeNo = in.readInt();
        mStatus = in.readString();
        mEmail = in.readString();
        mCommitterState = in.readString();
        mRequestDetailedAccounts = in.readInt() == 1;
        mSortkey = in.readString();
        mRequestChangeDetail = ChangeDetailLevels.valueOf(in.readString());
    }
}
