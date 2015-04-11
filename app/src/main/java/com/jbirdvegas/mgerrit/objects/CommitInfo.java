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

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.jbirdvegas.mgerrit.R;

import java.util.List;

/**
 * Contains information about a mRevision or patch set. This condenses both
 *  the RevisionInfo and CommitInfo objects together into the one level
 */
public class CommitInfo {

    /** The Change-Id of the change. */
    private String mChangeId;

    private static final String KEY_COMMIT = "commit";
    /**The commit ID of the current patch set of this change.
     * Don't set the @SerialisedName as there is a name conflict here */
    private String mCommit;

    public static final String KEY_AUTHOR = "author";
    @SerializedName(CommitInfo.KEY_AUTHOR)
    private CommitterObject mAuthorObject;

    private static final String KEY_COMMITTER = "committer";
    @SerializedName(CommitInfo.KEY_COMMITTER)
    private CommitterObject mCommitterObject;

    public static final String KEY_MESSAGE = "message";
    @SerializedName(CommitInfo.KEY_MESSAGE)
    private String mMessage;

    @SerializedName(JSONCommit.KEY_SUBJECT)
    private String mSubject;

    @SerializedName("_number")
    private String mPatchSetNumber;

    public static final String KEY_CHANGED_FILES = "files";
    // Information about the files in this patch set.
    @SerializedName(CommitInfo.KEY_CHANGED_FILES)
    private FileInfoList mFileInfos;

    @SerializedName("draft")
    private boolean mIsDraft;

    public static CommitInfo deserialise(JsonObject object, String changeId) {
        CommitInfo revision = new Gson().fromJson(object, CommitInfo.class);
        revision.mChangeId = changeId;

        // Add the changed files
        if (object.has(CommitInfo.KEY_CHANGED_FILES)) {
            JsonObject filesObj = object.get(CommitInfo.KEY_CHANGED_FILES).getAsJsonObject();
            revision.mFileInfos = FileInfoList.deserialize(filesObj);
        }

        // Some objects are nested here
        if (object.has(CommitInfo.KEY_COMMIT)) {
            JsonObject commitObj = object.get(KEY_COMMIT).getAsJsonObject();
            CommitInfo r2 = new Gson().fromJson(commitObj, CommitInfo.class);
            merge(revision, r2);

            if (commitObj.has(KEY_COMMIT)) {
                revision.mCommit = commitObj.get(KEY_COMMIT).getAsString();
            }
        }

        return revision;
    }

    /**
     * Merge two CommitInfo objects together, modifying the first
     * @param a The commitInfo object to be modified
     * @param b Secondary commitInfo object to be merged into a
     */
    private static void merge(CommitInfo a, final CommitInfo b) {
        a.mAuthorObject = b.mAuthorObject == null ? a.mAuthorObject : b.mAuthorObject;
        a.mCommitterObject = b.mCommitterObject == null ? a.mCommitterObject : b.mCommitterObject;
        a.mMessage = b.mMessage == null ? a.mMessage : b.mMessage;
        a.mSubject = b.mSubject == null ? a.mSubject : b.mSubject;
    }

    public CommitterObject getAuthorObject() {
        return mAuthorObject;
    }

    public CommitterObject getCommitterObject() {
        return mCommitterObject;
    }

    public String getMessage() {
        return mMessage;
    }

    protected void setMessage(Context context) {
        if (mMessage == null) {
            this.mMessage = context.getString(R.string.current_revision_is_draft_message);
        }
    }

    public String getChangeId() { return mChangeId; }

    public String getCommit() { return mCommit; }

    public String getSubject() { return mSubject; }

    public String getPatchSetNumber() { return mPatchSetNumber; }

    public boolean isIsDraft() { return mIsDraft; }

    public List<FileInfo> getChangedFiles() { return mFileInfos.getFiles(); }
    public void setChangedFiles(FileInfoList fileInfos) { this.mFileInfos = fileInfos; }

    @Override
    public String toString() {
        return "CommitInfo{" +
                "mChangeId='" + mChangeId + '\'' +
                ", mCommit='" + mCommit + '\'' +
                ", mAuthorObject=" + mAuthorObject +
                ", mCommitterObject=" + mCommitterObject +
                ", mMessage='" + mMessage + '\'' +
                ", mSubject='" + mSubject + '\'' +
                ", mPatchSetNumber='" + mPatchSetNumber + '\'' +
                ", mFileInfos=" + mFileInfos +
                ", mIsDraft=" + mIsDraft +
                '}';
    }
}
