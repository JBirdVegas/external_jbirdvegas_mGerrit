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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import java.util.ArrayList;

public class ChangeListFragment extends Fragment
    implements SearchView.OnQueryTextListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private FragmentActivity mParent;
    private View mThisFragment;
    private ArrayList<CharSequence> mTitles;

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
                        mSectionsPagerAdapter.getFragment(position).refresh(false);
                    }
                });

        mTitles = new ArrayList<CharSequence>();
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
        // Pass this on to the current CardsFragment instance
        getCurrentFragment().setSearchQuery(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Handled when the search is submitted instead.
        if (newText.isEmpty()) {
            getCurrentFragment().setSearchQuery("");
        }
        return false;
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
