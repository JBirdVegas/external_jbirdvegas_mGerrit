package com.jbirdvegas.mgerrit.views;

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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.support.v7.widget.SearchView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.message.SearchQueryChanged;
import com.jbirdvegas.mgerrit.search.OwnerSearch;
import com.jbirdvegas.mgerrit.search.ProjectSearch;
import com.jbirdvegas.mgerrit.search.SearchKeyword;
import org.jetbrains.annotations.Nullable;

import org.jetbrains.annotations.Contract;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

public class GerritSearchView extends SearchView
        implements SearchView.OnQueryTextListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "GerrritSearchView";
    private final SharedPreferences mPrefs;
    Context mContext;

    Set<SearchKeyword> mAdditionalKeywords;

    // The list of keyword tokens for the last processed query
    Set<SearchKeyword> mCurrentKeywords;

    public GerritSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setOnQueryTextListener(this);
        setupCancelButton();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        mCurrentKeywords = new HashSet<>();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        Integer user = PrefsFragment.getTrackingUser(mContext);
        if (user != null) {
            replaceKeyword(new OwnerSearch(user.toString()), true);
        }

        String project = PrefsFragment.getCurrentProject(mContext);
        if (project != null) {
            replaceKeyword(new ProjectSearch(project), true);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Set<SearchKeyword> tokens = constructTokens(query);
        if (tokens != null) {
            // If there is no project keyword in the query, it should be cleared
            if (SearchKeyword.findKeyword(tokens, ProjectSearch.class) < 0 &&
                    !PrefsFragment.getCurrentProject(mContext).isEmpty()) {
                PrefsFragment.setCurrentProject(mContext, null);
            }

            // If there is no owner keyword in the query, it should be cleared
            if (SearchKeyword.findKeyword(tokens, OwnerSearch.class) < 0 &&
                    PrefsFragment.getTrackingUser(mContext) != null) {
                PrefsFragment.clearTrackingUser(mContext);
            }
        }

        // Pass this on to the current CardsFragment instance
        if (!processTokens(tokens)) {
            Log.w(TAG, "Could not process query: " + query);
        }

        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Handled when the search is submitted instead.
        if (newText.isEmpty()) {
            onQueryTextSubmit(null);
        } else setVisibility(VISIBLE);
        return false;
    }

    /**
     * Set the search query. This will construct the SQL query and restart
     *  the loader to perform the query
     * @param query The search query text
     */
    private Set<SearchKeyword> constructTokens(@Nullable String query) {
        // An empty query will result in an empty set
        if (query == null || query.isEmpty()) {
            return new HashSet<>();
        }

        return SearchKeyword.constructTokens(query);
    }

    @Contract("null -> true")
    private boolean processTokens(final Set<SearchKeyword> tokens) {
        Set<SearchKeyword> newTokens = safeMerge(tokens, mAdditionalKeywords);
        mCurrentKeywords = newTokens;

        String where = "";
        ArrayList<String> bindArgs = new ArrayList<>();

        if (newTokens != null && !newTokens.isEmpty()) {
            where = SearchKeyword.constructDbSearchQuery(newTokens);
            if (where != null && !where.isEmpty()) {
                for (SearchKeyword token : newTokens) {
                    bindArgs.addAll(Arrays.asList(token.getEscapeArgument()));
                }
            } else {
                return false;
            }
        }

        EventBus.getDefault().postSticky(new SearchQueryChanged(where, bindArgs,
                getContext().getClass().getSimpleName(), tokens));
        return true;
    }

    /**
     * Always show the cancel button and set its onClick listener. The button
     *  has private visibility so we need reflection to access it.
     */
    private void setupCancelButton() {
        try {
            Field searchField = SearchView.class.getDeclaredField("mCloseButton");
            searchField.setAccessible(true);
            ImageView closeBtn = (ImageView) searchField.get(this);
            closeBtn.setVisibility(VISIBLE);
            closeBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleVisibility();
                }
            });
        } catch (Exception e) {
            EasyTracker.getInstance(mContext).send(MapBuilder.createEvent(
                    "GerritSearchView",
                    "setupCancelButton",
                    "search_button_reflection_visibility",
                    null).build());
            e.printStackTrace();
        }
    }

    public void toggleVisibility() {
        int visibility = getVisibility();
        if (visibility == View.GONE) {
            setVisibility(View.VISIBLE);
            requestFocus();
        }
        else setVisibility(View.GONE);
    }

    @Override
    public void setVisibility(int visibility) {
        String query = getQuery().toString();
        if (!query.isEmpty() && visibility == GONE) setQuery("", true);
        super.setVisibility(visibility);
        setIconified(visibility == GONE);
    }

    /**
     * Modifies future searches for this fragment by appending additional
     *  keywords to search for that will not be present in the original
     *  search query. This clears all old keywords that were previously injected.
     *
     *  Used for the changelog
     * @param keywords
     */
    public void injectKeywords(Set<SearchKeyword> keywords) {
        mAdditionalKeywords = new HashSet<>(keywords);
        onQueryTextSubmit(getQuery().toString()); // Force search refresh
    }

    /**
     * Add the elements of otherSet to oldSet and return a new set.
     */
    private Set<SearchKeyword> safeMerge(Set<SearchKeyword> oldSet, Set<SearchKeyword> otherSet) {
        HashSet<SearchKeyword> newSet = new HashSet<>();
        if (oldSet != null && !oldSet.isEmpty()) {
            newSet.addAll(oldSet);
        }
        if (otherSet != null && !otherSet.isEmpty()) {
            newSet.addAll(otherSet);
        }
        return newSet;
    }

    public void replaceKeyword(SearchKeyword keyword, boolean submit) {
        String currentQuery = getQuery().toString();
        String query = SearchKeyword.replaceKeyword(currentQuery, keyword);
        if (!query.equals(currentQuery)) this.setQuery(query, submit);
    }

    /**
     * @return The list of search keywords that were included in the query plus any additional
     *  keywords that were set via injectKeywords(Set<SearchKeyword>)
     */
    public Set<SearchKeyword> getLastQuery() {
        return mCurrentKeywords;
    }


    /**
     * Search for a given search keyword in the current list of tokens
     * @param keyword The search keyword to search for (needle)
     * @return Whether the keyword was found in the list or not
     */
    public boolean hasKeyword(SearchKeyword keyword) {
        return SearchKeyword.findKeyword(mCurrentKeywords, keyword) != -1;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PrefsFragment.CURRENT_PROJECT:
                replaceKeyword(new ProjectSearch(PrefsFragment.getCurrentProject(mContext)), true);
                break;
            case PrefsFragment.TRACKING_USER:
                replaceKeyword(new OwnerSearch(PrefsFragment.getTrackingUser(mContext)), true);
                break;
        }
    }
}
