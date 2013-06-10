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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import com.jbirdvegas.mgerrit.helpers.GerritTeamsHelper;
import com.jbirdvegas.mgerrit.objects.ChangeLogRange;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
import com.jbirdvegas.mgerrit.objects.GooFileObject;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.objects.Project;
import com.jbirdvegas.mgerrit.tasks.GerritTask;
import com.jbirdvegas.mgerrit.widgets.AddTeamView;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class GerritControllerActivity extends TabActivity {
    private static final String TAG = GerritControllerActivity.class.getSimpleName();
    private TabHost mTabHost;
    private CommitterObject mCommitterObject;
    private String mProject;
    private GooFileObject mChangeLogStart;
    private GooFileObject mChangeLogStop;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mTabHost = getTabHost();
        if (!CardsActivity.mSkipStalking) {
            try {
                mCommitterObject = getIntent()
                        .getExtras()
                        .getParcelable(CardsActivity.KEY_DEVELOPER);
            } catch (NullPointerException npe) {
                // non author specific view
                // use default
            }
        }

        try {
            mProject = getIntent().getStringExtra(JSONCommit.KEY_PROJECT);
        } catch (NullPointerException npe) {
            // not following one project
        }

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
        // Setup tabs //
        addTabs();
    }

    private void addTabs() {
        Intent base = new Intent();
        base.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_NO_HISTORY
            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (mChangeLogStart != null
                && mChangeLogStop != null) {
            Log.d(TAG, "Changelog in GerritControlerActivity: " + mChangeLogStart);
            base.putExtra(AOKPChangelog.KEY_CHANGELOG,
                    new ChangeLogRange(mChangeLogStart, mChangeLogStop));
            base.setClass(this, MergedTab.class);
            this.startActivity(base);
            return;
        }
        // if we are stalking one user pass the user information
        // along to all the tabs
        if (mCommitterObject != null && !CardsActivity.mSkipStalking) {
            base.putExtra(CardsActivity.KEY_DEVELOPER, mCommitterObject);
        }
        if (mProject != null) {
            base.putExtra(JSONCommit.KEY_PROJECT, mProject);
        }
        // Review tab
        Intent intentReview = new Intent(base)
                .setClass(this, ReviewTab.class);
        TabHost.TabSpec tabSpecReview = mTabHost
                .newTabSpec(getString(R.string.reviewable))
                .setContent(intentReview)
                .setIndicator(getString(R.string.reviewable));
        mTabHost.addTab(tabSpecReview);

        // Merged tab
        Intent intentMerged = new Intent(base)
                .setClass(this, MergedTab.class);
        TabHost.TabSpec tabSpecMerged = mTabHost
                .newTabSpec(getString(R.string.merged))
                .setContent(intentMerged)
                .setIndicator(getString(R.string.merged));
        mTabHost.addTab(tabSpecMerged);

        // Abandon tab
        Intent intentAbandon = new Intent(base)
                .setClass(this, AbandonedTab.class);
        TabHost.TabSpec tabSpecAbandon = mTabHost
                .newTabSpec(getString(R.string.abandoned))
                .setContent(intentAbandon)
                .setIndicator(getString(R.string.abandoned));
        mTabHost.addTab(tabSpecAbandon);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gerrit_instances_menu, menu);
        return true;
    }

    private AlertDialog alertDialog = null;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                Intent intent = new Intent(this, Prefs.class);
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
                refreshScreen(true);
                return true;
            case R.id.menu_team_instance:
                showGerritDialog();
                return true;
            case R.id.menu_projects:
                getProjectsList();
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

    private void getProjectsList() {
        new GerritTask(this) {
            @Override
            public void onJSONResult(String jsonString) {
                try {
                    JSONObject projectsJson = new JSONObject(jsonString);
                    Iterator stringIterator = projectsJson.keys();
                    List<Project> projectLinkedList
                            = new LinkedList<Project>();
                    while (stringIterator.hasNext()) {
                        String path = (String) stringIterator.next();
                        JSONObject projJson = projectsJson.getJSONObject(path);
                        String kind = projJson.getString(JSONCommit.KEY_KIND);
                        String id = projJson.getString(JSONCommit.KEY_ID);
                        projectLinkedList.add(Project.getInstance(path, kind, id));
                    }
                    Collections.sort(projectLinkedList);
                    ListView projectsList = new ListView(getThis());
                    projectsList.setAdapter(new ArrayAdapter<Object>(getThis(),
                            android.R.layout.simple_list_item_single_choice,
                            projectLinkedList.toArray()));
                    projectsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            Project project = (Project) adapterView.getItemAtPosition(i);
                            mProject = project.getmPath();
                            if (GerritControllerActivity.this.alertDialog != null) {
                                GerritControllerActivity.this.alertDialog.dismiss();
                                GerritControllerActivity.this.alertDialog = null;
                            }
                            refreshScreen(true);
                        }
                    });
                    Builder projectsBuilder = new Builder(getThis());
                    projectsBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    projectsBuilder.setView(projectsList);
                    AlertDialog alertDialog1 = projectsBuilder.create();
                    GerritControllerActivity.this.alertDialog = alertDialog1;
                    alertDialog1.show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.execute(Prefs.getCurrentGerrit(this) + "projects/?d");
    }

    private void showGerritDialog() {
        final Builder teamBuilder = new Builder(this);
        teamBuilder.setTitle(R.string.menu_gerrit_instance);
        ListView instances = new ListView(this);
        Resources res = getResources();
        final ArrayList <String> teams = new ArrayList<String>(0);
        final ArrayList<String> urls = new ArrayList<String>(0);
        String[] gerritNames = res.getStringArray(R.array.gerrit_names);
        Collections.addAll(teams, gerritNames);
        String[] gerritWeb = res.getStringArray(R.array.gerrit_webaddresses);
        Collections.addAll(urls, gerritWeb);
        GerritTeamsHelper teamsHelper = new GerritTeamsHelper();
        teams.addAll(teamsHelper.getGerritNamesList());
        urls.addAll(teamsHelper.getGerritUrlsList());
        final ArrayAdapter <String> instanceAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                teams);
        instances.setAdapter(instanceAdapter);
        instances.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    Prefs.setCurrentGerrit(view.getContext(), urls.get(i));
                    // refresh the screen and go straight to default "Review tab"
                    refreshScreen(true);
                    if (GerritControllerActivity.this.alertDialog != null) {
                        GerritControllerActivity.this.alertDialog.dismiss();
                        GerritControllerActivity.this.alertDialog = null;
                    }
                } catch (ArrayIndexOutOfBoundsException ignored) {

                }
            }
        });
        instances.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                // on long click delete the file and refresh the list
                File target = new File(GerritTeamsHelper.mExternalCacheDir + "/" + teams.get(i));
                boolean success = target.delete();
                Log.v(TAG, "Attempt to delete: " + target.getAbsolutePath()
                        + " was " + success);
                if (!success) {
                    Log.v(TAG, "Files present:" + Arrays.toString(GerritTeamsHelper.mExternalCacheDir.list()));
                }
                teams.remove(i);
                urls.remove(i);
                instanceAdapter.notifyDataSetChanged();
                return success;
            }
        });
        teamBuilder.setView(instances);
        teamBuilder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        teamBuilder.setPositiveButton(R.string.add_gerrit_team,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                AlertDialog addTeamDialog = new Builder(teamBuilder.getContext())
                        .setTitle(R.string.add_gerrit_team)
                        .setIcon(android.R.drawable.ic_input_add)
                        .create();
                AddTeamView.RefreshCallback callback =
                        new AddTeamView.RefreshCallback() {
                    @Override
                    public void refreshScreenCallback() {
                        refreshScreen(true);
                    }
                };
                AddTeamView addTeamView = new AddTeamView(
                        teamBuilder.getContext(),
                        addTeamDialog);
                addTeamView.addRefreshScreenCallback(callback);
                addTeamDialog.setView(addTeamView.getView());
                addTeamDialog.show();
            }
        });
        this.alertDialog = teamBuilder.create();
        this.alertDialog.show();
    }

    private GerritControllerActivity getThis() {
        return this;
    }

    public void refreshScreen(boolean keepTabPosition) {
        int currentTab = getTabHost().getCurrentTab();
        mTabHost.clearAllTabs();
        addTabs();
        if (keepTabPosition) {
            mTabHost.setCurrentTab(currentTab);
        }
    }
}