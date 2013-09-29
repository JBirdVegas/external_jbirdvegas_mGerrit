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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

/**
 * An activity representing a single Change detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * change details are presented side-by-side with a list of changes
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link com.jbirdvegas.mgerrit.PatchSetViewerFragment}.
 */
public class PatchSetViewerActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        /* If there was a fragment state save from previous configurations of
         * this activity, then it doesn't need to be added again.*/
        if (savedInstanceState == null) {
            /* Create the detail fragment and add it to the activity
             * using a fragment transaction.
             * Copy all the intent arguments over to the fragment */
            PatchSetViewerFragment fragment = new PatchSetViewerFragment();
            Bundle args = new Bundle();
            args.putAll(getIntent().getExtras());
            fragment.setArguments(args);

            // Display the fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commit();
        }
        else {
            setContentView(R.layout.commit_list);
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
        }
        return super.onOptionsItemSelected(item);
    }
}
