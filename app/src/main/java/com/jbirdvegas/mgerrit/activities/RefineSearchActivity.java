/*
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2015
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

package com.jbirdvegas.mgerrit.activities;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.adapters.SearchCategoryAdapter;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.search.SearchKeyword;
import com.jbirdvegas.mgerrit.search.categories.AfterCategory;
import com.jbirdvegas.mgerrit.search.categories.BeforeCategory;
import com.jbirdvegas.mgerrit.search.categories.BranchCategory;
import com.jbirdvegas.mgerrit.search.categories.OwnerCategory;
import com.jbirdvegas.mgerrit.search.categories.ProjectCategory;
import com.jbirdvegas.mgerrit.search.categories.SearchCategory;
import com.jbirdvegas.mgerrit.search.categories.StarredCategory;
import com.jbirdvegas.mgerrit.search.categories.TopicCategory;
import com.jbirdvegas.mgerrit.tasks.GerritService;

import java.util.ArrayList;
import java.util.Collection;

public class RefineSearchActivity extends AppCompatActivity {
    private ListView mCategoriesListView;
    private SearchCategoryAdapter mAdapter;
    private SearchView mSearchView;

    public static final String SEARCH_QUERY = "search_query";
    public static final String SEARCH_KEYWORDS = "search_keywords";
    public static final int REFINE_SEARCH_REQUEST = 13;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.setTheme(PrefsFragment.getCurrentThemeID(this));
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search_categories);

        setupActionBar();

        mCategoriesListView = (ListView) findViewById(R.id.lv_search_categories);

        View view = getLayoutInflater().inflate(R.layout.item_textview_header, mCategoriesListView, false);
        mCategoriesListView.addHeaderView(view, null, false);

        loadAdapter();

        // Update the list of projects for the project autocomplete
        startService(GerritService.DataType.Project);
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        // Action bar Up affordance
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void loadAdapter() {
        ArrayList<SearchCategory> categories = new ArrayList<>();
        categories.add(new BranchCategory());
        categories.add(new OwnerCategory());
        categories.add(new ProjectCategory());
        categories.add(new StarredCategory());
        categories.add(new TopicCategory());
        categories.add(new AfterCategory());
        categories.add(new BeforeCategory());

        Collection<SearchKeyword> keywords = getIntent().getParcelableArrayListExtra(SEARCH_KEYWORDS);
        SearchCategory.bindKeywordsToCategories(this, categories, keywords);

        mAdapter = new SearchCategoryAdapter(this, R.layout.item_search_category, categories);
        mCategoriesListView.setAdapter(mAdapter);
    }

    // Source: http://developer.android.com/guide/topics/search/search-dialog.html
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.projects_menu, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        mSearchView.setQueryHint(getString(R.string.changes_search_hint));
        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        if (getParent() instanceof GerritControllerActivity) {
            // The main GerritControllerActivity handles searching
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getParent().getComponentName()));
        }
        CharSequence searchQuery = getIntent().getCharSequenceExtra(SEARCH_QUERY);
        if (searchQuery != null && searchQuery.length() > 0) {
            mSearchView.setQuery(searchQuery, false);
        }
        mSearchView.setIconifiedByDefault(false);

        return true;
    }

    public void onClear(View view) {
        mAdapter.clear();
        returnResult(null);
        this.finish();
    }

    public void onApply(View view) {
        returnResult(mAdapter.getKeywords());
        this.finish();
    }

    private void returnResult(ArrayList<SearchKeyword> keywords) {
        Intent returnIntent = new Intent();
        if (mSearchView != null) {
            returnIntent.putExtra(SEARCH_QUERY, mSearchView.getQuery().toString());
        }
        returnIntent.putExtra(SEARCH_KEYWORDS, keywords);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startService(GerritService.DataType dataType) {
        Intent it = new Intent(this, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, dataType);
        startService(it);
    }
}
