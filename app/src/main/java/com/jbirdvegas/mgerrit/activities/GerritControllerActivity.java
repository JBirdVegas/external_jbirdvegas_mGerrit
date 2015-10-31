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

import android.app.AlertDialog.Builder;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.TheApplication;
import com.jbirdvegas.mgerrit.database.SelectedChange;
import com.jbirdvegas.mgerrit.fragments.ChangeListFragment;
import com.jbirdvegas.mgerrit.fragments.PatchSetViewerFragment;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.helpers.AnalyticsHelper;
import com.jbirdvegas.mgerrit.helpers.ROMHelper;
import com.jbirdvegas.mgerrit.message.GerritChanged;
import com.jbirdvegas.mgerrit.message.NewChangeSelected;
import com.jbirdvegas.mgerrit.message.NotSupported;
import com.jbirdvegas.mgerrit.views.GerritSearchView;

import de.greenrobot.event.EventBus;

public class GerritControllerActivity extends FragmentActivity {

    private static final String GERRIT_INSTANCE = "gerrit";
    private String mGerritWebsite;

    private Menu mMenu;

    // Indicates if we are running this in tablet mode.
    private boolean mTwoPane = false;
    private ChangeListFragment mChangeList;

    // This will be null if mTwoPane is false (i.e. not tablet mode)
    private PatchSetViewerFragment mChangeDetail;

    private int mTheme;
    private GerritSearchView mSearchView;

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mTheme = PrefsFragment.getCurrentThemeID(this);
        setTheme(mTheme);

        super.onCreate(savedInstanceState);

        // check if caller has a gerrit instance start screen preference
        // TODO: Check if the GERRIT_INSTANCE URL is in the list of Gerrits we have names for
        String suppliedGerritInstance = getIntent().getStringExtra(GERRIT_INSTANCE);
        if (suppliedGerritInstance != null
                && !suppliedGerritInstance.isEmpty()
                && suppliedGerritInstance.contains("http")) {
            // just set the prefs and allow normal loading
            PrefsFragment.setCurrentGerrit(this, suppliedGerritInstance, null);
        }

        GoogleAnalytics googleAnalytics = GoogleAnalytics.getInstance(this);

        String trackingId = getString(R.string.ga_trackingId);
        Tracker tracker = googleAnalytics.getTracker(trackingId);
        googleAnalytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
        tracker.send(MapBuilder
                .createAppView().build());

        // keep a log of what ROM our users run
        AnalyticsHelper.sendAnalyticsEvent(this, AnalyticsHelper.GA_APP_OPEN,
                AnalyticsHelper.GA_ROM_VERSION, ROMHelper.determineRom(this), null);

        // Keep track of what theme is being used
        AnalyticsHelper.sendAnalyticsEvent(this, AnalyticsHelper.GA_APP_OPEN,
                AnalyticsHelper.GA_THEME_SET_ON_OPEN, PrefsFragment.getCurrentTheme(this), null);

        setContentView(R.layout.main);

        FragmentManager fm = getSupportFragmentManager();
        if (findViewById(R.id.change_detail_fragment) != null) {
            // The detail container view will be present only in
            // large-screen layouts(res/values-sw600dp). If this view is present,
            // then the activity should be in two-pane mode.
            mTwoPane = true;
            mChangeDetail = (PatchSetViewerFragment) fm.findFragmentById(R.id.change_detail_fragment);
        }
        PrefsFragment.setTabletMode(this, mTwoPane);

        mChangeList = (ChangeListFragment) fm.findFragmentById(R.id.change_list_fragment);

        mGerritWebsite = PrefsFragment.getCurrentGerrit(this);
        String gerritName = PrefsFragment.getCurrentGerritName(this);
        if (gerritName != null) setTitle(gerritName);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (GerritSearchView) findViewById(R.id.search);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        hideChangelogOption(PrefsFragment.getCurrentGerrit(this));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gerrit_instances_menu, menu);
        this.mMenu = menu;
        return true;
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
                showHelpDialog();
                return true;
            case R.id.menu_refresh:
                refreshTabs();
                return true;
            case R.id.menu_team_instance:
                intent = new Intent(this, GerritSwitcher.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                return true;
            case R.id.menu_projects:
                intent = new Intent(this, ProjectsList.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                return true;
            case R.id.menu_changelog:
                // TODO: Send the current search query along too.
                Intent changelog = new Intent(this, AOKPChangelog.class);
                changelog.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(changelog);
                return true;
            case R.id.menu_search:
                // Toggle the visibility of the searchview
                mSearchView.toggleVisibility();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onGerritChanged(String newGerrit) {
        mGerritWebsite = newGerrit;
        Toast.makeText(this,
                getString(R.string.using_gerrit_toast) + ' ' + newGerrit,
                Toast.LENGTH_LONG).show();
        String gerritName = PrefsFragment.getCurrentGerritName(this);
        if (gerritName != null) setTitle(gerritName);
        hideChangelogOption(newGerrit);
        refreshTabs();
    }

    /* Mark all of the tabs as dirty to trigger a refresh when they are next
     *  resumed. refresh must be called on the current fragment as it is already
     *  resumed.
     */
    public void refreshTabs() {
        mChangeList.refreshTabs();

        // Hack this in to fix problem when Gerrits are updated and we don't update the version again
        ((TheApplication) getApplication()).requestServerVersion(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Manually check if the Gerrit source changed (from the Preferences)
        String s = PrefsFragment.getCurrentGerrit(this);
        if (!s.equals(mGerritWebsite)) onGerritChanged(s);

        // Apply the theme if it has changed
        int themeId = PrefsFragment.getCurrentThemeID(this);
        if (themeId != mTheme) {
            mTheme = themeId;
            setTheme(themeId);
            this.recreate();
        }
    }

    // Hide the AOKP Changelog menu option when AOKP's Gerrit is not selected
    private void hideChangelogOption(String gerrit) {
        MenuItem changelog = mMenu.findItem(R.id.menu_changelog);
        if (changelog != null) {
            changelog.setVisible(gerrit.contains("aokp"));
        }
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

    private void showHelpDialog() {
        Builder builder = new Builder(this);
        builder.setTitle(R.string.menu_help);
        LayoutInflater layoutInflater = this.getLayoutInflater();
        View dialog = layoutInflater.inflate(R.layout.dialog_help, null);
        builder.setView(dialog);
        builder.setNeutralButton(getString(R.string.gerrit_help),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(mGerritWebsite + "Documentation/index.html"));
                startActivity(browserIntent);
            }
        });
        builder.create();
        builder.show();
    }

    public void onEventMainThread(GerritChanged ev) {
        onGerritChanged(ev.getNewGerritUrl());
    }

    /**
     * Handler for when a change is selected in the list.
     */
    public void onEventMainThread(NewChangeSelected ev) {
        String changeId = ev.getChangeId();
        SelectedChange.setSelectedChange(this, changeId);

        if (mTwoPane) ev.setFragment(mChangeDetail);

        ev.inflate(this);
    }

    public void onEventMainThread(NotSupported ev) {
        Toast.makeText(this, ev.getMessage(), Toast.LENGTH_LONG).show();
    }
}
