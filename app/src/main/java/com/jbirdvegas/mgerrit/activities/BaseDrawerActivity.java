/*
 *
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (p4r4n01d), 2015
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
 *
 */

package com.jbirdvegas.mgerrit.activities;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.SigninActivity;
import com.jbirdvegas.mgerrit.database.Users;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.helpers.GerritTeamsHelper;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.objects.GerritDetails;
import com.jbirdvegas.mgerrit.search.IsSearch;
import com.jbirdvegas.mgerrit.views.GerritSearchView;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/*
 * Extends the base activity with the main navigation drawer
 */
public class BaseDrawerActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static int DRAWER_PROFILE_LOADER = 10;

    private Drawer mDrawer;
    private PrimaryDrawerItem mGerritDrawerItem;
    private GerritSearchView mSearchView;
    private AccountHeader mProfileHeader;
    private RequestQueue mRequestQuery;

    /**
     * Initialises the left navigation mDrawer and sets an adapter for the content
     */
    protected void initNavigationDrawer() {
        Toolbar toolbar = setupActionBar();

        mRequestQuery = Volley.newRequestQueue(this);

        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                ImageRequest imageRequest = GravatarHelper.imageVolleyRequest(imageView, uri.toString(), mRequestQuery);
                imageView.setTag(R.id.imageRequest, imageRequest);
            }

            @Override
            public void cancel(ImageView imageView) {
                ImageRequest imageRequest = (ImageRequest) imageView.getTag(R.id.imageRequest);
                if (imageRequest != null) imageRequest.cancel();
            }
        });

        mProfileHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withSelectionListEnabledForSingleProfile(false)
                .withHeaderBackground(R.drawable.header)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        Intent intent = new Intent(BaseDrawerActivity.this, SigninActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(intent);
                        return true;
                    }
                })
                .build();

        mDrawer = new DrawerBuilder().withActivity(this).withToolbar(toolbar)
                .inflateMenu(R.menu.main_drawer)
                .withAccountHeader(mProfileHeader)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        boolean result = onMenuItemSelected(drawerItem.getIdentifier());

                        if (drawerItem.getTag() instanceof GerritDetails) {
                            GerritDetails gerrit = (GerritDetails) drawerItem.getTag();
                            PrefsFragment.setCurrentGerrit(BaseDrawerActivity.this,
                                    gerrit.getGerritUrl(), gerrit.getGerritName());
                        }

                        mDrawer.closeDrawer();
                        return result;
                    }
                }).build();

        mGerritDrawerItem = (PrimaryDrawerItem) mDrawer.getDrawerItem(R.id.menu_heading)
                .withSelectable(false);

        getSupportLoaderManager().initLoader(DRAWER_PROFILE_LOADER, null, this);

        addGerritsToDrawer();
    }

    protected Toolbar setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        return toolbar;
    }

    protected Drawer getDrawer() {
        return mDrawer;
    }

    protected PrimaryDrawerItem getGerritDrawerItem() {
        return mGerritDrawerItem;
    }

    protected void setSearchView(GerritSearchView searchView) {
        mSearchView = searchView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (i == DRAWER_PROFILE_LOADER) {
            return Users.getSelf(this);
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> accountInfoLoader, Cursor info) {
        if (info.moveToFirst()) {
            String name = info.getString(info.getColumnIndex(Users.C_NAME));
            String email = info.getString(info.getColumnIndex(Users.C_EMAIL));

            mProfileHeader.addProfile(
                    new ProfileDrawerItem().withName(name).withEmail(email)
                            .withTextColor(getResources().getColor(R.color.text_light))
                            .withIcon(Uri.parse(GravatarHelper.getGravatarUrl(email))), 0
            );
            mProfileHeader.setDrawer(mDrawer);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mProfileHeader.clear();
    }

    private void addGerritsToDrawer() {
        GerritTeamsHelper teamsHelper = new GerritTeamsHelper();
        ArrayList<String> teams = new ArrayList<>(teamsHelper.getGerritNamesList());
        ArrayList<String> urls = new ArrayList<>(teamsHelper.getGerritUrlsList());

        Set<GerritDetails> gerrits = new HashSet<>();

        String currentGerrit = PrefsFragment.getCurrentGerrit(this);

        int min = Math.min(teams.size(), urls.size());
        for (int i = 0; i < min; i++) {
            if (!currentGerrit.equals(urls.get(i))) {
                gerrits.add(new GerritDetails(teams.get(i), urls.get(i)));
            }
        }
        ArrayList<GerritDetails> gerritData = new ArrayList<>(gerrits);
        Collections.sort(gerritData);

        for (GerritDetails gerrit : gerritData) {
            PrimaryDrawerItem item = new PrimaryDrawerItem().withName(gerrit.getGerritName())
                    .withTag(gerrit);
            mDrawer.addItem(item);
        }
    }

    private boolean onMenuItemSelected(int itemId) {
        Intent intent;
        switch (itemId) {
            case R.id.menu_save:
                intent = new Intent(this, PrefsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
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
            case R.id.menu_changes:
                // Clear the search query
                mSearchView.setVisibility(View.GONE);
                return true;
            case R.id.menu_starred:
                mSearchView.replaceKeyword(new IsSearch("starred"), true);
                return true;
            default:
                return false;
        }
    }
}
