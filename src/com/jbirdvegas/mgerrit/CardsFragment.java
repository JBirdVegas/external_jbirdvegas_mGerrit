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
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.support.v4.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.analytics.tracking.android.EasyTracker;
import com.haarman.listviewanimations.swinginadapters.SingleAnimationAdapter;
import com.jbirdvegas.mgerrit.adapters.ChangeListAdapter;
import com.jbirdvegas.mgerrit.adapters.EndlessAdapterWrapper;
import com.jbirdvegas.mgerrit.cards.CommitCardBinder;
import com.jbirdvegas.mgerrit.database.Changes;
import com.jbirdvegas.mgerrit.database.MoreChanges;
import com.jbirdvegas.mgerrit.database.SyncTime;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.ChangeLoadingFinished;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.StartingRequest;
import com.jbirdvegas.mgerrit.objects.GerritURL;
import com.jbirdvegas.mgerrit.search.AfterSearch;
import com.jbirdvegas.mgerrit.search.BeforeSearch;
import com.jbirdvegas.mgerrit.search.SearchKeyword;
import com.jbirdvegas.mgerrit.tasks.GerritService;
import com.jbirdvegas.mgerrit.tasks.GerritService.Direction;
import com.jbirdvegas.mgerrit.views.GerritSearchView;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Set;

public abstract class CardsFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener {

    public static final String SEARCH_QUERY = "SEARCH";
    private static int sChangesLimit = 0;
    protected String TAG = "CardsFragment";

    private GerritURL mUrl;

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

