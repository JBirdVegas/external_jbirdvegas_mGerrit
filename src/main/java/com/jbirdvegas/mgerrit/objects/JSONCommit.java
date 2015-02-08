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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.tasks.Deserializers;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.List;
import java.util.TimeZone;

public class JSONCommit {

    // public
    public static final String KEY_STATUS_OPEN = "open";
    public static final String KEY_STATUS_MERGED = "merged";
    public static final String KEY_STATUS_ABANDONED = "abandoned";

    public static final String KEY_INSERTED = "lines_inserted";
    public static final String KEY_DELETED = "lines_deleted";
    public static final String KEY_STATUS = "status";
    public static final String KEY_ID = "id";
    private static final String KEY_WEBSITE = "website";
    public static final String KEY_ACCOUNT_ID = "_account_id";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_DATE = "date";
    public static final String KEY_NAME = "name";
    public static final String KEY_KIND = "kind";
    public static final String KEY_PROJECT = "project";
    private static final String KEY_TOPIC = "topic";
    private static final String KEY_OWNER = "owner";

    // internal
    private static final String KEY_BRANCH = "branch";
    private static final String KEY_CHANGE_ID = "change_id";
    public static final String KEY_SUBJECT = "subject";
    private static final String KEY_CREATED = "created";
    private static final String KEY_UPDATED = "updated";
    private static final String KEY_MERGEABLE = "mergeable";
    private static final String KEY_SORT_KEY = "_sortkey";
    private static final String KEY_COMMIT_NUMBER = "_number";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_CURRENT_REVISION = "current_revision";
    private static final String KEY_LABELS = "labels";
    private static final String KEY_VERIFIED = "Verified";
    private static final String KEY_CODE_REVIEW = "Code-Review";
    public static final String KEY_REVISIONS = "revisions";
    private static final String KEY_TIMEZONE = "tz";
    private static final String KEY_MORE_CHANGES = "_more_changes";

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
        },
        DRAFT {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explanation_draft);
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
            switch (status_lower) {
                case KEY_STATUS_OPEN:
                case "new":
                    return NEW;
                case KEY_STATUS_MERGED:
                    return MERGED;
                case KEY_STATUS_ABANDONED:
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

        mWebAddress = String.format("%s%d/", Prefs.getCurrentGerrit(context), mCommitNumber);
    }

    public static JSONCommit getInstance(JSONObject object, Context context) {
        JSONCommit thisCommit = gson.fromJson(object.toString(), JSONCommit.class);
        thisCommit.mServerTimeZone = Prefs.getServerTimeZone(context);
        thisCommit.mLocalTimeZone = Prefs.getLocalTimeZone(context);
        thisCommit.mWebAddress = String.format("%s%d/",
                Prefs.getCurrentGerrit(context),
                thisCommit.mCommitNumber);

        // Set draft notices if these fields are empty
        thisCommit.mPatchSet.setMessage(context);
        return thisCommit;
    }

    /**
     * gerritcodereview#change
     */
    @SerializedName(JSONCommit.KEY_KIND)
    private String mKind;

    /**
     * The ling-form ID of the change
     */
    @SerializedName(JSONCommit.KEY_ID)
    private String mId;

    /**
     * The name of the project. *
     */
    @SerializedName(JSONCommit.KEY_PROJECT)
    private String mProject;

    /**
     * The name of the target branch.
     * The refs/heads/ prefix is omitted. *
     */
    @SerializedName(JSONCommit.KEY_BRANCH)
    private String mBranch;

    /**
     * The topic to which this change belongs. (optional)
     */
    @SerializedName(JSONCommit.KEY_TOPIC)
    private String mTopic;

    /**
     * The Change-Id of the change.
     */
    @SerializedName(JSONCommit.KEY_CHANGE_ID)
    private String mChangeId;

    /**
     * The subject of the change (header line of the commit message).
     */
    @SerializedName(JSONCommit.KEY_SUBJECT)
    private String mSubject;

    /**
     * The status of the change (NEW, SUBMITTED, MERGED, ABANDONED, DRAFT).
     */
    @SerializedName(JSONCommit.KEY_STATUS)
    private Status mStatus;

    /**
     * The timestamp of when the change was created.
     */
    @SerializedName(JSONCommit.KEY_CREATED)
    private String mCreatedDate;

    /**
     * The timestamp of when the change was last updated.
     */
    @SerializedName(JSONCommit.KEY_UPDATED)
    private String mLastUpdatedDate;

    /**
     * Whether the change is mergeable.
     * Not set for merged changes.
     */
    @SerializedName(JSONCommit.KEY_MERGEABLE)
    private boolean mIsMergeable = false;

    /**
     * The sortkey of the change. Used internally for pagination and syncing
     */
    @SerializedName(JSONCommit.KEY_SORT_KEY)
    private String mSortKey;

    /**
     * The legacy numeric ID of the change.
     */
    @SerializedName(JSONCommit.KEY_COMMIT_NUMBER)
    private int mCommitNumber;

    /**
     * The commit ID of the current patch set of this change.
     */
    @SerializedName(JSONCommit.KEY_CURRENT_REVISION)
    private String mCurrentRevision;

    /**
     * The owner of the change
     */
    @SerializedName(JSONCommit.KEY_OWNER)
    private CommitterObject mOwnerObject;

    /**
     * Auto-generated field comprising of the Gerrit instance and the commit number
     */
    @SerializedName(JSONCommit.KEY_WEBSITE)
    private String mWebAddress;

    @SerializedName(JSONCommit.KEY_LABELS)
    private ReviewerList mReviewers;

    private CommitInfo mPatchSet;

    private int mPatchSetNumber = -1;

    /**
     * The messages associated with the change
     */
    @SerializedName(JSONCommit.KEY_MESSAGES)
    private List<CommitComment> mMessagesList;

    /**
     * The messages associated with the change
     */
    @SerializedName(JSONCommit.KEY_MORE_CHANGES)
    private boolean mMoreChanges = true;


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
     * <p/>
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
     * example: Jun 09, 2013 07:47 40ms PM
     */
    public String getLastUpdatedDate(Context context) {
        return Tools.prettyPrintDateTime(context, mLastUpdatedDate, mServerTimeZone, mLocalTimeZone);
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

    public String getMessage() {
        return mPatchSet.getMessage();
    }

    public String getWebAddress() {
        return mWebAddress;
    }

    @Nullable
    public List<Reviewer> getReviewers() {
        if (mReviewers == null) return null;
        return mReviewers.getReviewers();
    }

    public void setReviewers(ReviewerList reviewerlist) {
        this.mReviewers = reviewerlist;
    }

    public CommitterObject getOwnerObject() {
        return mOwnerObject;
    }

    public String getTopic() {
        return mTopic;
    }

    public void setCurrentRevision(String currentRevision) {
        this.mCurrentRevision = currentRevision;
    }

    public CommitInfo getPatchSet() {
        return mPatchSet;
    }

    public void setPatchSet(CommitInfo patchSet) {
        this.mPatchSet = patchSet;
    }

    // Note that this value can only be false for the first or last change returned from querying the server
    public boolean areMoreChanges() {
        return mMoreChanges;
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
                ", mWebAddress='" + mWebAddress + '\'' +
                ", mReviewers=" + mReviewers +
                ", mPatchSet=" + mPatchSet +
                ", mPatchSetNumber=" + mPatchSetNumber +
                ", mMessagesList=" + mMessagesList +
                ", mMoreChanges=" + mMoreChanges +
                '}';
    }
}
