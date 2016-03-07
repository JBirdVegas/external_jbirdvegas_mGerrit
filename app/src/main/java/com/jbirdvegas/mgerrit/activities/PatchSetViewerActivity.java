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
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.ShareActionProvider;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.adapters.PatchSetAdapter;
import com.jbirdvegas.mgerrit.database.SelectedChange;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.fragments.PatchSetViewerFragment;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.NewChangeSelected;
import com.jbirdvegas.mgerrit.message.SearchQueryChanged;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import hugo.weaving.DebugLog;

/**
 * An activity representing a single Change detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * change details are presented side-by-side with a list of changes
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link com.jbirdvegas.mgerrit.fragments.PatchSetViewerFragment}.
 */
public class PatchSetViewerActivity extends BaseDrawerActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private ShareActionProvider mShareActionProvider;
    private PatchSetAdapter mAdapter;
    private ViewPager mViewPager;

    private EventBus mEventBus;

    // Relevant details for the selected change
    private String mStatus;
    private String mChangeId;
    private Integer mChangeNumber;
    private Integer mCurrentTab;

    private Cursor mCursor;
    private static Integer sChangeIdIndex;
    private Integer sChangeNumberIndex;

    public static int LOADER_CHANGES = 20;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setTheme(PrefsFragment.getCurrentThemeID(this));
        super.onCreate(savedInstanceState);

        setContentView(R.layout.patchset_pager);

        initNavigationDrawer(false);

        mEventBus = EventBus.getDefault();

        Bundle args = getIntent().getExtras();
        mStatus = args.getString(PatchSetViewerFragment.STATUS);
        setSelectedStatus(mStatus);

        // Don't pass args here as it does not have search query information
        getSupportLoaderManager().initLoader(LOADER_CHANGES, null, this);

        mAdapter = new PatchSetAdapter(this, getSupportFragmentManager(), args);

        // Set up the ViewPager with the sections adapter.
        /** The {@link android.support.v4.view.ViewPager} that will host the section contents. */
        mViewPager = (ViewPager) findViewById(R.id.tabs);
        mViewPager.setAdapter(mAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                onNewChangeSelected(position);
            }
        });
    }

    private void onNewChangeSelected(int position) {
        Pair<String, Integer> change = getChangeAtPosition(position);
        SelectedChange.setSelectedChange(this, change.first, change.second, mStatus);
        mChangeNumber = change.second;
        mChangeId = change.first;
        setTitleWithCommit(change.second);
        mEventBus.postSticky(new NewChangeSelected(change.first, change.second, mStatus, false));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.change_details_menu, menu);
        MenuItem item = menu.findItem(R.id.menu_details_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        if (mStatus != null) setShareIntent(mChangeId, mChangeNumber);
        return true;
    }

    /**
     * Constructs a share intent with the change information and sets it to the share
     *  action provider.
     * @param changeid The ID of the selected change
     * @param changeNumber The legacy number of the selected change
     */
    private void setShareIntent(String changeid, Integer changeNumber) {
        if (mShareActionProvider != null) {
            Intent intent = Tools.createShareIntent(this, changeid, changeNumber);
            mShareActionProvider.setShareIntent(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpTo(this, new Intent(this, GerritControllerActivity.class));
                return true;
            case R.id.menu_details_browser:
                if (mChangeNumber == null) return false;
                Tools.launchDiffInBrowser(this, mChangeNumber, null, null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets the details for the selected change and re-creates the options menu.
     * By using the status, we can query the database and get both the change id
     * and the change number.
     * @param status The status of the selected change
     */
    public void setSelectedStatus(String status) {
        mStatus = status;
        Pair<String, Integer> change = SelectedChange.getSelectedChange(this, mStatus);
        mChangeId = change.first;
        mChangeNumber = change.second;

        // We need to re-create the options menu to set the share intent again
        invalidateOptionsMenu();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        if (id != LOADER_CHANGES) return super.onCreateLoader(id, args);
        if (args == null) {
            SearchQueryChanged ev = mEventBus.getStickyEvent(SearchQueryChanged.class);
            if (ev != null) {
                String to = ev.getClazzName();
                if (GerritControllerActivity.class.getSimpleName().equals(to))
                    args = ev.getBundle();
            }
        }

        if (args != null) {
            String databaseQuery = args.getString(SearchQueryChanged.KEY_WHERE);
            if (databaseQuery != null && !databaseQuery.isEmpty()) {
                if (args.getStringArrayList(SearchQueryChanged.KEY_BINDARGS) != null) {
                    /* Create a copy as the findCommits function can modify the contents of bindArgs
                     *  and we want each receiver to use the bindArgs from the original broadcast */
                    ArrayList<String> bindArgs = new ArrayList<>();
                    bindArgs.addAll(args.getStringArrayList(SearchQueryChanged.KEY_BINDARGS));
                    return UserChanges.findCommits(this, mStatus, databaseQuery, bindArgs);
                }
            }
        }
        return UserChanges.findCommits(this, mStatus, null, null);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LOADER_CHANGES) {
            sChangeIdIndex = null;
            sChangeNumberIndex = null;
            mAdapter.notifyDataSetChanged();
        } else {
            super.onLoaderReset(loader);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursorLoader.getId() != LOADER_CHANGES) {
            super.onLoadFinished(cursorLoader, cursor);
            return;
        }

        /* Weird behaviour: This method gets called multiple times. Once after
         *  initialising this activity and starting the loader, then again later without
         *  manually resetting the loaders. If the current tab is set we have been here,
         *  so we don't need to find the page again.
         *
         *  Note: We could respond to the database being updated here (this is the only
         *   logical reason explaining the above), but this is called too frequently,
         *  so we will only update this when the user opens the activity
         */

        int pos = 0;
        mCursor = cursor;
        sChangeIdIndex = cursor.getColumnIndex(UserChanges.C_CHANGE_ID);
        sChangeNumberIndex = cursor.getColumnIndex(UserChanges.C_COMMIT_NUMBER);

        while (cursor.moveToNext()) {
            if (cursor.getString(sChangeIdIndex).equals(mChangeId)) {
                pos = cursor.getPosition();
                break;
            }
        }

        mAdapter.notifyDataSetChanged();

        if (mChangeNumber != null) setTitleWithCommit(mChangeNumber);

        if (pos == mViewPager.getCurrentItem()) mCurrentTab = pos;
        else if (mCurrentTab == null) {
            mCurrentTab = pos;
            if (mCurrentTab >= 0) mViewPager.setCurrentItem(mCurrentTab);
            onNewChangeSelected(mCurrentTab);
        }
    }

    @DebugLog
    public Pair<String, Integer> getChangeAtPosition(int position) {
        mCursor.moveToPosition(position);
        return new Pair<>(mCursor.getString(sChangeIdIndex), mCursor.getInt(sChangeNumberIndex));
    }

    public int getNumberOfChanges() {
        if (mCursor == null || mCursor.isClosed()) return 0;
        return mCursor.getCount();
    }

    private void setTitleWithCommit(int commitNumber) {
        String s = getResources().getString(R.string.change_detail_heading);
        setTitle(String.format(s, commitNumber));
    }

    @Keep @Subscribe(threadMode = ThreadMode.MAIN)
    // Listen for processed search query changes
    public void onSearchQueryChanged(SearchQueryChanged ev) {
        getSupportLoaderManager().restartLoader(0, null, this);
    }
}
