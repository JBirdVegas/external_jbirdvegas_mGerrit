package com.aokp.gerrit.objects;

import android.content.Context;
import android.util.Log;
import com.aokp.gerrit.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 3/31/13
 * Time: 4:55 PM
 */
public class JSONCommit {
    private static final String TAG = JSONCommit.class.getSimpleName();

    public static final String KEY_STATUS_OPEN = "open";
    public static final String KEY_STATUS_MERGED = "merged";
    public static final String KEY_STATUS_ABANDONED = "abandoned";
    private static final String KEY_KIND = "kind";
    private static final String KEY_ID = "id";
    private static final String KEY_PROJECT = "project";
    private static final String KEY_BRANCH = "branch";
    private static final String KEY_CHANGE_ID = "change_id";
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_STATUS = "status";
    private static final String KEY_CREATED = "created";
    private static final String KEY_UPDATED = "updated";
    private static final String KEY_MERGEABLE = "mergeable";
    private static final String KEY_SORT_KEY = "_sortkey";
    private static final String KEY_COMMIT_NUMBER = "_number";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_NAME = "name";
    private static final String KEY_CURRENT_REVISION = "current_revision";
    private static final String KEY_COMMITTER = "committer";
    private static final String KEY_CHANGED_FILES = "files";
    private static final String KEY_INSERTED = "lines_inserted";
    private static final String KEY_DELETED = "lines_deleted";

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
     * @param object JSONObject sent by gerrit in response to a query
     */
    public JSONCommit(JSONObject object) {
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
            mOwner = getOwnerName(object.getJSONObject(KEY_OWNER));
            mWebAddress = "http://gerrit.sudoservers.com/#/c/" + mCommitNumber + '/';
            try {
                mCurrentRevision = object.getString(KEY_CURRENT_REVISION);
                mCommitter = object.getString(KEY_COMMITTER);
                mMessage = object.getString(KEY_MESSAGE);
                mChangedFiles = getChangedFilesSet(object.getJSONObject(KEY_CHANGED_FILES));
            } catch (JSONException ignored) {
                // we only have these fields if we directly queried
                // gerrit for this changeset
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
    private String mOwner;
    private String mCurrentRevision;
    private String mCommitter;
    private String mMessage;
    private List<ChangedFile> mChangedFiles;
    private String mWebAddress;

    class ChangedFile {
        public String path;
        public String status;
        public int inserted;
        public int deleted;
        public ChangedFile parseFromJSONObject(String path, JSONObject object) throws JSONException {
            this.path = path;
            this.status = object.getString(KEY_STATUS);
            this.inserted = object.getInt(KEY_INSERTED);
            this.deleted = object.getInt(KEY_DELETED);
            return this;
        }
    }

    private List<ChangedFile> getChangedFilesSet(JSONObject filesObject){
        List<ChangedFile> list = new LinkedList<ChangedFile>();
        JSONArray keysArray = filesObject.names();
        for (int i = 0; keysArray.length()> i; i++) {
            try {
                String path = (String) keysArray.get(i);
                list.add(new ChangedFile().parseFromJSONObject(path, filesObject.getJSONObject(path)));
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse jsonObject", e);
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

    public String getOwner() {
        return mOwner;
    }

    public String getCurrentRevision() {
        return mCurrentRevision;
    }

    public String getCommitter() {
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
}
