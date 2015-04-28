package com.jbirdvegas.mgerrit.fragments;

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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;

import com.google.analytics.tracking.android.EasyTracker;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.activities.GerritControllerActivity;
import com.jbirdvegas.mgerrit.adapters.CommitDetailsAdapter;
import com.jbirdvegas.mgerrit.cards.PatchSetChangesCard;
import com.jbirdvegas.mgerrit.database.Changes;
import com.jbirdvegas.mgerrit.database.Config;
import com.jbirdvegas.mgerrit.database.FileChanges;
import com.jbirdvegas.mgerrit.database.Revisions;
import com.jbirdvegas.mgerrit.database.SelectedChange;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.database.UserMessage;
import com.jbirdvegas.mgerrit.database.UserReviewers;
import com.jbirdvegas.mgerrit.helpers.AnalyticsHelper;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.ChangeLoadingFinished;
import com.jbirdvegas.mgerrit.message.NewChangeSelected;
import com.jbirdvegas.mgerrit.message.StatusSelected;
import com.jbirdvegas.mgerrit.objects.FilesCAB;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.requestbuilders.ChangeEndpoints;
import com.jbirdvegas.mgerrit.search.ChangeSearch;
import com.jbirdvegas.mgerrit.tasks.GerritService;

import de.greenrobot.event.EventBus;

/**
 * Class handles populating the screen with several
 * cards each giving more information about the patchset
 * <p/>
 * All cards are located at jbirdvegas.mgerrit.cards.*
 */
