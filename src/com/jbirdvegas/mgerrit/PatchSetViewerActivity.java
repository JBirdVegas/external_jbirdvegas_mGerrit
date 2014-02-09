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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.jbirdvegas.mgerrit.database.SelectedChange;
import com.jbirdvegas.mgerrit.helpers.Tools;

/**
 * An activity representing a single Change detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * change details are presented side-by-side with a list of changes
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link com.jbirdvegas.mgerrit.PatchSetViewerFragment}.
 */
public class PatchSetViewerActivity extends FragmentActivity {

    private ShareActionProvider mShareActionProvider;

    // Relevant details for the selected change
    private String mStatus;
    private String mChangeId;
    private Integer mChangeNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setTheme(Prefs.getCurrentThemeID(this));
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
        } else {
            setContentView(R.layout.commit_list);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.change_details_menu, menu);
        MenuItem item = menu.findItem(R.id.menu_details_share);
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();

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
            String webAddress = Tools.getWebAddress(this, changeNumber);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra(Intent.EXTRA_SUBJECT,
                    String.format(getResources().getString(R.string.commit_shared_from_mgerrit),
                            changeid));
            intent.putExtra(Intent.EXTRA_TEXT, webAddress + " #mGerrit");
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
                String webAddress = Tools.getWebAddress(this, mChangeNumber);
                if (webAddress != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webAddress));
                    startActivity(browserIntent);
                } else {
                    Toast.makeText(this, R.string.failed_to_find_url, Toast.LENGTH_SHORT).show();
                }
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
}
