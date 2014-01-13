package com.jbirdvegas.mgerrit.objects;

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
    private boolean mRequestChangeDetail = false;
    private String mChangeID = "";

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

    public void setChangeID(String changeID) {
        if (changeID == null) changeID = "";
        mChangeID = changeID;
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

    public void requestChangeDetail(boolean request) {
        mRequestChangeDetail = request;
        if (request) {
            mRequestDetailedAccounts = false;
        }
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
            return sGerritBase + "projects/?d";
        }

        StringBuilder builder = new StringBuilder(0)
            .append(sGerritBase)
            .append(StaticWebAddress.getQuery());

        if (mChangeID != null && !mChangeID.isEmpty())
        {
            builder.append(mChangeID);
            addPlus = true;
        }

        if (mStatus != null && !mStatus.isEmpty())
        {
            builder.append(JSONCommit.KEY_STATUS)
                    .append(":")
                    .append(mStatus);
            addPlus = true;
        }

        if (mCommitterState != null && !mCommitterState.isEmpty() && mEmail != null && !mEmail.isEmpty())
        {
            if (addPlus) builder.append('+');
            builder.append(mCommitterState)
                .append(':')
                .append(mEmail);
            addPlus = true;
        }

        try {
            if (sProject != null && !sProject.isEmpty())
            {
                if (addPlus) builder.append('+');
                builder.append(JSONCommit.KEY_PROJECT)
                    .append(":")
                    .append(URLEncoder.encode(sProject, "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (mSortkey != null && !mSortkey.isEmpty()) {
            builder.append("&P=").append(mSortkey);
        }

        if (mRequestChangeDetail) {
            builder.append(JSONCommit.CURRENT_PATCHSET_ARGS);
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
        if (mStatus == null) return null;
        else {
            return JSONCommit.KEY_STATUS + ":" + mStatus;
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
        dest.writeString(mChangeID);
        dest.writeString(mStatus);
        dest.writeString(mEmail);
        dest.writeString(mCommitterState);
        dest.writeInt(mListProjects ? 1 : 0);
        dest.writeInt(mRequestDetailedAccounts ? 1 : 0);
        dest.writeString(mSortkey);
        dest.writeInt(mRequestChangeDetail ? 1 : 0);
    }

    public GerritURL(Parcel in) {
        sGerritBase = in.readString();
        sProject = in.readString();
        mChangeID = in.readString();
        mStatus = in.readString();
        mEmail = in.readString();
        mCommitterState = in.readString();
        mListProjects = in.readInt() == 1;
        mRequestDetailedAccounts = in.readInt() == 1;
        mSortkey = in.readString();
        mRequestChangeDetail = in.readInt() == 1;
    }
}