public class PatchSetViewerFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private View disconnectedView;
    private Activity mParent;
    private Context mContext;

    private ChangeEndpoints mUrl;
    private String mSelectedChange;
    private String mStatus;
    private int mChangeNumber;
    // Whether the server supports the new change details endpoint (false if so)
    private boolean sIsLegacyVersion;

    private CommitDetailsAdapter mAdapter;
    private FilesCAB mFilesCAB;

    public static final String CHANGE_ID = "changeID";
    public static final String CHANGE_NO = "changeNo";
    public static final String STATUS = "queryStatus";

    public static final int LOADER_COMMIT = 1;
    public static final int LOADER_PROPERTIES = 2;
    public static final int LOADER_MESSAGE = 3;
    public static final int LOADER_FILES = 4;
    public static final int LOADER_REVIEWERS = 5;
    public static final int LOADER_COMMENTS = 6;

    private EventBus mEventBus;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.patchset_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParent = this.getActivity();
        mContext = mParent.getApplicationContext();
    }

    private void init() {
        View currentFragment = this.getView();

        ExpandableListView mListView = (ExpandableListView) currentFragment.findViewById(R.id.commit_cards);
        disconnectedView = currentFragment.findViewById(R.id.disconnected_view);

        sIsLegacyVersion = !Config.isDiffSupported(mParent);

        mAdapter = new CommitDetailsAdapter(mParent);
        mListView.setAdapter(mAdapter);

        // Child click listeners (relevant for the changes cards)
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mFilesCAB = new FilesCAB(mParent, !sIsLegacyVersion);
        mAdapter.setContextualActionBar(mFilesCAB);
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ExpandableListView listView = (ExpandableListView) parent;
                long pos = listView.getExpandableListPosition(position);
                int groupPos = ExpandableListView.getPackedPositionGroup(pos);
                int childPos = ExpandableListView.getPackedPositionChild(pos);

                if (!mAdapter.isLongClickSupported(groupPos, childPos)) {
                    return false;
                }

                // In case this is a group view and does not have the change number tagged
                view.setTag(R.id.changeID, mSelectedChange);
                FilesCAB.TagHolder holder = new FilesCAB.TagHolder(view, mContext,
                        groupPos, childPos >= 0);

                // Set the title to be shown in the action bar
                if (holder.filePath != null) {
                    mFilesCAB.setTitle(holder.filePath);
                } else {
                    String s = mParent.getResources().getString(R.string.change_detail_heading);
                    mFilesCAB.setTitle(String.format(s, holder.changeNumber.intValue()));
                }

                mFilesCAB.setActionMode(getActivity().startActionMode(mFilesCAB));
                ActionMode actionMode = mFilesCAB.getActionMode();

                // Call requires API 14 (ICS)
                actionMode.setTag(holder);
                view.setSelected(true);
                return true;
            }
        });
        mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                // This is only valid for the changed files group
                int childItemType = mAdapter.getChildType(groupPosition, childPosition);
                if (childItemType != CommitDetailsAdapter.Cards.CHANGED_FILES.ordinal()) {
                    return false;
                }
                // View the diff and close the CAB if a change diff could be viewed
                boolean diffLaunched = PatchSetChangesCard.onViewClicked(mParent, v);
                if (diffLaunched) {
                    ActionMode mode = mFilesCAB.getActionMode();
                    if (mode != null) mode.finish();
                }
                return diffLaunched;
            }
        });

        // Remember to expand the groups which don't have a header otherwise they will not be shown
        mListView.expandGroup(0);

        mUrl = new ChangeEndpoints();

        Button retryButton = (Button) currentFragment.findViewById(R.id.btn_retry);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sIsLegacyVersion) sendRequest(GerritService.DataType.LegacyCommitDetails);
                else sendRequest(GerritService.DataType.Commit);
            }
        });

        if (getArguments() == null) {
            /** This should be the default value of {@link ChangeListFragment.mSelectedStatus } */
            setStatus(JSONCommit.Status.NEW.toString());
            loadChange(true);
        } else {
            Bundle args = getArguments();
            setStatus(args.getString(STATUS));
            String changeid = args.getString(CHANGE_ID);
            mChangeNumber = args.getInt(CHANGE_NO);

            if (changeid != null && !changeid.isEmpty()) {
                loadChange(changeid);
            }
        }

        mEventBus = EventBus.getDefault();
    }

    /**
     * Start the updater to check for an update if necessary
     */
    private void sendRequest(GerritService.DataType dataType) {

        // If we aren't connected, there's nothing to do here
        if (!switchViews()) return;

        /*
         * Requires Gerrit version 2.8
         * /changes/{change-id}/detail with arguments was introduced in version 2.8,
         * so this will not be able to get the files changed or the full commit message
         * in prior Gerrit versions.
         */
        GerritService.sendRequest(mParent, dataType, mUrl);
    }

    private void restartLoaders(String changeID) {
        Bundle args = new Bundle();
        args.putString(CHANGE_ID, changeID);

        for (int i = LOADER_COMMIT; i <= LOADER_COMMENTS; i++) {
            getLoaderManager().restartLoader(i, args, this);
        }
    }

    /**
     * Determine the changeid to load and send an intent to load the change.
     * By sending an intent, the main activity is notified (GerritControllerActivity
     * on tablets. This can then tell the change list adapter that we have selected
     * a change.
     *
     * @param direct true: load this change directly, false: send out an intent
     */
    private void loadChange(boolean direct) {
        if (mStatus == null) {
            // Without the status we cannot find a changeid to load data for
            return;
        }

        Pair<String, Integer> change = SelectedChange.getSelectedChange(mContext, mStatus);
        String changeID;
        int changeNumber;

        if (change == null || change.first.isEmpty()) {
            change = Changes.getMostRecentChange(mParent, mStatus);
            if (change == null || change.first.isEmpty()) {
                // No changes to load data from
                AnalyticsHelper.sendAnalyticsEvent(mParent, "PatchSetViewerFragment",
                        "load_change", "null_changeID", null);
                return;
            }
        }

        changeID = change.first;
        mChangeNumber = change.second;

        if (direct) loadChange(changeID);
        else {
            mEventBus.post(new NewChangeSelected(changeID, mChangeNumber, mStatus, this));
        }
    }

    /**
     * Set the change id to load details for and load the change
     *
     * @param changeId A valid change id
     */
    public void loadChange(String changeId) {
        // If we have already loaded this change there is nothing to do
        if (!changeId.equals(this.mSelectedChange)) {
            this.mSelectedChange = changeId;

            mUrl.addSearchKeyword(new ChangeSearch(mSelectedChange));
            mUrl.setChangeNumber(mChangeNumber);
            mUrl.requestChangeDetail(true, sIsLegacyVersion);

            if (sIsLegacyVersion) sendRequest(GerritService.DataType.LegacyCommitDetails);
            else sendRequest(GerritService.DataType.CommitDetails);

            restartLoaders(changeId);
        }
    }

    /**
     * Use this to set the status to ensure we only use the database status
     *
     * @param status A valid change status string (database or web format)
     */
    public void setStatus(String status) {
        this.mStatus = JSONCommit.Status.getStatusString(status);
    }

    public boolean compareStatus(String status1, String status2) {
        return (JSONCommit.Status.getStatusString(status1)).equals(JSONCommit.Status.getStatusString(status2));
    }

    /**
     * Helper function to get the selected status in the change list fragment.
     * If it is phone mode (the parent is not the main activity), then this will
     * return null.
     *
     * @return The selected status in the change list fragment, or null if not available
     */
    @Nullable
    private String getStatus() {
        if (mParent instanceof GerritControllerActivity) {
            GerritControllerActivity controllerActivity = (GerritControllerActivity) mParent;
            return controllerActivity.getChangeList().getStatus();
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mEventBus.register(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance(mParent).activityStart(mParent);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(mParent).activityStop(mParent);
    }

    @Override
    public void onPause() {
        super.onPause();
        mEventBus.unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CHANGE_ID, mSelectedChange);
        outState.putInt(CHANGE_NO, mChangeNumber);
        outState.putString(STATUS, mStatus);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mSelectedChange = savedInstanceState.getString(CHANGE_ID);
            mChangeNumber = savedInstanceState.getInt(CHANGE_NO);
            setStatus(savedInstanceState.getString(STATUS));
            restartLoaders(mSelectedChange);
        }
        super.onViewStateRestored(savedInstanceState);
    }

    /*
    Possible cards

    --Patch Set--
    Select patchset number to display in these cards
    -------------

    --Times Card--
    Original upload time
    Most recent update
    --------------

    --Inline comments Card?--
    Show all comments inlined on code view pages
    **may be kind of pointless without context of surrounding code**
    * maybe a webview for each if possible? *
    -------------------------

     */

    private boolean switchViews() {
        boolean isconn = Tools.isConnected(mParent);
        if (isconn) {
            disconnectedView.setVisibility(View.GONE);
        } else {
            disconnectedView.setVisibility(View.VISIBLE);
        }
        return isconn;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String changeID = args.getString(PatchSetViewerFragment.CHANGE_ID);

        switch (id) {
            case LOADER_COMMIT:
                return UserChanges.getCommitProperties(mContext, changeID);
            case LOADER_PROPERTIES:
                return Changes.getCommitProperties(mContext, changeID);
            case LOADER_MESSAGE:
                return Revisions.getCommitMessage(mContext, changeID);
            case LOADER_FILES:
                return FileChanges.getFileChanges(mContext, changeID);
            case LOADER_REVIEWERS:
                return UserReviewers.getReviewersForChange(mContext, changeID);
            case LOADER_COMMENTS:
                return UserMessage.getMessagesForChange(mContext, changeID);
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        CommitDetailsAdapter.Cards cardType = null;

        switch (cursorLoader.getId()) {
            case LOADER_COMMIT:
                cardType = CommitDetailsAdapter.Cards.COMMIT;
                break;
            case LOADER_PROPERTIES:
                cardType = CommitDetailsAdapter.Cards.PROPERTIES;
                break;
            case LOADER_MESSAGE:
                cardType = CommitDetailsAdapter.Cards.COMMIT_MSG;
                break;
            case LOADER_FILES:
                cardType = CommitDetailsAdapter.Cards.CHANGED_FILES;
                break;
            case LOADER_REVIEWERS:
                cardType = CommitDetailsAdapter.Cards.REVIEWERS;
                break;
            case LOADER_COMMENTS:
                cardType = CommitDetailsAdapter.Cards.COMMENTS;
                break;
        }
        if (cardType != null) mAdapter.setCursor(cardType, cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        onLoadFinished(cursorLoader, null);
    }

    public void onEventMainThread(StatusSelected ev) {
        setStatus(ev.getStatus());
        loadChange(false);
    }

    public void onEventMainThread(ChangeLoadingFinished ev) {
        String status = ev.getStatus();

        /* We may have got a broadcast saying that data from another tab
         *  has been loaded. */
        if (compareStatus(status, getStatus())) {
            setStatus(status);
            loadChange(false);
        }
    }
}
