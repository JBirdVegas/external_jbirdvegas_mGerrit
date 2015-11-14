package com.jbirdvegas.mgerrit.activities;

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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.adapters.ProjectsListAdapter;
import com.jbirdvegas.mgerrit.database.ProjectsTable;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.StartingRequest;
import com.jbirdvegas.mgerrit.tasks.GerritService;

import java.io.Serializable;

import de.greenrobot.event.EventBus;

public class ProjectsList extends BaseDrawerActivity
    implements LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener
{
    ExpandableListView mProjectsListView;
    ProjectsListAdapter mListAdapter;
    private EventBus mEventBus;
    private String mQuery;

    private static final String SEPERATOR = ProjectsTable.SEPERATOR;
    private static final int LOADER_PROJECTS = 0;

    private SwipeRefreshLayout mSwipeLayout;
    private SwipeRefreshLayout.OnRefreshListener mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            startService();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.setTheme(PrefsFragment.getCurrentThemeID(this));
        super.onCreate(savedInstanceState);

        setContentView(R.layout.projects_list);

        initNavigationDrawer(false);

        mProjectsListView = (ExpandableListView) findViewById(R.id.projects);

        mProjectsListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id)
            {
                TextView tv = (TextView) v;
                String subgroup = tv.getText().toString();
                String root = mListAdapter.getGroupName(groupPosition);
                setProject(root, subgroup);
                return true;
            }
        });

        mProjectsListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                TextView tv = (TextView) v;
                String group = tv.getText().toString();
                if (mListAdapter.getChildrenCount(groupPosition) < 1) {
                    setProject(group, "");
                    return true;
                }
                return false;
            }
        });

        setSwipeRefreshLayout();

        handleIntent(this.getIntent());

        mEventBus = EventBus.getDefault();

        navigationSetSelectedById(R.id.menu_projects);

        // Todo: We don't always need to query the server here
        startService();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // Searching is already handled when the query text changes.
        if (!Intent.ACTION_SEARCH.equals(intent.getAction()))
            loadAdapter();
    }

    @Override
    protected Toolbar setupActionBar() {
        Toolbar toolbar = super.setupActionBar();
        // Action bar Up affordance
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.menu_projects);
        return toolbar;
    }

    private void loadAdapter(){
        // Set the adapter for the list view
        mListAdapter = new ProjectsListAdapter(this,
                new String[] { ProjectsTable.C_ROOT }, // Name for group layouts
                new int[] { android.R.id.text1 },
                new String[] { ProjectsTable.C_SUBPROJECT }, // Name for child layouts
                new int[] { android.R.id.text1 });
        mProjectsListView.setAdapter(mListAdapter);

        getSupportLoaderManager().initLoader(LOADER_PROJECTS, null, this);
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
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(this);

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

    @Override
    protected void onResume() {
        super.onResume();
        mEventBus.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mEventBus.unregister(this);
    }

    private void setSwipeRefreshLayout() {
        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeLayout.setColorSchemeResources(R.color.text_orange, R.color.text_green, R.color.text_red,
                android.R.color.transparent);
        mSwipeLayout.setOnRefreshListener(mRefreshListener);
        mSwipeLayout.setEnabled(false);
    }

    private void setProject(String root, String subproject) {
        String project;
        if (subproject.isEmpty()) project = root;
        else project = root + SEPERATOR + subproject;
        PrefsFragment.setCurrentProject(getApplicationContext(), project);
        this.finish();
    }

    private void startService() {
        Intent it = new Intent(this, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Project);
        startService(it);
    }

    /* We load the data on a separate thread (AsyncTaskLoader) but what to do
     *  on the main thread? Probably best to block (with a alert dialog) like
     *  the old implementation did, then unblock once all the data has been
     *  downloaded and we can start binding data to views.
     */

    // Note: Using the platform Loader here (android.app.Loader)
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_PROJECTS) {
            return ProjectsTable.getProjects(this, mQuery);
        } else {
            return super.onCreateLoader(id, args);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == LOADER_PROJECTS) {
            mListAdapter.setSubprojectQuery(mQuery);
            mListAdapter.changeCursor(cursor);
            mSwipeLayout.setRefreshing(false);
        } else {
            super.onLoadFinished(loader, cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LOADER_PROJECTS) {
            mListAdapter.changeCursor(null);
        } else {
            super.onLoaderReset(loader);
        }
    }

    /**
     * Split the query as if it was the name (or part) of a base project,
     *  a sub project or base project/sub project. Note that providing the
     *  seperator in the query will change the results that are provided.
     * @param query The (partial) name of a base project, sub project or both.
     * @return A pair comprised of a base project and sub project matching the query
     *  to search for
     */
    private Pair<String, String> splitQuery(String query) {
        String p[] = query.split(SEPERATOR, 2);
        if (p.length < 2) return new Pair<>(p[0], p[0]);
        else return new Pair<>(p[0], p[1]);
    }

    @Override
    public boolean onQueryTextChange(String query) {
        mQuery = query;
        getSupportLoaderManager().restartLoader(LOADER_PROJECTS, null, this);
        int sizeOfGroups = mListAdapter.getGroupCount();
        for (int i = 0; sizeOfGroups > i; i++) {
            mProjectsListView.expandGroup(i);
        }
        return true; // Don't have support for suggestions yet.
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        /*
         Although the submit request is handled when the query changes,
         *  return false here to hide the soft keyboard.
         */
        return false;
    }

    public void onEventMainThread(StartingRequest ev) {
        Intent intent = ev.getIntent();
        if (intent.getSerializableExtra(GerritService.DATA_TYPE_KEY) == GerritService.DataType.Project) {
            mSwipeLayout.setRefreshing(true);
        }
    }

    public void onEventMainThread(Finished ev) {
        Intent intent = ev.getIntent();
        Serializable dataType = ev.getIntent().getSerializableExtra(GerritService.DATA_TYPE_KEY);
        if (ev.getItems() < 1 && dataType == GerritService.DataType.Project) {
            mSwipeLayout.setRefreshing(false);
        }
    }

    public void onEventMainThread(ErrorDuringConnection ev) {
        Intent intent = ev.getIntent();
        if (intent.getSerializableExtra(GerritService.DATA_TYPE_KEY) == GerritService.DataType.Project) {
            mSwipeLayout.setRefreshing(false);
        }
    }
}
