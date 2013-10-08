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

import android.app.Activity;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ExpandableListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.adapters.ProjectsListAdapter;
import com.jbirdvegas.mgerrit.database.ProjectsTable;
import com.jbirdvegas.mgerrit.listeners.DefaultGerritReceivers;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.StartingRequest;
import com.jbirdvegas.mgerrit.objects.GerritURL;
import com.jbirdvegas.mgerrit.tasks.GerritService;

public class ProjectsList extends Activity
    implements LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener
{
    ExpandableListView mProjectsListView;
    ProjectsTable mProjectsTable;
    ProjectsListAdapter mListAdapter;
    private DefaultGerritReceivers receivers;
    private String mQuery;

    private static final String SEPERATOR = ProjectsTable.SEPERATOR;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.projects_list);

        // Action bar Up affordance
        getActionBar().setDisplayHomeAsUpEnabled(true);

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

        handleIntent(this.getIntent());

        receivers = new DefaultGerritReceivers(this);

        // Todo: We don't always need to query the server here
        startService();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent)
    {
        // Searching is already handled when the query text changes.
        if (!Intent.ACTION_SEARCH.equals(intent.getAction()))
            loadAdapter();
    }

    private void loadAdapter()
    {
        // Set the adapter for the list view
        mListAdapter = new ProjectsListAdapter(this,
                android.R.layout.simple_expandable_list_item_1,
                new String[] { ProjectsTable.C_ROOT }, // Name for group layouts
                new int[] { android.R.id.text1 },
                R.layout.projects_subproject_row,
                new String[] { ProjectsTable.C_SUBPROJECT }, // Name for child layouts
                new int[] { android.R.id.text1 });
        mProjectsListView.setAdapter(mListAdapter);

        getLoaderManager().initLoader(0, null, this);
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

    // Register to receive messages.
    private void registerReceivers() {
        receivers.registerReceivers(StartingRequest.TYPE,
                Finished.TYPE,
                ErrorDuringConnection.TYPE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceivers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        receivers.unregisterReceivers();
    }

    private void setProject(String root, String subproject) {
        String project;
        if (subproject.equals("")) project = root;
        else project = root + SEPERATOR + subproject;
        Prefs.setCurrentProject(getApplicationContext(), project);
        ProjectsList.this.finish();
    }

    private void startService() {

        GerritURL url = new GerritURL();

        // This is just a precaution in case it has not been set yet
        GerritURL.setGerrit(Prefs.getCurrentGerrit(this));
        url.listProjects();

        Intent it = new Intent(this, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Project);
        it.putExtra(GerritService.URL_KEY, url);
        startService(it);
    }

    /* We load the data on a seperate thread (AsyncTaskLoader) but what to do
     *  on the main thread? Probably best to block (with a alert dialog) like
     *  the old implementation did, then unblock once all the data has been
     *  downloaded and we can start binding data to views.
     */

    // Note: Using the platform Loader here (android.app.Loader)
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ProjectsTable.getProjects(this, mQuery);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mListAdapter.setSubprojectQuery(mQuery);
        mListAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mListAdapter.changeCursor(null);
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
        if (p.length < 2) return new Pair<String, String>(p[0], p[0]);
        else return new Pair<String, String>(p[0], p[1]);
    }

    @Override
    public boolean onQueryTextChange(String query) {
        mQuery = query;
        getLoaderManager().restartLoader(0, null, this);
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
}
