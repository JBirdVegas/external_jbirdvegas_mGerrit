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

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.fragments.CommentFragment;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.tasks.GerritService;

/**
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link CommentFragment}.
 */
public class CommentActivity extends BaseDrawerActivity
        implements LoaderCallbacks<Cursor> {

    private CommentFragment mCommentFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(PrefsFragment.getCurrentThemeID(this));
        setContentView(R.layout.activity_single_pane);

        setupActionBar();

        mCommentFragment = new CommentFragment();
        //Pass the arguments straight on to the fragment
        mCommentFragment.setArguments(getIntent().getExtras());
        // Display the fragment as the main content.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, mCommentFragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.comment_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_send:
                mCommentFragment.addComment();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
