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
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
import com.jbirdvegas.mgerrit.objects.JSONCommit;


public class GerritControllerActivity extends TabActivity {
    private static final String TAG = GerritControllerActivity.class.getSimpleName();
    private TabHost mTabHost;
    private CommitterObject mCommitterObject;
    private String mProject;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        try {
            mCommitterObject = getIntent()
                    .getExtras()
                    .getParcelable(CardsActivity.KEY_DEVELOPER);
        } catch (NullPointerException npe) {
            // non author specific view
            // use default
        }

        try {
            mProject = getIntent().getStringExtra(JSONCommit.KEY_PROJECT);
        } catch (NullPointerException npe) {
            // not following one project
        }
        // Setup tabs //
        mTabHost = getTabHost();
        addTabs();
    }

    private void addTabs() {
        Intent base = new Intent();
        base.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // if we are stalking one user pass the user information
        // along to all the tabs
        if (mCommitterObject != null) {
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
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
                int currentTab = getTabHost().getCurrentTab();
                mTabHost.clearAllTabs();
                addTabs();
                mTabHost.setCurrentTab(currentTab);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}