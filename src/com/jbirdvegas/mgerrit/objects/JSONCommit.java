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

import com.google.gson.annotations.SerializedName;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
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
    private static final String KEY_COMMITTER = "committer";

    // internal
    private static final String KEY_BRANCH = "branch";
    private static final String KEY_CHANGE_ID = "change_id";
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_CREATED = "created";
    private static final String KEY_UPDATED = "updated";
    private static final String KEY_MERGEABLE = "mergeable";
    private static final String KEY_SORT_KEY = "_sortkey";
    private static final String KEY_COMMIT_NUMBER = "_number";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_CURRENT_REVISION = "current_revision";
    private static final String KEY_CHANGED_FILES = "files";
    private static final String KEY_LABELS = "labels";
    private static final String KEY_VERIFIED = "Verified";
    private static final String KEY_CODE_REVIEW = "Code-Review";
    private static final String KEY_ALL = "all";
    private static final String KEY_VALUE = "value";
    private static final String KEY_REVISIONS = "revisions";
    private static final String KEY_COMMIT = "commit";
    private static final String KEY_TIMEZONE = "tz";
    private static final boolean DEBUG = false;
    private static final String GERRIT_DATE_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS";
    private static final String HUMAN_READABLE_DATE_FORMAT = "MMMM dd, yyyy '%s' hh:mm:ss aa";

    private TimeZone mServerTimeZone;
    private TimeZone mLocalTimeZone;

    public List<CommitComment> getMessagesList() {
        return mMessagesList;
    }

    public enum Status {
        NEW {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explaination_new);
            }
        },
        SUBMITTED {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explaination_submitted);
            }
        },
        MERGED {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explaination_merged);
            }
        },
        ABANDONED {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explaination_abandoned);
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
            if (status_lower.equals(KEY_STATUS_OPEN.toLowerCase()) || status.equals("new")) {
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

    /**
     * Default constructor holds a single commit represented by
     * a json formatted response
     *
     * @param object JSONObject sent by mgerrit in response to a query
     */
    @SuppressWarnings("NestedTryStatement")
    public JSONCommit(JSONObject object, Context context) {
        mServerTimeZone = Prefs.getServerTimeZone(context);
        mLocalTimeZone = Prefs.getLocalTimeZone(context);
        try {
            mKind = object.getString(KEY_KIND);
            mId = object.getString(KEY_ID);
            mProject = object.getString(KEY_PROJECT);
            mBranch = object.getString(KEY_BRANCH);
            mChangeId = object.getString(KEY_CHANGE_ID);
            mSubject = object.getString(KEY_SUBJECT);
            mStatus = Status.valueOf(object.getString(KEY_STATUS));
            mCreatedDate = object.getString(KEY_CREATED);
            mLastUpdatedDate = object.getString(KEY_UPDATED);
            try {
                mIsMergeable = object.getBoolean(KEY_MERGEABLE);
            } catch (JSONException ignored) {
                // object is either Abandoned or Merged
                // ignore and move on
                mIsMergeable = false;
            }
            mSortKey = object.getString(KEY_SORT_KEY);
            mCommitNumber = object.getInt(KEY_COMMIT_NUMBER);
            mOwnerObject = CommitterObject.getInstance(object.getJSONObject(KEY_OWNER));
            mWebAddress = String.format("%s#/c/%d/",
                    Prefs.getCurrentGerrit(context),
                    mCommitNumber);

            // TODO: labels are not available >2.6
            try {
                // code review labels
                // v2.5 labels only include the expected values -2, -1, 0, 1, 2
                // v2.6 labels include verifiers and code reviewers
                //      with their associated values
                JSONObject labels = object.getJSONObject(KEY_LABELS);
                mVerifiedReviewers = getReviewers(
                        labels.getJSONObject(KEY_VERIFIED).getJSONArray(KEY_ALL));
                mCodeReviewers = getReviewers(
                        labels.getJSONObject(KEY_CODE_REVIEW).getJSONArray(KEY_ALL));
            } catch (JSONException je) {
                if (DEBUG)
                    Log.e(TAG, "Failed to get reviewer labels", je);
            }

            /*
            handle messages that may not exist

            If we throw JSONException here then we did not
            directly query the patchset and are only showing
            the commit list. Don't bother trying to load the
            rest as they are dependant on JSONObject "current_revision"
            which does not exist past here

             **
             *  There is one other circumstance where the patchset
             * was once public but the current revision of the patch
             * set is a draft (we do not have authenticated actions
             * permission) and therefor the information about the
             * current revision is not public and hidden.
             *
             * This causes almost all fields to be null. This is a rare
             * case but needs to be addressed in the catch block here
             **
            */
            // string displayed instead of blank information we don't have
            String draftNotice = context.getString(R.string.current_revision_is_draft_message);
            try {
                try {
                    mMessagesList = makeMessagesList(object);
                } catch(JSONException je) {
                    if (DEBUG)
                        Log.d(TAG, "could not find messages!", je);
                }
                // we did not directly query the patch set
                try {
                    mCurrentRevision = object.getString(KEY_CURRENT_REVISION);
                } catch (JSONException je) {
                    if (DEBUG)
                        Log.d(TAG, "current_revision was a fail lets try looking for revision",
                                je);
                    mCurrentRevision = object.getString(KEY_REVISIONS);
                }

                try {
                    mMessage = getMessageFromJSON(object, mCurrentRevision);
                } catch (JSONException je) {
                    if (DEBUG) {
                        Log.e(TAG, "Failed to get message from commit", je);
                    }
                    mMessage = draftNotice;
                }

                try {
                    mChangedFiles = getChangedFilesSet(object, mCurrentRevision);
                } catch (JSONException je) {
                    if (DEBUG) {
                        Log.e(TAG, "Failed to get changed files list", je);
                    }
                    mChangedFiles = new ArrayList<ChangedFile>(0);
                    mChangedFiles.add(new ChangedFile(draftNotice));
                }

                try {
                    mAuthorObject = getCommitter(mCurrentRevision, KEY_AUTHOR, object);
                    mCommitterObject = getCommitter(mCurrentRevision, KEY_COMMITTER, object);
                } catch (JSONException je) {
                    if (DEBUG) {
                        Log.e(TAG, "Failed to get author/committer objects", je);
                    }
                }

                mPatchSetNumber = getPatchSetNumberInternal(object, mCurrentRevision);
            } catch (JSONException ignored) {
                /* TODO: No code nested in this try block is outside of its own nested try block
                 *  therefore, this catch block should never get executed.
                 */
                mPatchSetNumber = -1;
                String unknown = context.getString(R.string.unknown);
                mAuthorObject = CommitterObject.getInstance(unknown, unknown, null, null);
                mCommitterObject = CommitterObject.getInstance(unknown, unknown, null, null);
            }
        } catch (JSONException e) {
            if (DEBUG) {
                Log.e(TAG, "Failed to parse JSONObject into useful data", e);
            }
        }
    }

    private List<CommitComment> makeMessagesList(JSONObject object) throws JSONException {
        LinkedList<CommitComment> linkedList = new LinkedList<CommitComment>();
        JSONArray messagesArray = object.getJSONArray(KEY_MESSAGES);
        for (int i = 0; messagesArray.length() > i; i++) {
            linkedList.add(CommitComment.getInstance(messagesArray.getJSONObject(i)));
        }
        return linkedList.isEmpty() ? new LinkedList<CommitComment>() : linkedList;
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

    @SerializedName(JSONCommit.KEY_AUTHOR)
    private CommitterObject mAuthorObject;

    @SerializedName(JSONCommit.KEY_COMMITTER)
    private CommitterObject mCommitterObject;

    @SerializedName(JSONCommit.KEY_MESSAGE)
    private String mMessage;

    @SerializedName(JSONCommit.KEY_CHANGED_FILES)
    private List<ChangedFile> mChangedFiles;

    @SerializedName(JSONCommit.KEY_WEBSITE)
    private String mWebAddress;

    @SerializedName(JSONCommit.KEY_VERIFIED)
    private List<Reviewer> mVerifiedReviewers;

    @SerializedName(JSONCommit.KEY_CODE_REVIEW)
    private List<Reviewer> mCodeReviewers;

    // Not serialised
    private int mPatchSetNumber;

    @SerializedName(JSONCommit.KEY_MESSAGES)
    private List<CommitComment> mMessagesList;

    private CommitterObject getCommitter(String currentRevision,
                                         String authorOrCommitter,
                                         JSONObject mainObject)
            throws JSONException {
        if (DEBUG) {
            Log.v(TAG, "JSONObject we check for: " + mainObject);
        }
        JSONObject allRevisions = mainObject.getJSONObject(KEY_REVISIONS);
        JSONObject revisionObject = allRevisions.getJSONObject(currentRevision);
        JSONObject commitObject = revisionObject.getJSONObject(KEY_COMMIT);
        JSONObject authorObject = commitObject.getJSONObject(authorOrCommitter);
        return CommitterObject.getInstance(authorObject.getString(KEY_NAME),
                authorObject.getString(KEY_EMAIL),
                authorObject.getString(KEY_DATE),
                authorObject.getString(KEY_TIMEZONE));
    }

    private String getMessageFromJSON(JSONObject mainObject,
                                      String currentRevision)
            throws JSONException {
        JSONObject allRevisions = mainObject.getJSONObject(KEY_REVISIONS);
        JSONObject revisionObject = allRevisions.getJSONObject(currentRevision);
        JSONObject commitObject = revisionObject.getJSONObject(KEY_COMMIT);
        return commitObject.getString(KEY_MESSAGE);
    }

    private int getPatchSetNumberInternal(JSONObject mainObject,
                                          String currentRevision)
            throws JSONException {
        JSONObject allRevisions = mainObject.getJSONObject(KEY_REVISIONS);
        JSONObject revisionObject = allRevisions.getJSONObject(currentRevision);
        return revisionObject.getInt(KEY_COMMIT_NUMBER);
    }

    private List<ChangedFile> getChangedFilesSet(JSONObject mainObject,
                                                 String currentRevision)
            throws JSONException {
        JSONObject allRevisions = mainObject.getJSONObject(KEY_REVISIONS);
        JSONObject revisionObject = allRevisions.getJSONObject(currentRevision);
        JSONObject filesObject = revisionObject.getJSONObject(KEY_CHANGED_FILES);
        List<ChangedFile> list = new ArrayList<ChangedFile>(0);
        JSONArray keysArray = filesObject.names();

        /* If there are no files changed (i.e. a merge commit) then an empty
         * list should be returned. */
        if (keysArray == null) {
            return list;
        }

        for (int i = 0; keysArray.length() > i; i++) {
            try {
                String path = (String) keysArray.get(i);
                list.add(ChangedFile.parseFromJSONObject(path,
                        filesObject.getJSONObject(path)));
            } catch (JSONException e) {
                if (DEBUG) {
                    Log.e(TAG, "Failed to parse jsonObject", e);
                }
            }
        }
        return list;
    }

    private List<Reviewer> getReviewers(JSONArray jsonArray)
            throws JSONException {
        List<Reviewer> list = new ArrayList<Reviewer>(0);
        for (int i = 0; jsonArray.length() > i; i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            try {
                list.add(new Reviewer(object.getString(KEY_VALUE),
                        object.getString(KEY_NAME),
                        object.getString(KEY_EMAIL)));
            } catch (JSONException je) {
                list.add(new Reviewer(null,
                        object.getString(KEY_NAME),
                        object.getString(KEY_EMAIL)));
            }
            if (DEBUG) Log.v(TAG, "Found Reviewer: " + list.get(i).toString());
        }
        return list;
    }

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
        return mCommitterObject;
    }

    public String getMessage() {
        return mMessage;
    }

    public List<ChangedFile> getChangedFiles() {
        return mChangedFiles;
    }

    public String getWebAddress() {
        return mWebAddress;
    }

    public List<Reviewer> getVerifiedReviewers() {
        return mVerifiedReviewers;
    }

    public ArrayList<Reviewer> getCodeReviewers() {
        return (ArrayList<Reviewer>) mCodeReviewers;
    }

    public CommitterObject getAuthorObject() {
        return mAuthorObject;
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
        mAuthorObject = parcel.readParcelable(CommitterObject.class.getClassLoader());
        mCommitterObject = parcel.readParcelable(CommitterObject.class.getClassLoader());
        mMessage = parcel.readString();
        mChangedFiles = parcel.readArrayList(ChangedFile.class.getClassLoader());
        mWebAddress = parcel.readString();
        mVerifiedReviewers = parcel.readArrayList(ChangedFile.class.getClassLoader());
        mCodeReviewers = parcel.readArrayList(ChangedFile.class.getClassLoader());
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
        parcel.writeParcelable(mAuthorObject, 0);
        parcel.writeParcelable(mCommitterObject, 0);
        parcel.writeString(mMessage);
        parcel.writeTypedList(mChangedFiles);
        parcel.writeString(mWebAddress);
        parcel.writeTypedList(mVerifiedReviewers);
        parcel.writeTypedList(mCodeReviewers);
        parcel.writeInt(mPatchSetNumber);
        parcel.writeTypedList(mMessagesList);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("JSONCommit{");
        sb.append("mKind='").append(mKind).append('\'');
        sb.append(", mId='").append(mId).append('\'');
        sb.append(", mProject='").append(mProject).append('\'');
        sb.append(", mBranch='").append(mBranch).append('\'');
        sb.append(", mChangeId='").append(mChangeId).append('\'');
        sb.append(", mSubject='").append(mSubject).append('\'');
        sb.append(", mStatus=").append(mStatus);
        sb.append(", mCreatedDate='").append(mCreatedDate).append('\'');
        sb.append(", mLastUpdatedDate='").append(mLastUpdatedDate).append('\'');
        sb.append(", mIsMergeable=").append(mIsMergeable);
        sb.append(", mSortKey='").append(mSortKey).append('\'');
        sb.append(", mCommitNumber=").append(mCommitNumber);
        sb.append(", mCurrentRevision='").append(mCurrentRevision).append('\'');
        sb.append(", mOwnerObject=").append(mOwnerObject);
        sb.append(", mAuthorObject=").append(mAuthorObject);
        sb.append(", mCommitterObject=").append(mCommitterObject);
        sb.append(", mMessage='").append(mMessage).append('\'');
        sb.append(", mChangedFiles=").append(mChangedFiles);
        sb.append(", mWebAddress='").append(mWebAddress).append('\'');
        sb.append(", mVerifiedReviewers=").append(mVerifiedReviewers);
        sb.append(", mCodeReviewers=").append(mCodeReviewers);
        sb.append(", mPatchSetNumber=").append(mPatchSetNumber);
        sb.append(", mMessagesList=").append(mMessagesList);
        sb.append(", Topic='").append(mTopic).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
