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
import android.util.Log;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JSONCommit {
    private static final String TAG = JSONCommit.class.getSimpleName();

    // public
    public static final String KEY_FOOBAR = "foobar";
    public static final String KEY_STATUS_OPEN = "open";
    public static final String KEY_STATUS_MERGED = "merged";
    public static final String KEY_STATUS_ABANDONED = "abandoned";
    public static final String KEY_JSON_COMMIT = "json_commit";
    public static final String KEY_PATCHSET_IN_JSON = "patchset_json";
    // used to query commit message
    public static final String CURRENT_PATCHSET_ARGS = new StringBuilder(0)
            .append("&o=CURRENT_REVISION")
            .append("&o=CURRENT_COMMIT")
            .append("&o=CURRENT_FILES")
            .append("&o=DETAILED_LABELS")
            .append("&o=DETAILED_ACCOUNTS")
            //.append("&o=MESSAGES") soon :)
            .toString();
    public static final String KEY_INSERTED = "lines_inserted";
    public static final String KEY_DELETED = "lines_deleted";
    public static final String KEY_STATUS = "status";
    public static final String KEY_ID = "id";
    public static String KEY_WEBSITE = "website";

    // internal
    private static final String KEY_KIND = "kind";
    private static final String KEY_PROJECT = "project";
    private static final String KEY_BRANCH = "branch";
    private static final String KEY_CHANGE_ID = "change_id";
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_CREATED = "created";
    private static final String KEY_UPDATED = "updated";
    private static final String KEY_MERGEABLE = "mergeable";
    private static final String KEY_SORT_KEY = "_sortkey";
    private static final String KEY_COMMIT_NUMBER = "_number";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_NAME = "name";
    private static final String KEY_CURRENT_REVISION = "current_revision";
    private static final String KEY_AUTHOR = "author";
    private static final String KEY_COMMITTER = "committer";
    private static final String KEY_CHANGED_FILES = "files";
    private static final String KEY_LABELS = "labels";
    private static final String KEY_VERIFIED = "Verified";
    private static final String KEY_CODE_REVIEW = "Code-Review";
    private static final String KEY_RECOMMENDED = "recommended";
    private static final String KEY_DISLIKED = "disliked";
    private static final String KEY_ALL = "all";
    private static final String KEY_VALUE = "value";
    private static final String KEY_ACCOUNT_ID = "_account_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REVISIONS = "revisions";
    private static final String KEY_COMMIT = "commit";
    private static final String KEY_DATE = "date";
    private static final String KEY_TIMEZONE = "tz";

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
        public String getExplaination(Context c){
            return null;
        }

    }

    /**
     * Default constructor holds a single commit represented by
     * a json formatted response
     *
     * @param object JSONObject sent by mgerrit in response to a query
     */
    public JSONCommit(JSONObject object, Context context) {
        mRawJSONCommit = object;
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
            mOwnerName = getOwnerName(object.getJSONObject(KEY_OWNER));
            mWebAddress = String.format("%s#/c/%d/",
                    Prefs.getCurrentGerrit(context),
                    mCommitNumber);
            try {
                mCurrentRevision = object.getString(KEY_CURRENT_REVISION);
                mMessage = getMessageFromJSON(object, mCurrentRevision);
                mChangedFiles = getChangedFilesSet(object, mCurrentRevision);
                mAuthor = getCommitter(mCurrentRevision, KEY_AUTHOR, object);
                mCommitter = getCommitter(mCurrentRevision, KEY_COMMITTER, object);
            } catch (JSONException ignored) {
                // we only have these fields if we directly queried
                // mgerrit for this changeset
            }
            // handle labels
            try {
                JSONObject labels = object.getJSONObject(KEY_LABELS);
                mPatchSetNumber = getPatchSetNumberInternal(object, mCurrentRevision);
                mVerifiedReviewers = getReviewers(
                        labels.getJSONObject(KEY_VERIFIED).getJSONArray(KEY_ALL));
                mCodeReviewers = getReviewers(
                        labels.getJSONObject(KEY_CODE_REVIEW).getJSONArray(KEY_ALL));
            } catch (JSONException ignored) {
                // we did not directly query the patch set
            }
            // handle owner object that may not exist
            try {
                mOwnerObject = CommitterObject.getInstance(
                        object.getJSONObject(KEY_OWNER).getString(KEY_NAME),
                        object.getJSONObject(KEY_OWNER).getString(KEY_EMAIL));
            } catch (JSONException ignored) {
                // we did not directly query the patch set
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSONObject into useful data", e);
        }
    }

    private final JSONObject mRawJSONCommit;
    private String mKind;
    private String mId;
    private String mProject;
    private String mBranch;
    private String mChangeId;
    private String mSubject;
    private Status mStatus;
    private String mCreatedDate;
    private String mLastUpdatedDate;
    private boolean mIsMergeable;
    private String mSortKey;
    private int mCommitNumber;
    private String mOwnerName;
    private String mCurrentRevision;
    private CommitterObject mOwnerObject;
    private CommitterObject mAuthor;
    private CommitterObject mCommitter;
    private String mMessage;
    private List<ChangedFile> mChangedFiles;
    private String mWebAddress;
    private List<Reviewer> mVerifiedReviewers;
    private List<Reviewer> mCodeReviewers;
    private int mPatchSetNumber;

    private CommitterObject getCommitter(String currentRevision,
                                         String authorOrCommitter,
                                         JSONObject mainObject)
            throws JSONException {
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
        for (int i = 0; keysArray.length()> i; i++) {
            try {
                String path = (String) keysArray.get(i);
                list.add(ChangedFile.parseFromJSONObject(path,
                        filesObject.getJSONObject(path)));
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse jsonObject", e);
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
                list.add(Reviewer.getReviewerInstance(object.getString(KEY_VALUE),
                        object.getString(KEY_NAME)));
            } catch (JSONException je) {
                list.add(Reviewer.getReviewerInstance(null,
                        object.getString(KEY_NAME)));
            }
        }
        return list;
    }

    private String getOwnerName(JSONObject owners) {
        try {
            return owners.getString(KEY_NAME);
        } catch (JSONException e) {
            // should never happen all commit objects have an owner
            Log.wtf(TAG, "Failed to find commit owner!", e);
            return "Unknown";
        }
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

    public boolean isIsMergeable() {
        return mIsMergeable;
    }

    public String getSortKey() {
        return mSortKey;
    }

    public int getCommitNumber() {
        return mCommitNumber;
    }

    public String getOwnerName() {
        return mOwnerName;
    }

    public String getCurrentRevision() {
        return mCurrentRevision;
    }

    public CommitterObject getCommitter() {
        return mCommitter;
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


    public JSONObject getRawJSONCommit() {
        return mRawJSONCommit;
    }

    public List<Reviewer> getVerifiedReviewers() {
        return mVerifiedReviewers;
    }

    public ArrayList<Reviewer> getCodeReviewers() {
        return (ArrayList<Reviewer>) mCodeReviewers;
    }

    public CommitterObject getAuthor() {
        return mAuthor;
    }

    public CommitterObject getOwnerObject() {
        return mOwnerObject;
    }

    public int getPatchSetNumber() {
        return mPatchSetNumber;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("JSONCommit{");
        sb.append("mRawJSONCommit=").append(mRawJSONCommit);
        sb.append(", mKind='").append(mKind).append('\'');
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
        sb.append(", mOwnerName='").append(mOwnerName).append('\'');
        sb.append(", mCurrentRevision='").append(mCurrentRevision).append('\'');
        sb.append(", mOwnerObject=").append(mOwnerObject);
        sb.append(", mAuthor=").append(mAuthor);
        sb.append(", mCommitter=").append(mCommitter);
        sb.append(", mMessage='").append(mMessage).append('\'');
        sb.append(", mChangedFiles=").append(mChangedFiles);
        sb.append(", mWebAddress='").append(mWebAddress).append('\'');
        sb.append(", mVerifiedReviewers=").append(mVerifiedReviewers);
        sb.append(", mCodeReviewers=").append(mCodeReviewers);
        sb.append(", mPatchSetNumber=").append(mPatchSetNumber);
        sb.append('}');
        return sb.toString();
    }
}