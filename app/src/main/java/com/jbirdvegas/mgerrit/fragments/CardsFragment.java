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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.analytics.tracking.android.EasyTracker;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.activities.BaseDrawerActivity;
import com.jbirdvegas.mgerrit.activities.RefineSearchActivity;
import com.jbirdvegas.mgerrit.adapters.ChangeListAdapter;
import com.jbirdvegas.mgerrit.adapters.EndlessAdapterWrapper;
import com.jbirdvegas.mgerrit.adapters.HeaderAdapterDecorator;
import com.jbirdvegas.mgerrit.adapters.HeaderAdapterWrapper;
import com.jbirdvegas.mgerrit.cards.CommitCardBinder;
import com.jbirdvegas.mgerrit.database.Changes;
import com.jbirdvegas.mgerrit.database.MoreChanges;
import com.jbirdvegas.mgerrit.database.SyncTime;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.ChangeLoadingFinished;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.NewChangeSelected;
import com.jbirdvegas.mgerrit.message.SearchQueryChanged;
import com.jbirdvegas.mgerrit.message.SearchStateChanged;
import com.jbirdvegas.mgerrit.message.StartingRequest;
import com.jbirdvegas.mgerrit.search.AfterSearch;
import com.jbirdvegas.mgerrit.search.BeforeSearch;
import com.jbirdvegas.mgerrit.search.SearchKeyword;
import com.jbirdvegas.mgerrit.tasks.GerritService;
import com.jbirdvegas.mgerrit.tasks.GerritService.Direction;
import com.jbirdvegas.mgerrit.views.GerritSearchView;
import com.nhaarman.listviewanimations.appearance.SingleAnimationAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Set;

import se.emilsjolander.stickylistheaders.ExpandableStickyListHeadersListView;

