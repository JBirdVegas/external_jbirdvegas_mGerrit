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

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.tasks.Deserializers;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class JSONCommit implements Parcelable {
    private static final String TAG = JSONCommit.class.getSimpleName();

    // public
    public static final String KEY_STATUS_OPEN = "open";
    public static final String KEY_STATUS_MERGED = "merged";
    public static final String KEY_STATUS_ABANDONED = "abandoned";
    public static final String DETAILED_ACCOUNTS_ARG = "&o=DETAILED_ACCOUNTS";
    // used to query commit message
    public static final String CURRENT_PATCHSET_ARGS = new StringBuilder(0)
            .append("&o=CURRENT_REVISION")
            .append("&o=CURRENT_COMMIT")
            .append("&o=CURRENT_FILES")
            .append("&o=DETAILED_LABELS")
            .append(DETAILED_ACCOUNTS_ARG)
            .append("&o=MESSAGES")
            .toString();
    public static final String KEY_INSERTED = "lines_inserted";
    public static final String KEY_DELETED = "lines_deleted";
    public static final String KEY_STATUS = "status";
    public static final String KEY_ID = "id";
    public static final String KEY_WEBSITE = "website";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_ACCOUNT_ID = "_account_id";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_DATE = "date";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_NAME = "name";
    public static final String KEY_KIND = "kind";
    public static final String KEY_PROJECT = "project";
    private static final String KEY_TOPIC = "topic";
    private static final String KEY_OWNER = "owner";
    public static final String KEY_COMMITTER = "committer";

    // internal
    private static final String KEY_BRANCH = "branch";
    private static final String KEY_CHANGE_ID = "change_id";
    public static final String KEY_SUBJECT = "subject";
    private static final String KEY_CREATED = "created";
    private static final String KEY_UPDATED = "updated";
    private static final String KEY_MERGEABLE = "mergeable";
    private static final String KEY_SORT_KEY = "_sortkey";
    public static final String KEY_COMMIT_NUMBER = "_number";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_CURRENT_REVISION = "current_revision";
    public static final String KEY_CHANGED_FILES = "files";
    private static final String KEY_LABELS = "labels";
    private static final String KEY_VERIFIED = "Verified";
    private static final String KEY_CODE_REVIEW = "Code-Review";
    public static final String KEY_REVISIONS = "revisions";
    public static final String KEY_COMMIT = "commit";
    private static final String KEY_TIMEZONE = "tz";
    private static final String GERRIT_DATE_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS";
    private static final String HUMAN_READABLE_DATE_FORMAT = "MMMM dd, yyyy '%s' hh:mm:ss aa";

    private TimeZone mServerTimeZone;
    private TimeZone mLocalTimeZone;

    private static Gson gson;
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Deserializers.addDeserializers(gsonBuilder);
        gson = gsonBuilder.create();
    }

    public enum Status {
        NEW {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explanation_new);
            }
        },
        SUBMITTED {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explanation_submitted);
            }
        },
        MERGED {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explanation_merged);
            }
        },
        ABANDONED {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explanation_abandoned);
            }
        };

        /**
         * Returns an explanation of the status users can understand
         *
         * @param c Context of caller to access resources
         * @return human/user readable status explanation
         */
        public String getExplaination(Context c) {
            return null;
        }

        public static Status getStatusFromString(final String status) {
            String status_lower = status.toLowerCase();
            if (status_lower.equals(KEY_STATUS_OPEN.toLowerCase()) || status_lower.equals("new")) {
                return NEW;
            } else if (status_lower.equals(KEY_STATUS_MERGED.toLowerCase())) {
                return MERGED;
            } else if (status_lower.equals(KEY_STATUS_ABANDONED)) {
                return ABANDONED;
            }
            return SUBMITTED;
        }

        // Convert the status to a Status enum instance and back again
        public static String getStatusString(final String status) {
            return getStatusFromString(status).toString();
        }
    }

    public JSONCommit(Context context, String changeid, int commitNumber, String project,
                      String subject, CommitterObject committer, String updated, String status) {

        mServerTimeZone = Prefs.getServerTimeZone(context);
        mLocalTimeZone = Prefs.getLocalTimeZone(context);

        mChangeId = changeid;
        mCommitNumber = commitNumber;
        mProject = project;
        mSubject = subject;
        mOwnerObject = committer;
        mLastUpdatedDate = updated;
        mStatus = Status.valueOf(status);

        mWebAddress = String.format("%s#/c/%d/", Prefs.getCurrentGerrit(context), mCommitNumber);
    }

    public static JSONCommit getInstance(JSONObject object, Context context) {
        JSONCommit thisCommit = gson.fromJson(object.toString(), JSONCommit.class);
        thisCommit.mServerTimeZone = Prefs.getServerTimeZone(context);
        thisCommit.mLocalTimeZone = Prefs.getLocalTimeZone(context);
        thisCommit.mWebAddress = String.format("%s#/c/%d/",
                Prefs.getCurrentGerrit(context),
                thisCommit.mCommitNumber);

        // Set draft notices if these fields are empty
        thisCommit.mPatchSet.setMessage(context);
        if (thisCommit.mFileInfos == null) {
            thisCommit.mFileInfos = FileInfoList.setDraftNotice(context);
        }
        return thisCommit;
    }

    @SerializedName(JSONCommit.KEY_KIND)
    private String mKind;

    @SerializedName(JSONCommit.KEY_ID)
    private String mId;

    @SerializedName(JSONCommit.KEY_PROJECT)
    private String mProject;

    @SerializedName(JSONCommit.KEY_BRANCH)
    private String mBranch;

    @SerializedName(JSONCommit.KEY_TOPIC)
    private String mTopic;

    @SerializedName(JSONCommit.KEY_CHANGE_ID)
    private String mChangeId;

    @SerializedName(JSONCommit.KEY_SUBJECT)
    private String mSubject;

    @SerializedName(JSONCommit.KEY_STATUS)
    private Status mStatus;

    @SerializedName(JSONCommit.KEY_CREATED)
    private String mCreatedDate;

    @SerializedName(JSONCommit.KEY_UPDATED)
    private String mLastUpdatedDate;

    @SerializedName(JSONCommit.KEY_MERGEABLE)
    private boolean mIsMergeable = false;

    @SerializedName(JSONCommit.KEY_SORT_KEY)
    private String mSortKey;

    @SerializedName(JSONCommit.KEY_COMMIT_NUMBER)
    private int mCommitNumber;

    @SerializedName(JSONCommit.KEY_CURRENT_REVISION)
    private String mCurrentRevision;

    @SerializedName(JSONCommit.KEY_OWNER)
    private CommitterObject mOwnerObject;

    @SerializedName(JSONCommit.KEY_CHANGED_FILES)
    private FileInfoList mFileInfos;

    @SerializedName(JSONCommit.KEY_WEBSITE)
    private String mWebAddress;

    @SerializedName(JSONCommit.KEY_LABELS)
    private ReviewerList mReviewers;

    private CommitInfo mPatchSet;

    private int mPatchSetNumber = -1;

    @SerializedName(JSONCommit.KEY_MESSAGES)
    private List<CommitComment> mMessagesList;


    public String getKind() {
        return mKind;
    }

    public String getId() {
        return mId;
    }

    public String getProject() {
        return mProject;
    }

    public String getBranch() {
        return mBranch;
    }

    public String getChangeId() {
        return mChangeId;
    }

    public String getSubject() {
        return mSubject;
    }

    public Status getStatus() {
        return mStatus;
    }

    public String getCreatedDate() {
        return mCreatedDate;
    }

    public String getLastUpdatedDate() {
        return mLastUpdatedDate;
    }

    public List<CommitComment> getMessagesList() {
        return mMessagesList;
    }

    /**
     * PrettyPrint the Gerrit provided timestamp
     * format into a more human readable format
     *
     * I have no clue what the ten zeros
     * after the seconds are good for as
     * the exact same ten zeros are in all
     * databases regardless of the stamp's
     * time or timezone... it comes from
     * Google so we just handle oddities
     * downstream :(
     * from "2013-06-09 19:47:40.000000000"
     * to Jun 09, 2013 07:47 40ms PM
     *
     * @return String representation of the date
     *         example: Jun 09, 2013 07:47 40ms PM
     */
    @SuppressWarnings("SimpleDateFormatWithoutLocale")
    public String getLastUpdatedDate(Context context) {
        try {
            SimpleDateFormat currentDateFormat
                    = new SimpleDateFormat(GERRIT_DATE_FORMAT, Locale.US);
            DateFormat humanDateFormat = new SimpleDateFormat(
                    String.format(HUMAN_READABLE_DATE_FORMAT,
                            context.getString(R.string.at)),
                    Locale.getDefault());
            // location of server
            currentDateFormat.setTimeZone(mServerTimeZone);
            // local location
            humanDateFormat.setTimeZone(mLocalTimeZone);
            Log.d(TAG, String.format("Local timezone: %s | Server timezone: %s",
                    mLocalTimeZone.getDisplayName(),
                    mServerTimeZone.getDisplayName()));
            return humanDateFormat.format(currentDateFormat.parse(mLastUpdatedDate));
        } catch (ParseException e) {
            e.printStackTrace();
            return mLastUpdatedDate;
        }
    }

    public boolean isIsMergeable() {
        return mIsMergeable;
    }

    public String getSortKey() {
        return mSortKey;
    }

    public int getCommitNumber() {
        return mCommitNumber;
    }

    public String getCurrentRevision() {
        return mCurrentRevision;
    }

    public CommitterObject getCommitterObject() {
        return mPatchSet.getCommitterObject();
    }

    public String getMessage() {
        return mPatchSet.getMessage();
    }

    public List<FileInfo> getChangedFiles() {
        return mFileInfos.getFiles();
    }

    public String getWebAddress() {
        return mWebAddress;
    }

    public Reviewer[] getReviewers() {
        return mReviewers.getReviewers();
    }

    public void setReviewers(ReviewerList reviewerlist) {
        this.mReviewers = reviewerlist;
    }

    public List<Reviewer> getVerifiedReviewers() {
        ArrayList<Reviewer> rs = new ArrayList<>();
        for (Reviewer r : mReviewers.getReviewers()) {
            if (KEY_VERIFIED.equals(r.getLabel())) {
                rs.add(r);
            }
        }
        return rs;
    }

    public ArrayList<Reviewer> getCodeReviewers() {
        ArrayList<Reviewer> rs = new ArrayList<>();
        for (Reviewer r : mReviewers.getReviewers()) {
            if (KEY_CODE_REVIEW.equals(r.getLabel())) {
                rs.add(r);
            }
        }
        return rs;
    }

    public CommitterObject getAuthorObject() {
        return mPatchSet.getAuthorObject();
    }

    public CommitterObject getOwnerObject() {
        return mOwnerObject;
    }

    public int getPatchSetNumber() {
        return mPatchSetNumber;
    }

    public String getTopic() {
        return mTopic;
    }

    public void setStatus(String status) {
        mStatus = Status.valueOf(status);
    }

    public void setCurrentRevision(String currentRevision) {
        this.mCurrentRevision = currentRevision;
    }

    public void setPatchSet(CommitInfo patchSet) {
        this.mPatchSet = patchSet;
    }

    public void setChangedFiles(FileInfoList fileInfos) {
        this.mFileInfos = fileInfos;
    }

    public void setPatchSetNumber(int patchSetNumber) {
        this.mPatchSetNumber = patchSetNumber;
    }

    // Parcelable implementation
    public JSONCommit(Parcel parcel) {
        mKind = parcel.readString();
        mId = parcel.readString();
        mProject = parcel.readString();
        mBranch = parcel.readString();
        mChangeId = parcel.readString();
        mSubject = parcel.readString();
        mStatus = Status.valueOf(parcel.readString());
        mCreatedDate = parcel.readString();
        mLastUpdatedDate = parcel.readString();
        mIsMergeable = parcel.readByte() == 1;
        mSortKey = parcel.readString();
        mCommitNumber = parcel.readInt();
        mCurrentRevision = parcel.readString();
        mOwnerObject = parcel.readParcelable(CommitterObject.class.getClassLoader());
        mPatchSet = parcel.readParcelable(CommitInfo.class.getClassLoader());
        mFileInfos = parcel.readParcelable(FileInfoList.class.getClassLoader());
        mWebAddress = parcel.readString();
        mReviewers = parcel.readParcelable(ReviewerList.class.getClassLoader());
        mPatchSetNumber = parcel.readInt();
        mMessagesList = parcel.readArrayList(CommitComment.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mKind);
        parcel.writeString(mId);
        parcel.writeString(mProject);
        parcel.writeString(mBranch);
        parcel.writeString(mChangeId);
        parcel.writeString(mSubject);
        parcel.writeString(mStatus.name());
        parcel.writeString(mCreatedDate);
        parcel.writeString(mLastUpdatedDate);
        parcel.writeByte((byte) (mIsMergeable ? 1 : 0));
        parcel.writeString(mSortKey);
        parcel.writeInt(mCommitNumber);
        parcel.writeString(mCurrentRevision);
        parcel.writeParcelable(mOwnerObject, 0);
        parcel.writeParcelable(mPatchSet, 0);
        parcel.writeParcelable(mFileInfos, 0);
        parcel.writeString(mWebAddress);
        parcel.writeParcelable(mReviewers, 0);
        parcel.writeInt(mPatchSetNumber);
        parcel.writeTypedList(mMessagesList);
    }

    @Override
    public String toString() {
        return "JSONCommit{" +
                "mServerTimeZone=" + mServerTimeZone +
                ", mLocalTimeZone=" + mLocalTimeZone +
                ", mKind='" + mKind + '\'' +
                ", mId='" + mId + '\'' +
                ", mProject='" + mProject + '\'' +
                ", mBranch='" + mBranch + '\'' +
                ", mTopic='" + mTopic + '\'' +
                ", mChangeId='" + mChangeId + '\'' +
                ", mSubject='" + mSubject + '\'' +
                ", mStatus=" + mStatus +
                ", mCreatedDate='" + mCreatedDate + '\'' +
                ", mLastUpdatedDate='" + mLastUpdatedDate + '\'' +
                ", mIsMergeable=" + mIsMergeable +
                ", mSortKey='" + mSortKey + '\'' +
                ", mCommitNumber=" + mCommitNumber +
                ", mCurrentRevision='" + mCurrentRevision + '\'' +
                ", mOwnerObject=" + mOwnerObject +
                ", mFileInfos=" + mFileInfos +
                ", mWebAddress='" + mWebAddress + '\'' +
                ", mReviewers=" + mReviewers +
                ", mPatchSet=" + mPatchSet +
                ", mPatchSetNumber=" + mPatchSetNumber +
                ", mMessagesList=" + mMessagesList +
                '}';
    }
}
