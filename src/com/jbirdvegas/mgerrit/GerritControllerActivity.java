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

import android.app.AlertDialog.Builder;
import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

import com.jbirdvegas.mgerrit.database.SelectedChange;
import com.jbirdvegas.mgerrit.helpers.ROMHelper;
import com.jbirdvegas.mgerrit.listeners.DefaultGerritReceivers;
import com.jbirdvegas.mgerrit.message.ConnectionEstablished;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.EstablishingConnection;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.HandshakeError;
import com.jbirdvegas.mgerrit.message.InitializingDataTransfer;
import com.jbirdvegas.mgerrit.message.ProgressUpdate;
import com.jbirdvegas.mgerrit.objects.GerritURL;
import com.jbirdvegas.mgerrit.objects.GooFileObject;
import com.jbirdvegas.mgerrit.search.OwnerSearch;
import com.jbirdvegas.mgerrit.search.ProjectSearch;
import com.jbirdvegas.mgerrit.search.SearchKeyword;
import com.jbirdvegas.mgerrit.tasks.GerritTask;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class GerritControllerActivity extends FragmentActivity {

    private static final String TAG = GerritControllerActivity.class.getSimpleName();
    private static final String GERRIT_INSTANCE = "gerrit";
    private String mGerritWebsite;
    private GooFileObject mChangeLogStart;
    private GooFileObject mChangeLogStop;

    /**
     * Keep track of all the GerritTask instances so the dialog can be dismissed
     *  when this activity is paused.
     */
    private Set<GerritTask> mGerritTasks;

    // Listener for changes to which commit is selected
    private BroadcastReceiver mChangeListener;

    private DefaultGerritReceivers receivers;

    // This is maintained for checking if the project has changed without looking
    private String mCurrentProject;
    private Menu mMenu;

    // Indicates if we are running this in tablet mode.
    private boolean mTwoPane;
    private ChangeListFragment mChangeList;

    // This will be null if mTwoPane is false (i.e. not tablet mode)
    private PatchSetViewerFragment mChangeDetail;

    private SearchView searchView;
    // Wrapper around searchView for modifying searchView before it is initialised
    private SearchViewProperties mSearchViewProperties = new SearchViewProperties();
    private int mTheme;


    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);

        registerReceivers();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);

        receivers.unregisterReceivers();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mChangeListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mTheme = Prefs.getCurrentThemeID(this);
        setTheme(mTheme);

        super.onCreate(savedInstanceState);

        // check if caller has a gerrit instance start screen preference
        String suppliedGerritInstance = getIntent().getStringExtra(GERRIT_INSTANCE);
        if (suppliedGerritInstance != null
                && !suppliedGerritInstance.isEmpty()
                && suppliedGerritInstance.contains("http")) {
            // just set the prefs and allow normal loading
            Prefs.setCurrentGerrit(this, suppliedGerritInstance);
        }

        GoogleAnalytics googleAnalytics = GoogleAnalytics.getInstance(this);

        String trackingId = getString(R.string.ga_trackingId);
        Tracker tracker = googleAnalytics.getTracker(trackingId);
        googleAnalytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
        tracker.send(MapBuilder
                .createAppView().build());

        // keep a log of what ROM our users run
        EasyTracker easyTracker = EasyTracker.getInstance(this);
        easyTracker.send(MapBuilder
                .createEvent(AnalyticsConstants.GA_APP_OPEN,     // Event category (required)
                        AnalyticsConstants.GA_ROM_VERSION,  // Event action (required)
                        ROMHelper.determineRom(this),   // Event label
                        null)            // Event value (long)
                .build());
        // note this screen as viewed
        easyTracker.send(MapBuilder
                .createAppView().build());

        // Keep track of what theme is being used
        easyTracker.send(MapBuilder
                .createEvent(AnalyticsConstants.GA_APP_OPEN,
                        AnalyticsConstants.GA_THEME_SET_ON_OPEN,
                        Prefs.getCurrentTheme(this),
                        null)
                .build());
        // note this screen as viewed
        easyTracker.send(MapBuilder.createAppView().build());

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);

        FragmentManager fm = getSupportFragmentManager();
        if (findViewById(R.id.change_detail_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
            mChangeDetail = (PatchSetViewerFragment) fm.findFragmentById(R.id.change_detail_fragment);
            // TODO: In two-pane mode, list items should be given the 'activated' state when touched.
        }

        mChangeList = (ChangeListFragment) fm.findFragmentById(R.id.change_list_fragment);

        mGerritWebsite = Prefs.getCurrentGerrit(this);

        /* Initially set the current Gerrit globally here.
         *  We can rely on callbacks to know when they change */
        GerritURL.setGerrit(Prefs.getCurrentGerrit(this));
        GerritURL.setProject(Prefs.getCurrentProject(this));

        // Don't register listeners here. It is registered in onResume instead.
        mChangeListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String changeid = intent.getStringExtra(PatchSetViewerFragment.CHANGE_ID);
                String status = intent.getStringExtra(PatchSetViewerFragment.STATUS);
                boolean expand = intent.getBooleanExtra(PatchSetViewerFragment.EXPAND_TAG, false);
                onChangeSelected(changeid, status, expand);
            }
        };

        handleIntent(this.getIntent());
    }

    private void init() {
        // ensure we are not tracking a project unintentionally
        if ("".equals(Prefs.getCurrentProject(this))) {
            Prefs.setCurrentProject(this, null);
        }
        mCurrentProject = Prefs.getCurrentProject(this);

        try {
            mChangeLogStart = getIntent()
                    .getExtras()
                    .getParcelable(AOKPChangelog.KEY_CHANGELOG_START);
            mChangeLogStop = getIntent()
                    .getExtras()
                    .getParcelable(AOKPChangelog.KEY_CHANGELOG_STOP);
        } catch (NullPointerException npe) {
            Log.d(TAG, "Changelog was null");
        }

        mGerritTasks = new HashSet<>();

        receivers = new DefaultGerritReceivers(this);
    }

    // Register to receive messages.
    private void registerReceivers() {
        if (receivers == null) {
            receivers = new DefaultGerritReceivers(this);
        }
        receivers.registerReceivers(EstablishingConnection.TYPE,
                ConnectionEstablished.TYPE,
                InitializingDataTransfer.TYPE,
                ProgressUpdate.TYPE,
                Finished.TYPE,
                HandshakeError.TYPE,
                ErrorDuringConnection.TYPE);

        LocalBroadcastManager.getInstance(this).registerReceiver(mChangeListener,
                new IntentFilter(PatchSetViewerFragment.NEW_CHANGE_SELECTED));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        hideChangelogOption(Prefs.getCurrentGerrit(this));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gerrit_instances_menu, menu);
        this.mMenu = menu;

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);
        // Let the change list fragment handle queries directly.
        searchView.setOnQueryTextListener(mChangeList);
        searchView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                SearchView view = (SearchView) v;
                if (view.isIconified()) {
                    mMenu.findItem(R.id.menu_team_instance).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                    mMenu.findItem(R.id.menu_projects).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                } else {
                    mMenu.findItem(R.id.menu_team_instance).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    mMenu.findItem(R.id.menu_projects).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }
            }
        });

        setupSearchQuery();
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent)
    {
        String action = intent.getAction();
        if (TheApplication.PREF_CHANGE_TYPE.equals(action)) {
            onPreferenceChanged(intent.getStringExtra(TheApplication.PREF_CHANGE_KEY));
        } else if (!Intent.ACTION_SEARCH.equals(action)) {
            // Searching is already handled when the query text changes.
            init();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_save:
                intent = new Intent(this, PrefsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                return true;
            case R.id.menu_help:
                Builder builder = new Builder(this);
                builder.setTitle(R.string.menu_help);
                LayoutInflater layoutInflater = this.getLayoutInflater();
                View dialog = layoutInflater.inflate(R.layout.dialog_help, null);
                builder.setView(dialog);
                builder.create();
                builder.show();
                return true;
            case R.id.menu_refresh:
                refreshTabs();
                return true;
            case R.id.menu_team_instance:
                DialogFragment newFragment = new GerritSwitcher();
                String tag = getResources().getString(R.string.choose_gerrit_instance);
                // Must use getFragmentManager not getSupportFragmentManager here
                newFragment.show(getFragmentManager(), tag);
                return true;
            case R.id.menu_projects:
                intent = new Intent(this, ProjectsList.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                return true;
            case R.id.menu_changelog:
                Intent changelog = new Intent(this,
                        AOKPChangelog.class);
                changelog.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(changelog);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onGerritChanged(String newGerrit) {
        mGerritWebsite = newGerrit;
        Toast.makeText(this,
                getString(R.string.using_gerrit_toast) + ' ' + newGerrit,
                Toast.LENGTH_LONG).show();
        hideChangelogOption(newGerrit);

        // Unset the project - we don't track these across Gerrit instances
        Prefs.setCurrentProject(this, null);
        Prefs.clearTrackingUser(this);

        refreshTabs();
    }

    private void onProjectChanged(String newProject) {
        String query = getSearchQuery();
        query = SearchKeyword.replaceKeyword(query, new ProjectSearch(newProject));
        mSearchViewProperties.setQuery(query, true);
        mCurrentProject = newProject;
    }

    private void onUserTrackingChanged(Integer userTracking) {
        String query = getSearchQuery();
        String user = userTracking == null ? "" : userTracking.toString();
        query = SearchKeyword.replaceKeyword(query, new OwnerSearch(user));
        mSearchViewProperties.setQuery(query, true);
    }

    /* Mark all of the tabs as dirty to trigger a refresh when they are next
     *  resumed. refresh must be called on the current fragment as it is already
     *  resumed.
     */
    public void refreshTabs() {
        mChangeList.refreshTabs();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mGerritTasks != null) {
            Iterator<GerritTask> it = mGerritTasks.iterator();
            while (it.hasNext()) {
                GerritTask gerritTask = it.next();
                if (gerritTask.getStatus() == AsyncTask.Status.FINISHED) {
                    it.remove();
                }
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Manually check if the Gerrit source changed (from the Preferences)
        String s = Prefs.getCurrentGerrit(this);
        if (!s.equals(mGerritWebsite)) onGerritChanged(s);

        // Manually check if the project changed (e.g. we are resuming from the Projects List)
        s = Prefs.getCurrentProject(this);
        if (!s.equals(mCurrentProject)) onProjectChanged(s);

        // Apply the theme if it has changed
        int themeId = Prefs.getCurrentThemeID(this);
        if (themeId != mTheme) {
            mTheme = themeId;
            setTheme(themeId);
            this.recreate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // if screen gets disposed of before mGerritTasks is initialized
        if (mGerritTasks != null) {
            for (GerritTask gerritTask : mGerritTasks) {
                gerritTask.cancel(true);
            }
            mGerritTasks.clear();
            mGerritTasks = null;
        }
    }

    // Hide the AOKP Changelog menu option when AOKP's Gerrit is not selected
    private void hideChangelogOption(String gerrit) {
        MenuItem changelog = mMenu.findItem(R.id.menu_changelog);
        if (gerrit.contains("aokp")) {
            changelog.setVisible(true);
        } else {
            changelog.setVisible(false);
        }
    }

    /**
     * Handler for when a change is selected in the list.
     * @param changeID The currently selected change ID
     * @param expand Whether to expand the change and view the change details.
     *               Relevant only to the tablet layout.
     */
    public void onChangeSelected(String changeID, String status, boolean expand) {
        Bundle arguments = new Bundle();
        arguments.putString(PatchSetViewerFragment.CHANGE_ID, changeID);
        arguments.putString(PatchSetViewerFragment.STATUS, status);

        SelectedChange.setSelectedChange(this, changeID);

        if (mTwoPane) {
            mChangeDetail.setSelectedChange(changeID);
        } else if (expand) {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, PatchSetViewerActivity.class);
            detailIntent.putExtras(arguments);
            startActivity(detailIntent);
        }
    }

    private void onPreferenceChanged(String key) {
        switch (key) {
            case Prefs.GERRIT_KEY:
                onGerritChanged(Prefs.getCurrentGerrit(this));
                break;
            case Prefs.CURRENT_PROJECT:
                onProjectChanged(Prefs.getCurrentProject(this));
                break;
            case Prefs.TRACKING_USER:
                onUserTrackingChanged(Prefs.getTrackingUser(this));
                break;
            case Prefs.ANIMATION_KEY:
                mChangeList.getCurrentFragment().toggleAnimations(Prefs.getAnimationPreference(this));
                break;
        }
    }

    public String getSearchQuery() {
        return mSearchViewProperties.getQuery();
    }

    public ChangeListFragment getChangeList() {
        return mChangeList;
    }

    /**
     * @return The change detail fragment, may be null.
     */
    public PatchSetViewerFragment getChangeDetail() {
        return mChangeDetail;
    }

    // Call this ONLY after the searchView has been initialised
    private void setupSearchQuery() {

        String oldQuery = "";
        if (searchView != null && searchView.getQuery() != null) {
            oldQuery = searchView.getQuery().toString();
        }
        String query = "";
        if (mSearchViewProperties != null) query = mSearchViewProperties.mQuery;

        if (!mCurrentProject.isEmpty()) {
            query = SearchKeyword.replaceKeyword(query, new ProjectSearch(mCurrentProject));
        }

        Integer user = Prefs.getTrackingUser(this);
        if (user != null) {
            query = SearchKeyword.replaceKeyword(query, new OwnerSearch(user.toString()));
        }

        if (!oldQuery.equals(query)) {
            mSearchViewProperties.setQuery(query, false); // Don't submit (it will be submitted initially)
        }

        searchView.setIconified(query.isEmpty());
    }

    // SearchView properties to be set and can be used by setupSearchQuery when the search
    //  view is visible
    class SearchViewProperties {
        String mQuery = "";

        void setQuery(String query, boolean submit) {
            if (query == null) query = "";
            mQuery = query;
            if (searchView != null) {
                searchView.setQuery(mQuery, submit);
                searchView.setIconified(query.isEmpty());
            }
        }
        String getQuery() {
            if (searchView != null) {
                return searchView.getQuery().toString();
            } else {
                return mQuery;
            }
        }
    }
}
