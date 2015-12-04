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

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.activities.SigninActivity;
import com.jbirdvegas.mgerrit.database.Users;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.helpers.GerritTeamsHelper;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.message.GerritChanged;
import com.jbirdvegas.mgerrit.message.SearchQueryChanged;
import com.jbirdvegas.mgerrit.objects.GerritDetails;
import com.jbirdvegas.mgerrit.search.IsSearch;
import com.jbirdvegas.mgerrit.search.SearchKeyword;
import com.jbirdvegas.mgerrit.views.GerritSearchView;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
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

import nl.littlerobots.squadleader.Keep;

/*
 * Extends the base activity with the main navigation drawer.
 * Activities requiring the navigation drawer can extend this class and call initNavigationDrawer.
 * A toolbar must also be present in the activity's layout
 */
@Keep
public abstract class BaseDrawerActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static int DRAWER_PROFILE_LOADER = 10;

    private Drawer mDrawer;
    private GerritSearchView mSearchView;
    private AccountHeader mProfileHeader;
    private RequestQueue mRequestQuery;
    private ArrayList<GerritDetails> mGerrits = new ArrayList<>();


    private AbstractDrawerImageLoader mDrawerImageLoader = new AbstractDrawerImageLoader() {
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
    };

    /**
     * Initialises the left navigation mDrawer and sets an adapter for the content
     * @param isParent
     */
    protected void initNavigationDrawer(boolean isParent) {
        Toolbar toolbar = setupActionBar();

        mRequestQuery = Volley.newRequestQueue(this);

        DrawerImageLoader.init(mDrawerImageLoader);

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

        DrawerBuilder drawerBuilder = new DrawerBuilder().withActivity(this)
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
                        setGerritSelected(PrefsFragment.getCurrentGerrit(BaseDrawerActivity.this));
                        mDrawer.closeDrawer();
                        return result;
                    }
                });

        /* This condition should always be true, but just to make sure
         * (and keep Android Studio happy) */
        if (getSupportActionBar() != null) {
            // If we are the parent we want to show the hamburger icon, else show the back icon
            if (isParent) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                drawerBuilder.withToolbar(toolbar);
            } else {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        mDrawer = drawerBuilder.build();

        getSupportLoaderManager().initLoader(DRAWER_PROFILE_LOADER, null, this);

        addGerritsToDrawer();
    }

    /**
     * Sets the toolbar as the action bar. We cannot have a navigation drawer without
     * an action bar
     * @return The toolbar to be used as the action bar
     */
    protected Toolbar setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        return toolbar;
    }

    protected Drawer getDrawer() {
        return mDrawer;
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
        mProfileHeader.clear();
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

        int min = Math.min(teams.size(), urls.size());
        for (int i = 0; i < min; i++) {
            gerrits.add(new GerritDetails(teams.get(i), urls.get(i)));
        }
        ArrayList<GerritDetails> gerritData = new ArrayList<>(gerrits);
        Collections.sort(gerritData);

        // Add a divider if we have not added any Gerrit instances to the drawer yet
        if (gerritData.size() > 0 && mGerrits.size() == 0) {
            mDrawer.addItem(new DividerDrawerItem());
        }

        for (GerritDetails gerrit : gerritData) {
            if (!mGerrits.contains(gerrit)) {
                PrimaryDrawerItem item = new PrimaryDrawerItem().withName(gerrit.getGerritName()).withTag(gerrit);
                mDrawer.addItem(item);
                mGerrits.add(gerrit);
            }

        }
    }

    protected void navigationSetSelectedById(int id) {
        mDrawer.setSelection(id);
    }

    /**
     * Mark a Gerrit in the list as selected. Note: This does not modify the drawer's
     *  selection, it just makes the row stand out.
     * @param gerritUrl The URL of the Gerrit to mark as selected.
     */
    private void setGerritSelected(String gerritUrl) {

        // Get the themed next icon
        final TypedValue typedvalueattr = new TypedValue();
        getTheme().resolveAttribute(R.attr.nextIcon, typedvalueattr, true);

        for (IDrawerItem item : mDrawer.getDrawerItems()) {
            if (item.getTag() instanceof GerritDetails) {
                GerritDetails gerrit = (GerritDetails) item.getTag();
                if (gerrit.getGerritUrl().equals(gerritUrl)) {
                    ((PrimaryDrawerItem) item).withIcon(typedvalueattr.resourceId).withSelectable(false);
                } else {
                    ((PrimaryDrawerItem) item).withIcon((Drawable) null).withSelectable(true);
                    // Need to notify the adapter that we want to clear the icon for this item
                    mDrawer.getAdapter().notifyItemChanged(mDrawer.getPosition(item));
                }
            }
        }
    }

    private boolean onMenuItemSelected(int itemId) {
        Intent intent;
        switch (itemId) {
            case R.id.menu_save:
                intent = new Intent(this, PrefsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                return true;
            case R.id.menu_team_instance:
                intent = new Intent(this, GerritSwitcher.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                return true;
            case R.id.menu_projects:
                intent = new Intent(this, ProjectsList.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                return true;
            case R.id.menu_changes:
                // Clear the search query
                if (!(this instanceof GerritControllerActivity)) {
                    intent = new Intent(this, GerritControllerActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
                return true;
            case R.id.menu_starred:
                if (mSearchView != null) {
                    mSearchView.replaceKeyword(new IsSearch("starred"), true);
                }
                return true;
            default:
                return false;
        }
    }

    public void onGerritChanged(GerritChanged ev) {
        /* Don't need to restart the loader here as it will be triggered after selecting a new Gerrit
         * as the switcher is a new activity */
        addGerritsToDrawer();
        setGerritSelected(ev.getNewGerritUrl());
    }

    public void onSearchQueryChanged(SearchQueryChanged ev) {
        if (ev == null) {
            if (mSearchView.hasKeyword(new IsSearch(IsSearch.OP_VALUE_STARRED))) {
                navigationSetSelectedById(R.id.menu_starred);
            } else {
                navigationSetSelectedById(R.id.menu_changes);
            }
        } else {
            Set<SearchKeyword> tokens = ev.getTokens();
            if (tokens != null && !tokens.isEmpty() &&
                    SearchKeyword.findKeyword(tokens, new IsSearch(IsSearch.OP_VALUE_STARRED)) != -1) {
                navigationSetSelectedById(R.id.menu_starred);
            } else {
                navigationSetSelectedById(R.id.menu_changes);
            }
        }
    }
}