public abstract class CardsFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static int sChangesLimit = 0;
    protected final String TAG = getClass().getSimpleName();

    private FragmentActivity mParent;

    // Indicates that this fragment will need to be refreshed
    private boolean mIsDirty = false;
    // Indicates whether a request to force an update is pending
    private boolean mNeedsForceUpdate = false;
    // Indicates whether the current fragment is refreshing
    private boolean mIsRefreshing = false;

    private static boolean sIsTabletMode = false;

    private ExpandableStickyListHeadersListView mListView;
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
    private SwipeRefreshLayout.OnRefreshListener mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refresh(true);
        }
    };
    private EventBus mEventBus;
    private HeaderAdapterDecorator mHeaderAdapterWrapper;
    private HeaderAdapterWrapper mHeaderAdapter;

    View mRefineSearchCard;
    private boolean refineSearchShowing = false;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        init(savedInstanceState);
        setup();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.commit_list, container, false);

        // Its important to attach this reference to every view created
        // if not then when a CardsFragment gets created for each subsequent
        // subclass the referenced layout is actually pointing to the original
        // fragment. Hence all other fragments will throw null when we attempt
        // to perform actions (like swiping) on the current fragment.
        setSwipeRefreshLayout(view);
        return view;
    }

    private void setSwipeRefreshLayout(View view) {
        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeLayout.setColorSchemeResources(R.color.text_orange, R.color.text_green, R.color.text_red,
                android.R.color.transparent);
        mSwipeLayout.setOnRefreshListener(mRefreshListener);
    }

    private void init(Bundle savedInstanceState) {
        mParent = this.getActivity();
        View mCurrentFragment = this.getView();
        RequestQueue mRequestQueue = Volley.newRequestQueue(mParent);

        // Setup the list
        int[] to = new int[] { R.id.commit_card_title, R.id.commit_card_commit_owner,
                R.id.commit_card_project_name, R.id.commit_card_last_updated,
                R.id.commit_card_commit_status, R.id.commit_card_committer_image,
                R.id.commit_card_starred };

        String[] from = new String[] { UserChanges.C_SUBJECT, UserChanges.C_NAME,
                UserChanges.C_PROJECT, UserChanges.C_UPDATED, UserChanges.C_STATUS, UserChanges.C_EMAIL,
                UserChanges.C_STARRED };

        mListView = (ExpandableStickyListHeadersListView) mCurrentFragment.findViewById(R.id.commit_cards);
        mAdapter = new ChangeListAdapter(mParent, R.layout.commit_card_row, null, from, to, 0,
                getQuery());
        mAdapter.setViewBinder(new CommitCardBinder(mParent, mRequestQueue));

        mHeaderAdapter = new HeaderAdapterWrapper(mParent, mAdapter);

        mEndlessAdapter = new EndlessAdapterWrapper(mParent, mHeaderAdapter) {
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

        mHeaderAdapterWrapper = new HeaderAdapterDecorator(mEndlessAdapter, mHeaderAdapter);
        mListView.setAdapter(mHeaderAdapterWrapper);
        mListView.setDrawingListUnderStickyHeader(false);
        mListView.getWrappedList().setDividerHeight(32);

        sChangesLimit = mParent.getResources().getInteger(R.integer.changes_limit);

        mSearchView = (GerritSearchView) mParent.findViewById(R.id.search);

        mEventBus = EventBus.getDefault();

        sIsTabletMode = PrefsFragment.isTabletMode(mParent);
    }

    private void setup() {
        if (mListView.getCount() < 1) sendRequest(Direction.Older, null);
        else loadNewerChanges();

        // We cannot use the search query here as the SearchView may not have been initialised yet.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mEndlessAdapter != null) {
            toggleAnimations(mEndlessAdapter);
            mListView.setOnScrollListener(mEndlessAdapter);
        } else toggleAnimations(mHeaderAdapter);

        EasyTracker.getInstance(getActivity()).activityStart(getActivity());

        mEventBus.register(this);

        // We would get this event after using the viewpager in the Patchset Viewer
        NewChangeSelected ev = mEventBus.getStickyEvent(NewChangeSelected.class);
        if (ev != null && ev.compareStatuses(getQuery())) {
            mAdapter.setSelectedChangeId(ev.getChangeId());
            mEventBus.removeStickyEvent(ev); // Only remove if we processed it
        }

        // If we have filters active, show the option to refine the search
        if (mSearchView.getFilterCount() > 0) {
            toggleRefineSearchCard(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(getActivity()).activityStop(getActivity());

        mEventBus.unregister(this);
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

        if (keywords == null || keywords.isEmpty()) {
            keywords = mSearchView.getLastQuery();
        }

        ArrayList<SearchKeyword> keywordsArray = new ArrayList<>();
        keywordsArray.addAll(keywords);

        Intent it = new Intent(mParent, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Commit);
        it.putParcelableArrayListExtra(GerritService.CHANGE_KEYWORDS, keywordsArray);
        it.putExtra(GerritService.CHANGES_LIST_DIRECTION, direction);
        it.putExtra(GerritService.CHANGE_STATUS, getQuery());
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
        if (this.isAdded() && forceUpdate) {
            if (mListView.getCount() < 1) sendRequest(Direction.Older, null);
            else loadNewerChanges();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (mSwipeLayout != null) {
            mSwipeLayout.setRefreshing(mIsRefreshing);
        }

        /* Refresh if necessary. As the fragment has just been attached,
         *  we can assume it is added here */
        if (!mIsDirty) return;
        mIsDirty = false;
        getLoaderManager().restartLoader(0, null, this);
        if (mNeedsForceUpdate) {
            if (mListView.getCount() < 1) sendRequest(Direction.Older, null);
            else loadNewerChanges();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = mParent.getMenuInflater();
        inflater.inflate(R.menu.change_list_menu, menu);
    }

    /**
     * Enables or disables listview animations. This simply toggles the
     *  adapter, initialising a new adapter if necessary.
     * @param baseAdapter The current adapter for the listview
     */
    public void toggleAnimations(BaseAdapter baseAdapter) {
        boolean anim = PrefsFragment.getAnimationPreference(mParent);
        if (mAnimationsEnabled != null && anim == mAnimationsEnabled) return;
        else mAnimationsEnabled = anim;

        /* If animations have been enabled, setup and use an animation adapter, otherwise use
         *  the regular adapter. The data should always be bound to mAdapter */
        BaseAdapter adapter = Tools.toggleAnimations(mAnimationsEnabled, mListView.getWrappedList(), mAnimAdapter, baseAdapter);
        if (mEndlessAdapter != null) {
            mEndlessAdapter.setParentAdatper(adapter);
        }

        if (mAnimationsEnabled) {
            mAnimAdapter = (SingleAnimationAdapter) adapter;
        }
    }

    public void markDirty() { mIsDirty = true; }

    public void markChangeAsSelected(String changeid) {
        if (mAdapter != null) mAdapter.setSelectedChangeId(changeid);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        if (args == null) {
            SearchQueryChanged ev = mEventBus.getStickyEvent(SearchQueryChanged.class);
            if (ev != null) {
                String to = ev.getClazzName();
                if (mParent.getClass().getSimpleName().equals(to))
                    args = ev.getBundle();
            }
        }

        if (args != null) {
            String databaseQuery = args.getString(SearchQueryChanged.KEY_WHERE);
            if (databaseQuery != null && !databaseQuery.isEmpty()) {
                if (args.getStringArrayList(SearchQueryChanged.KEY_BINDARGS) != null) {
                    /* Create a copy as the findCommits function can modify the contents of bindArgs
                     *  and we want each receiver to use the bindArgs from the original broadcast */
                    ArrayList<String> bindArgs = new ArrayList<>();
                    bindArgs.addAll(args.getStringArrayList(SearchQueryChanged.KEY_BINDARGS));
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

        if (sIsTabletMode) {
            // Broadcast that we have finished loading changes
            mEventBus.post(new ChangeLoadingFinished(getQuery()));
        }

        if (mEndlessAdapter != null) {
            mEndlessAdapter.notifyDataSetChanged();
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        ArrayList<SearchKeyword> keywords;
        String query;

        if (requestCode == RefineSearchActivity.REFINE_SEARCH_REQUEST) {
            if (resultCode == Activity.RESULT_OK){
                keywords = (ArrayList<SearchKeyword>) data.getSerializableExtra(RefineSearchActivity.SEARCH_KEYWORDS);
                if (mSearchView != null) {
                    mSearchView.injectKeywords(keywords);
                    setFilterCount();
                }
                query = data.getStringExtra(RefineSearchActivity.SEARCH_QUERY);
                if (!mSearchView.getQuery().toString().equals(query)) {
                    mSearchView.setQuery(query, true);
                }
            }
        }
    }

    public void onStartRefresh() {
        if (isAdded() && mSwipeLayout != null) {
            mSwipeLayout.setRefreshing(true);
        } else {
            mIsRefreshing = true;
        }
    }

    public void onStopRefresh() {
        if (isAdded() && mSwipeLayout != null) {
            mSwipeLayout.setRefreshing(false);
        } else {
            mIsRefreshing = false;
        }
    }

    private void toggleRefineSearchCard(boolean show) {
        LayoutInflater inflater = (LayoutInflater) mParent.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (show && !refineSearchShowing) {
            if (mRefineSearchCard == null) {
                mRefineSearchCard = inflater.inflate(R.layout.refine_search_card, null);
                mRefineSearchCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Set<SearchKeyword> keywords = mSearchView.getLastQuery();
                        Intent it = new Intent(mParent, RefineSearchActivity.class);
                        // The search query so the SearchView in the toolbar is copied over
                        it.putExtra(RefineSearchActivity.SEARCH_QUERY, mSearchView.getQuery());
                        // The active SearchKeyword filters to be bound to the SearchCategories
                        it.putParcelableArrayListExtra(RefineSearchActivity.SEARCH_KEYWORDS, new ArrayList<Parcelable>(keywords));
                        // Expect a result as we are processing the data of the activity here
                        startActivityForResult(it, RefineSearchActivity.REFINE_SEARCH_REQUEST);
                    }
                });
            }
            setFilterCount();
            refineSearchShowing = true;
            mListView.addHeaderView(mRefineSearchCard, null, true);
        } else if (!show && refineSearchShowing) {
            refineSearchShowing = false;
            mListView.removeHeaderView(mRefineSearchCard);
        }
    }

    private void setFilterCount() {
        TextView txtFilterCount = (TextView) mRefineSearchCard.findViewById(R.id.txtRefineSearchKeywordCount);
        int filterCount = mSearchView.getFilterCount();
        if (filterCount > 0) {
            txtFilterCount.setText(String.format(getString(R.string.refine_search_num_keywords), filterCount));
        } else {
            txtFilterCount.setText("");
        }
    }


    // Listen for processed search query changes
    @Keep @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onSearchQueryChanged(SearchQueryChanged ev) {
        // mEventBus.removeStickyEvent(ev);

        String to = ev.getClazzName();
        if (isAdded() && mParent.getClass().getSimpleName().equals(to)) {
            getLoaderManager().restartLoader(0, null, this);
        }
        // Let the drawer know that the search query was changed
        if (mParent instanceof BaseDrawerActivity) {
            ((BaseDrawerActivity) mParent).onSearchQueryChanged(ev);
        }
    }

    @Keep @Subscribe(threadMode = ThreadMode.MAIN)
    /* Tell the endless adapter we have finished loading when there was no data */
    public void onFinishedLoadingChanges(Finished ev) {
        if (!getQuery().equals(ev.getStatus())) {
            return;
        }

        onStopRefresh();

        Intent processed = ev.getIntent();
        Direction direction = (Direction) processed.getSerializableExtra(GerritService.CHANGES_LIST_DIRECTION);

        if (mEndlessAdapter != null) {
            if (direction == Direction.Newer) {
                // We loaded more changes so the data may have changed. finishedDataLoading is only for older changes
                mEndlessAdapter.notifyDataSetChanged();
            } else {
                mEndlessAdapter.finishedDataLoading();

                if (ev.getItems() < sChangesLimit) {
                    // Remove the endless adapter as we have no more older changes to load
                    // The scroll listener is only used for loading older changes
                    mListView.setOnScrollListener(null);
                }
            }
        }
    }

    @Keep @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartingLoadingChanges(StartingRequest ev) {
        if (getQuery().equals(ev.getStatus())) {
            onStartRefresh();
        }
    }

    @Keep @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectionError(ErrorDuringConnection ev) {
        if (getQuery().equals(ev.getStatus())) {
            onStopRefresh();
        }
    }

    @Keep @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSearchStateChanged(SearchStateChanged ev) {
        boolean show = ev.isSearchVisible();
        // Show the refine search card when their are filters (SearchKeywords) set
        if (!ev.isSearchVisible() && mSearchView.getFilterCount() > 0) show = true;
        toggleRefineSearchCard(show);
    }
}