    /* Broadcast receiver to tell the endless adapter we have finished loading when there was
     * no data */
    private final BroadcastReceiver finishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mSwipeLayout != null) mSwipeLayout.setRefreshing(false);

            Intent processed = intent.getParcelableExtra(Finished.INTENT_KEY);
            Direction direction = (Direction) processed.getSerializableExtra(GerritService.CHANGES_LIST_DIRECTION);

            if (mEndlessAdapter == null || direction == Direction.Newer) return;

            int itemsFetched = intent.getIntExtra(Finished.ITEMS_FETCHED_KEY, 0);
            if (itemsFetched < sChangesLimit) {
                // Remove the endless adapter as we have no more changes to load
                mEndlessAdapter.finishedDataLoading();
            }
        }
    };

    private final BroadcastReceiver startReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mSwipeLayout.setRefreshing(true);
        }
    };

    private ListView mListView;
    // Adapter that binds data to the listview
    private ChangeListAdapter mAdapter;
    // Wrapper for mAdapter, enabling animations
    private SingleAnimationAdapter mAnimAdapter;
    // Wrapper for above listview adapters to help provide infinite list functionality
    private EndlessAdapterWrapper mEndlessAdapter;

    // Whether animations have been enabled
    private Boolean mAnimationsEnabled = null;

    private GerritSearchView mSearchView;
    private SwipeRefreshLayout mSwipeLayout;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
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
        RequestQueue mRequestQueue = Volley.newRequestQueue(mParent);

        // Setup the list
        int[] to = new int[] { R.id.commit_card_title, R.id.commit_card_commit_owner,
                R.id.commit_card_project_name, R.id.commit_card_last_updated,
                R.id.commit_card_commit_status };

        String[] from = new String[] { UserChanges.C_SUBJECT, UserChanges.C_NAME,
                UserChanges.C_PROJECT, UserChanges.C_UPDATED, UserChanges.C_STATUS };

        mListView = (ListView) mCurrentFragment.findViewById(R.id.commit_cards);
        registerForContextMenu(mListView);

        mAdapter = new ChangeListAdapter(mParent, R.layout.commit_card, null, from, to, 0,
                getQuery());
        mAdapter.setViewBinder(new CommitCardBinder(mParent, mRequestQueue));

        mEndlessAdapter = new EndlessAdapterWrapper(mParent, mAdapter) {
            @Override
            public void loadData() {
                Set<SearchKeyword> keywords = mSearchView.getLastQuery();
                String updated = Changes.getOldestUpdatedTime(mParent, getQuery());
                if (updated != null) {
                    keywords = SearchKeyword.retainOldest(keywords, new BeforeSearch(updated));
                }
                sendRequest(Direction.Older, keywords);
            }
        };

        mEndlessAdapter.setEnabled(MoreChanges.areOlderChanges(mParent, getQuery()));

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mAdapter.itemClickListener(view);
            }
        });

        sChangesLimit = mParent.getResources().getInteger(R.integer.changes_limit);

        mUrl = new GerritURL();
        // Need the account id of the owner here to maintain FK db constraint
        mUrl.setRequestDetailedAccounts(true);
        mUrl.setStatus(getQuery());

        mSearchView = (GerritSearchView) mParent.findViewById(R.id.search);

        mSwipeLayout = (SwipeRefreshLayout) mParent.findViewById(R.id.swipe_container);

        mSwipeLayout.setColorScheme(R.color.text_orange, R.color.text_green, R.color.text_red,
                android.R.color.transparent);
    }

    private void setup()
    {
        loadNewerChanges();

        // We cannot use the search query here as the SearchView may not have been initialised yet.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(mParent).registerReceiver(mSearchQueryListener,
                new IntentFilter(CardsFragment.SEARCH_QUERY));

        if (mEndlessAdapter != null) {
            toggleAnimations(mEndlessAdapter);
            mListView.setOnScrollListener(mEndlessAdapter);
        } else toggleAnimations(mAdapter);

        EasyTracker.getInstance(getActivity()).activityStart(getActivity());

        LocalBroadcastManager.getInstance(mParent).registerReceiver(startReceiver,
            new IntentFilter(StartingRequest.TYPE));

        LocalBroadcastManager.getInstance(mParent).registerReceiver(finishedReceiver,
                new IntentFilter(Finished.TYPE));

        mSwipeLayout.setOnRefreshListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(mParent).unregisterReceiver(mSearchQueryListener);
        EasyTracker.getInstance(getActivity()).activityStop(getActivity());

        LocalBroadcastManager.getInstance(mParent).unregisterReceiver(startReceiver);
        LocalBroadcastManager.getInstance(mParent).unregisterReceiver(finishedReceiver);

        mSwipeLayout.setOnRefreshListener((TheApplication) mParent.getApplication());
    }

    /**
     * Each tab provides its own query for ?p=status:[open:merged:abandoned]
     *
     * @return current tab name used for query { open, merged, abandoned }
     */
    abstract String getQuery();

    private void loadNewerChanges() {
        Set<SearchKeyword> keywords = null;

        String updated = Changes.getNewestUpdatedTime(mParent, getQuery());
        if (updated != null && !updated.isEmpty()) {
            keywords = mSearchView.getLastQuery();
            SearchKeyword.replaceKeyword(keywords, new AfterSearch(updated));
        }

        sendRequest(Direction.Newer, keywords);
    }

    /**
     * Start the updater to check for an update if necessary
     */
    private void sendRequest(Direction direction, Set<SearchKeyword> keywords) {
        if (mNeedsForceUpdate) {
            mNeedsForceUpdate = false;
            SyncTime.clear(mParent);
        }

        GerritURL url = new GerritURL(mUrl);
        if (sChangesLimit > 0) url.setLimit(sChangesLimit);

        if (keywords == null || keywords.isEmpty()) {
            keywords = mSearchView.getLastQuery();
        }
        url.addSearchKeywords(keywords);

        Intent it = new Intent(mParent, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Commit);
        it.putExtra(GerritService.URL_KEY, url);
        it.putExtra(GerritService.CHANGES_LIST_DIRECTION, direction);
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
        if (this.isAdded() && forceUpdate) loadNewerChanges();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        /* Refresh if necessary. As the fragment has just been attached,
         *  we can assume it is added here */
        if (!mIsDirty) return;
        mIsDirty = false;
        getLoaderManager().restartLoader(0, null, this);
        if (mNeedsForceUpdate) loadNewerChanges();
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
     * @param baseAdapter The current adapter for the listview
     */
    public void toggleAnimations(BaseAdapter baseAdapter) {
        boolean anim = Prefs.getAnimationPreference(mParent);
        if (mAnimationsEnabled != null && anim == mAnimationsEnabled) return;
        else mAnimationsEnabled = anim;

        /* If animations have been enabled, setup and use an animation adapter, otherwise use
         *  the regular adapter. The data should always be bound to mAdapter */
        BaseAdapter adapter = Tools.toggleAnimations(mAnimationsEnabled, mListView, mAnimAdapter, baseAdapter);

        if (mAnimationsEnabled) {
            mAnimAdapter = (SingleAnimationAdapter) adapter;
            if (baseAdapter == mEndlessAdapter) {
                mEndlessAdapter.setParentAdatper(mAnimAdapter);
            }
        }
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

        if (cursor.getCount() < 1 && mEndlessAdapter != null) {
            mEndlessAdapter.startDataLoading();
        }
    }

    @Override public void onRefresh() {
        refresh(true);
    }
}
