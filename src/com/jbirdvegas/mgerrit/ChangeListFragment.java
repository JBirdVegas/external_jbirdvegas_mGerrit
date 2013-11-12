package com.jbirdvegas.mgerrit;

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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.jbirdvegas.mgerrit.message.StatusSelected;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.search.OwnerSearch;
import com.jbirdvegas.mgerrit.search.ProjectSearch;
import com.jbirdvegas.mgerrit.search.SearchKeyword;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ChangeListFragment extends Fragment
        implements SearchView.OnQueryTextListener {

    private static final String TAG = ChangeListFragment.class.getSimpleName();
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private FragmentActivity mParent;
    private View mThisFragment;
    private ArrayList<CharSequence> mTitles;

    // This should be set to the status corresponding to the initially selected tab
    private String mSelectedStatus = JSONCommit.Status.NEW.toString();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.change_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        setupTabs();
    }

    /** MUST BE CALLED ON MAIN THREAD */
    private void setupTabs() {

        mParent = getActivity();
        mThisFragment = this.getView();

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(mParent.getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        /** The {@link android.support.v4.view.ViewPager} that will host the section contents. */
        mViewPager = (ViewPager) mThisFragment.findViewById(R.id.tabs);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager
                .setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
                {
                    @Override
                    public void onPageSelected(int position)
                    {
                        String status = mSectionsPagerAdapter.getStatusAtPostion(position);
                        new StatusSelected(mParent, status).sendUpdateMessage();
                        mSelectedStatus = status;
                        CardsFragment fragment = mSectionsPagerAdapter.getFragment(position);
                        if (fragment == null) {
                            // It may just have not been instantiated yet
                            fragment = (CardsFragment) mSectionsPagerAdapter.getItem(position);
                            if (fragment == null) {
                                Log.e(TAG, String.format("Cannot refresh the page selected at position %d", position));
                                return; // We cannot do any more here
                            }
                        }
                        fragment.refresh(false);
                    }
                });

        mTitles = new ArrayList<>();
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++)
            mTitles.add(mSectionsPagerAdapter.getPageTitle(i));
    }

    public void refreshTabs() {
        mSectionsPagerAdapter.refreshTabs();
    }

    public CardsFragment getCurrentFragment() {
        return mSectionsPagerAdapter.getCurrentFragment();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Set<SearchKeyword> tokens = constructTokens(query);
        if (tokens == null) {
            Log.w(TAG, "Could not process query: " + query);
        } else {
            // If there is no project keyword in the query, it should be cleared
            if (SearchKeyword.findKeyword(tokens, ProjectSearch.class) < 0 &&
                    !Prefs.getCurrentProject(mParent).equals("")) {
                Prefs.setCurrentProject(mParent, null);
            }

            // If there is no owner keyword in the query, it should be cleared
            if (SearchKeyword.findKeyword(tokens, OwnerSearch.class) < 0 &&
                    Prefs.getTrackingUser(mParent) != null) {
                Prefs.clearTrackingUser(mParent);
            }

            // Pass this on to the current CardsFragment instance
            if (!processTokens(tokens)) {
                Log.w(TAG, "Could not process query: " + query);
            }
        }

        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Handled when the search is submitted instead.
        if (newText.isEmpty()) {
            onSearchQueryCleared();
            // This should be called last and should always be processable
            processTokens(null);
        }
        return false;
    }

    public String getStatus() {
        return mSelectedStatus;
    }


    /**
     * Set the search query. This will construct the SQL query and restart
     *  the loader to perform the query
     * @param query The search query text
     */
    public Set<SearchKeyword> constructTokens(String query) {
        // Clear any previous searches that where made
        if (query == null || query.isEmpty()) {
            return new HashSet<>();
        }

        return SearchKeyword.constructTokens(query);
    }

    // Additional logic to be run when the search query is empty
    private void onSearchQueryCleared() {
        Prefs.setCurrentProject(mParent, null);
        Prefs.clearTrackingUser(mParent);
    }

    public boolean processTokens(Set<SearchKeyword> tokens) {
        Bundle bundle = new Bundle();

        if (tokens != null && !tokens.isEmpty()) {
            String where = SearchKeyword.constructDbSearchQuery(tokens);
            if (where != null && !where.isEmpty()) {
                ArrayList<String> bindArgs = new ArrayList<>();
                for (SearchKeyword token : tokens) {
                    bindArgs.addAll(Arrays.asList(token.getEscapeArgument()));
                }

                bundle.putString("WHERE", where);
                bundle.putStringArrayList("BIND_ARGS", bindArgs);
            } else {
                return false;
            }
        }
        Intent intent = new Intent(CardsFragment.SEARCH_QUERY);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(mParent).sendBroadcast(intent);
        return true;
    }


    /**
     * A {@link android.support.v4.app.FragmentStatePagerAdapter} that returns a
     * fragment corresponding to one of the sections/tabs/pages.
     */
    class SectionsPagerAdapter extends FragmentStatePagerAdapter
    {
        public int mPageCount = 3;

        ReviewTab mReviewTab = null;
        MergedTab mMergedTab = null;
        AbandonedTab mAbandonedTab = null;

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public String getStatusAtPostion(int position) {
            switch (position) {
                case 0: return JSONCommit.KEY_STATUS_OPEN;
                case 1: return JSONCommit.KEY_STATUS_MERGED;
                case 2: return JSONCommit.KEY_STATUS_ABANDONED;
                default: return null;
            }
        }

        @Override
        /** Called to instantiate the fragment for the given page.
         * IMPORTANT: Do not use this to monitor the currently selected page as it is used
         *  to load neighbouring tabs that may not be selected. */
        public Fragment getItem(int position) {
            CardsFragment fragment;

            switch (position) {
                case 0:
                    fragment = new ReviewTab();
                    mReviewTab = (ReviewTab) fragment;
                    break;
                case 1:
                    fragment = new MergedTab();
                    mMergedTab = (MergedTab) fragment;
                    break;
                case 2:
                    fragment = new AbandonedTab();
                    mAbandonedTab = (AbandonedTab) fragment;
                    break;
                default: return null;
            }

            return fragment;
        }

        // The ViewPager monitors the current tab position so we can get the
        //  ViewPager from the enclosing class and use the fragment recording
        //  to get the current fragment
        public CardsFragment getCurrentFragment() {
            int pos = mViewPager.getCurrentItem();
            return getFragment(pos);
        }

        public CardsFragment getFragment(int pos) {
            switch (pos) {
                case 0: return mReviewTab;
                case 1: return mMergedTab;
                case 2: return mAbandonedTab;
                default: return null;
            }
        }

        @Override
        /** Return the number of views available. */
        public int getCount() { return mPageCount; }

        @Override
        /** Called by the ViewPager to obtain a title string to describe
         *  the specified page. */
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.reviewable);
                case 1: return getString(R.string.merged);
                case 2: return getString(R.string.abandoned);
            }
            return null;
        }

        private void refreshTabs() {
            if (mReviewTab != null) mReviewTab.markDirty();
            if (mMergedTab != null) mMergedTab.markDirty();
            if (mAbandonedTab != null) mAbandonedTab.markDirty();
            // Its possible the current fragment may be null... if that happens
            // reload the page
            CardsFragment currentFragment = getCurrentFragment();
            if (currentFragment == null) {
                onCreate(null);
            } else {
                currentFragment.refresh(true);
            }
        }
    }
}
