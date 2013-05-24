package com.jbirdvegas.mgerrit.listeners;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import com.jbirdvegas.mgerrit.CardsActivity;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.ChangeLogRange;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.objects.Reviewer;

public class TrackingClickListener implements View.OnClickListener {
    private static final String TAG = TrackingClickListener.class.getSimpleName();
    private Reviewer mReviewer;
    private String mProjectPath = null;
    private Activity mCallerActivity = null;
    private CommitterObject mCommitterObject = null;
    private ChangeLogRange mChangeLogRange = null;

    /**
     * Generate an AlertDialog asking if requesting original content
     * selected user has submitted (status:owner) or if all commits
     * user is tagged in as a Reviewer
     *
     * @param activity        Calling Activity
     * @param committerObject User selected to follow (stalk)
     */
    public TrackingClickListener(Activity activity, CommitterObject committerObject) {
        mCallerActivity = activity;
        mCommitterObject = committerObject;
    }

    /**
     * Shows all commits to a specific project
     *
     * @param projectPath
     */
    public TrackingClickListener(Activity activity, String projectPath) {
        mCallerActivity = activity;
        mProjectPath = projectPath;
    }

    public TrackingClickListener(Reviewer reviewer) {
        mReviewer = reviewer;
    }

    public TrackingClickListener(CardsActivity mCardsActivity, String project, ChangeLogRange changeLogRange) {
        mCallerActivity = mCardsActivity;
        mProjectPath = project;
        mChangeLogRange = changeLogRange;
    }

    public TrackingClickListener addUserToStalk(CommitterObject committerObject) {
        mCommitterObject = committerObject;
        return this;
    }

    public TrackingClickListener addProjectToStalk(String projectPath) {
        mProjectPath = projectPath;
        return this;
    }

    public TrackingClickListener removeUser() {
        mCommitterObject = null;
        return this;
    }

    public TrackingClickListener removePath() {
        mProjectPath = null;
        return this;
    }

    @Override
    public void onClick(final View view) {
        // Show content of a single user
        if (mCommitterObject != null && mProjectPath == null && mChangeLogRange == null) {
            // ask which content we are interested in
            CardsActivity.mSkipStalking = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(mCallerActivity);
            builder.setTitle(R.string.context_menu_view_diff_dialog);

            builder.setNegativeButton(R.string.context_menu_owner,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // notify the object what we want
                            mCommitterObject.setState(CardsActivity.KEY_OWNER);
                            view.getContext().startActivity(
                                    Prefs.getStalkerIntent(mCallerActivity, mCommitterObject));
                        }
                    });

            builder.setPositiveButton(R.string.context_menu_reviewer,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // notify the object what we want
                            mCommitterObject.setState(CardsActivity.KEY_REVIEWER);
                            view.getContext().startActivity(
                                    Prefs.getStalkerIntent(mCallerActivity, mCommitterObject));
                        }
                    });
            builder.create().show();
        } else if (mProjectPath != null && mCommitterObject == null && mChangeLogRange == null) {
            // Show content of an entire project relative to our current status
            view.getContext().startActivity(Prefs.getStalkerIntent(mCallerActivity)
                    .putExtra(JSONCommit.KEY_PROJECT, mProjectPath));
        } else if (mCommitterObject != null && mProjectPath != null && mChangeLogRange == null) {
            CardsActivity.mSkipStalking = false;
            view.getContext().startActivity(Prefs.getStalkerIntent(mCallerActivity, mCommitterObject)
                    .putExtra(JSONCommit.KEY_PROJECT, mProjectPath));
        } else if (mCommitterObject != null && mProjectPath != null && mChangeLogRange != null) {
            Intent changelogStalker = Prefs.getStalkerIntent(mCallerActivity, mCommitterObject)
                    .putExtra(JSONCommit.KEY_PROJECT, mProjectPath)
                    .putExtra(ChangeLogRange.KEY, mChangeLogRange);
            view.getContext().startActivity(changelogStalker);
        }
    }

    @Override
    public String toString() {
        return "StalkerModeClickListener{" +
                "mReviewer=" + mReviewer +
                ", mProjectPath='" + mProjectPath + '\'' +
                ", mCallerActivity=" + mCallerActivity +
                ", mCommitterObject=" + mCommitterObject +
                '}';
    }
}