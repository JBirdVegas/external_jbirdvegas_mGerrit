package com.jbirdvegas.mgerrit;

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.analytics.tracking.android.EasyTracker;
import com.haarman.listviewanimations.swinginadapters.SingleAnimationAdapter;
import com.jbirdvegas.mgerrit.adapters.ChangeListAdapter;
import com.jbirdvegas.mgerrit.cards.CommitCardBinder;
import com.jbirdvegas.mgerrit.database.SyncTime;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.ChangeLoadingFinished;
import com.jbirdvegas.mgerrit.objects.GerritURL;
import com.jbirdvegas.mgerrit.tasks.GerritService;
import com.jbirdvegas.mgerrit.views.GerritSearchView;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public abstract class CardsFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String KEY_DEVELOPER = "committer_object";
    public static final String KEY_OWNER = "owner";
    public static final String KEY_REVIEWER = "reviewer";

    public static final String SEARCH_QUERY = "SEARCH";
    protected String TAG = "CardsFragment";

    private GerritURL mUrl;

    private RequestQueue mRequestQueue;

    private FragmentActivity mParent;

    // Indicates that this fragment will need to be refreshed
    private boolean mIsDirty = false;
    // Indicates whether a request to force an update is pending
    private boolean mNeedsForceUpdate = false;

    // Broadcast receiver to receive processed search query changes
    private BroadcastReceiver mSearchQueryListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String to = intent.getStringExtra(GerritSearchView.KEY_TO);
            if (isAdded() && mParent.getClass().getSimpleName().equals(to)) {
                getLoaderManager().restartLoader(0, null, CardsFragment.this);
            }
        }
    };

    private ListView mListView;
    // Adapter that binds data to the listview
    private ChangeListAdapter mAdapter;
    // Wrapper for mAdapter, enabling animations
    private SingleAnimationAdapter mAnimAdapter = null;
    // Whether animations have been enabled
    private boolean mAnimationsEnabled;
    private GerritSearchView mSearchView;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        init(savedInstanceState);
        setup();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.commit_list, container, false);
    }

    private void init(Bundle savedInstanceState) {
        mParent = this.getActivity();
        View mCurrentFragment = this.getView();
        mRequestQueue = Volley.newRequestQueue(mParent);

        // Setup the list
        int[] to = new int[] { R.id.commit_card_title, R.id.commit_card_commit_owner,
                R.id.commit_card_project_name, R.id.commit_card_last_updated,
                R.id.commit_card_commit_status };

        String[] from = new String[] { UserChanges.C_SUBJECT, UserChanges.C_NAME,
                UserChanges.C_PROJECT, UserChanges.C_UPDATED, UserChanges.C_STATUS };

        mListView = (ListView) mCurrentFragment.findViewById(R.id.commit_cards);
        mAdapter = new ChangeListAdapter(mParent, R.layout.commit_card, null, from, to, 0,
                getQuery());
        mAdapter.setViewBinder(new CommitCardBinder(mParent, mRequestQueue));

        registerForContextMenu(mListView);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mAdapter.itemClickListener(view);
            }
        });

        /* If animations have been enabled, setup and use an animation adapter, otherwise use
         *  the regular adapter. The data should always be bound to mAdapter */
       mAnimationsEnabled = Tools.toggleAnimations(Prefs.getAnimationPreference(mParent),
               mListView, mAnimAdapter, mAdapter);

        mUrl = new GerritURL();

        // Need the account id of the owner here to maintain FK db constraint
        mUrl.setRequestDetailedAccounts(true);
        mUrl.setStatus(getQuery());

        mSearchView = (GerritSearchView) mParent.findViewById(R.id.search);
    }

    private void setup()
    {
        sendRequest();

        // We cannot use the search query here as the SearchView may not have been initialised yet.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(mParent).registerReceiver(mSearchQueryListener,
                new IntentFilter(CardsFragment.SEARCH_QUERY));

        boolean animations = Prefs.getAnimationPreference(mParent);
        if (animations != mAnimationsEnabled) {
            toggleAnimations(animations);
        }
        EasyTracker.getInstance(getActivity()).activityStart(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(mParent).unregisterReceiver(mSearchQueryListener);
        EasyTracker.getInstance(getActivity()).activityStop(getActivity());
    }

    /**
     * Each tab provides its own query for ?p=status:[open:merged:abandoned]
     *
     * @return current tab name used for query { open, merged, abandoned }
     */
    abstract String getQuery();

    /**
     * Start the updater to check for an update if necessary
     */
    private void sendRequest() {
        if (mNeedsForceUpdate) {
            mNeedsForceUpdate = false;
            SyncTime.clear(mParent);
        }

        Intent it = new Intent(mParent, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Commit);
        it.putExtra(GerritService.URL_KEY, mUrl);
        mParent.startService(it);
    }

    /**
     * Refresh this fragment if it was marked as dirty by restarting the loader
     * @param forceUpdate If set, forces an update of the data from the server.
     *                    This can be independent of refreshing, so this method
     *                    can be used to force an update.
     */
    protected void refresh(boolean forceUpdate)
    {
        /* If the fragment has been attached to an activity, refresh it.
         *  Otherwise it will be refreshed when it is attached by checking
         *  whether it is marked as dirty.
         */
        if (this.isAdded() && mIsDirty) {
            mIsDirty = false;
            getLoaderManager().restartLoader(0, null, this);
        }

        mNeedsForceUpdate = forceUpdate;
        if (this.isAdded() && forceUpdate) sendRequest();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        /* Refresh if necessary. As the fragment has just been attached,
         *  we can assume it is added here */
        if (!mIsDirty) return;
        mIsDirty = false;
        getLoaderManager().restartLoader(0, null, this);
        if (mNeedsForceUpdate) sendRequest();
    }

    private void setMenuItemTitle(MenuItem menuItem, String formatString, String parameters) {
        String title = String.format(formatString, parameters);
        menuItem.setTitle(title);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = mParent.getMenuInflater();
        inflater.inflate(R.menu.change_list_menu, menu);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        View targetView = info.targetView;

        // Set the title of the user tracking menu item
        MenuItem userMenuItem = menu.findItem(R.id.menu_change_track_user);
        setMenuItemTitle(userMenuItem, getResources().getString(R.string.context_menu_track_user),
                (String) targetView.getTag(R.id.userName));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        View targetView = info.targetView;
        String webAddress = (String) targetView.getTag(R.id.webAddress);
        switch (item.getItemId()) {
            case R.id.menu_change_details:
                mListView.performItemClick(targetView, info.position, info.id);
                return true;
            case R.id.menu_change_browser:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webAddress));
                mParent.startActivity(browserIntent);
                return true;
            case R.id.menu_change_track_user:
                int user = (int) targetView.getTag(R.id.user);
                Prefs.setTrackingUser(mParent, user);
                return true;
            case R.id.menu_change_track_project:
                String project = (String) targetView.getTag(R.id.project);
                Prefs.setCurrentProject(mParent, project);
                return true;
            case R.id.menu_change_share:
                String changeid = (String) targetView.getTag(R.id.changeID);
                Intent intent = Tools.createShareIntent(mParent, changeid, webAddress);
                mParent.startActivity(intent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Enables or disables listview animations. This simply toggles the
     *  adapter, initialising a new adapter if necessary.
     * @param enable Whether to enable animations on the listview
     */
    public void toggleAnimations(boolean enable) {
        mAnimationsEnabled = Tools.toggleAnimations(enable, mListView, mAnimAdapter, mAdapter);
    }

    public void markDirty() { mIsDirty = true; }

    public void markChangeAsSelected(String changeid) {
        if (mAdapter != null) mAdapter.setSelectedChangeId(changeid);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        if (args == null) {
            args = mSearchView.getLastProcessedQuery();
            String to = args.getString(GerritSearchView.KEY_TO);
            if (!mParent.getClass().getSimpleName().equals(to))
                args = null;
        }

        if (args != null) {
            String databaseQuery = args.getString("WHERE");
            if (databaseQuery != null && !databaseQuery.isEmpty()) {
                if (args.getStringArrayList("BIND_ARGS") != null) {
                    /* Create a copy as the findCommits function can modify the contents of bindArgs
                     *  and we want each receiver to use the bindArgs from the original broadcast */
                    ArrayList<String> bindArgs = new ArrayList<>();
                    bindArgs.addAll(args.getStringArrayList("BIND_ARGS"));
                    return UserChanges.findCommits(mParent, getQuery(), databaseQuery, bindArgs);
                }
            }
        }
        return UserChanges.findCommits(mParent, getQuery(), null, null);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Naive implementation
        mAdapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
        // Broadcast that we have finished loading changes
        new ChangeLoadingFinished(mParent, getQuery()).sendUpdateMessage();
    }

}
