package com.jbirdvegas.mgerrit.activities;

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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.fragments.ChangelogFragment;
import com.jbirdvegas.mgerrit.objects.GooFileObject;
import com.jbirdvegas.mgerrit.search.AgeSearch;
import com.jbirdvegas.mgerrit.search.SearchKeyword;
import com.jbirdvegas.mgerrit.views.GerritSearchView;

import java.util.HashSet;

public class AOKPChangelog extends AppCompatActivity implements ChangelogActivity {

    private String mQuery = "https://goo.im/json2&path=/devs/aokp/" + Build.DEVICE + "/nightly";

    private GerritSearchView mSearchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setTheme(PrefsFragment.getCurrentThemeID(this));
        super.onCreate(savedInstanceState);

        setContentView(R.layout.aokp_changelog);

        // Action bar Up affordance
        getActionBar().setDisplayHomeAsUpEnabled(true);

        PrefsFragment.setGerritInstanceByName(this, "AOKP");

        ChangelogFragment frag;
        frag = (ChangelogFragment) getSupportFragmentManager().findFragmentById(R.id.changelog_fragment);
        frag.setQuery(mQuery);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (GerritSearchView) findViewById(R.id.search);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.changelog_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_save:
                Intent intent = new Intent(this, PrefsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                return true;
            case R.id.menu_search:
                // Toggle the visibility of the searchview
                mSearchView.toggleVisibility();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onBuildSelected(GooFileObject earlier, GooFileObject later) {
        HashSet<SearchKeyword> set = new HashSet<>(2);
        // if user selects the oldest build; earlier Object will be null
        if (earlier != null) {
            set.add(new AgeSearch(earlier.getModified(), ">="));
        }
        set.add(new AgeSearch(later.getModified(), "<="));
        mSearchView.injectKeywords(set);
    }
}
