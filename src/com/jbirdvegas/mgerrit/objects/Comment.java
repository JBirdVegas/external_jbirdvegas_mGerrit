package com.jbirdvegas.mgerrit.objects;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/12/13 4:11 PM
 *
 *
 *
 *
 */
public class Comment {
    private String mFile;
    private String mKind;
    private String mId;
    private long mLineNumber;
    // may be null if not a reply.
    private String mInReplyTo;
    private String mMessage;
    private String mTimeStamp;
    private long mAuthorAccountNumber;
    private String mAuthorName;
    private String mAuthorEmail;

    public Comment(String file,
                   String kind,
                   String id,
                   long lineNumber,
                   String message,
                   String timeStamp,
                   long authorAccountNumber,
                   String authorName,
                   String authorEmail) {
        this(file, kind, id, lineNumber, null, message, timeStamp, authorAccountNumber, authorName, authorEmail);
    }

    public Comment(String file,
                   String kind,
                   String id,
                   long lineNumber,
                   String inReplyTo,
                   String message,
                   String timeStamp,
                   long authorAccountNumber,
                   String authorName,
                   String authorEmail) {
        mFile = file;
        mKind = kind;
        mId = id;
        mLineNumber = lineNumber;
        mInReplyTo = inReplyTo;
        mMessage = message;
        mTimeStamp = timeStamp;
        mAuthorAccountNumber = authorAccountNumber;
        mAuthorName = authorName;
        mAuthorEmail = authorEmail;
    }

    public Comment(String file, JSONObject comment) throws JSONException {
        mFile = file;
        mKind = comment.getString(JSONCommit.KEY_KIND);
        mId = comment.getString(JSONCommit.KEY_ID);/*
        mLineNumber = comment.getLong(JSONCommit.KEY_LINE);
        try {
            mInReplyTo =  comment.getString(JSONCommit.KEY_IN_REPLY_TO);
        } catch (JSONException ignored) {
            mInReplyTo = null;
        }
        mMessage = comment.getString(JSONCommit.KEY_MESSAGE);
        mTimeStamp = comment.getString(JSONCommit.KEY_UPDATED);
        mAuthorAccountNumber = comment.getJSONObject(JSONCommit.KEY_AUTHOR).getLong(JSONCommit.KEY_ACCOUNT_ID);
        mAuthorName = comment.getJSONObject(JSONCommit.KEY_AUTHOR).getString(JSONCommit.KEY_NAME);
        mAuthorEmail = comment.getJSONObject(JSONCommit.KEY_AUTHOR).getString(JSONCommit.KEY_EMAIL);
        */
    }

    public String getFile() {
        return mFile;
    }

    public String getKind() {
        return mKind;
    }

    public String getId() {
        return mId;
    }

    public long getLineNumber() {
        return mLineNumber;
    }

    public String getInReplyTo() {
        return mInReplyTo;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getTimeStamp() {
        return mTimeStamp;
    }

    public long getAuthorAccountNumber() {
        return mAuthorAccountNumber;
    }

    public String getAuthorName() {
        return mAuthorName;
    }

    public String getAuthorEmail() {
        return mAuthorEmail;
    }
}