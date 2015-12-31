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

import android.app.SearchManager;
import android.content.Context;
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
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.search.BranchCategory;
import com.jbirdvegas.mgerrit.search.SearchCategory;
import com.jbirdvegas.mgerrit.search.TopicCategory;

import java.util.ArrayList;

public class RefineSearchActivity extends AppCompatActivity {
    private ListView mCategoriesListView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.setTheme(PrefsFragment.getCurrentThemeID(this));
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search_categories);

        setupActionBar();

        mCategoriesListView = (ListView) findViewById(R.id.lv_search_categories);

        mCategoriesListView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        // Action bar Up affordance
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void loadAdapter(){
        ArrayList<SearchCategory> categories = new ArrayList<>();
        categories.add(new BranchCategory());
        categories.add(new TopicCategory());
    }

    // Source: http://developer.android.com/guide/topics/search/search-dialog.html
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.projects_menu, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);

        return true;
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
}
