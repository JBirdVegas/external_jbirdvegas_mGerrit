package com.aokp.gerrit;

import android.app.ActionBar;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;

public class GerritControllerActivity extends TabActivity {
    private static final String TAG = GerritControllerActivity.class.getSimpleName();
    private ActionBar mActionBar;
    private TabHost mTabHost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mActionBar = getActionBar();
        // Setup tabs //
        mTabHost = getTabHost();
        addTabs();
    }

    private void addTabs() {
        // Review tab
        Intent intentReview = new Intent().setClass(this, ReviewTab.class);
        TabHost.TabSpec tabSpecReview = mTabHost
                .newTabSpec(getString(R.string.reviewable))
                .setContent(intentReview)
                .setIndicator(getString(R.string.reviewable));
        mTabHost.addTab(tabSpecReview);

        // Merged tab
        Intent intentMerged = new Intent().setClass(this, MergedTab.class);
        TabHost.TabSpec tabSpecMerged = mTabHost
                .newTabSpec(getString(R.string.merged))
                .setContent(intentMerged)
                .setIndicator(getString(R.string.merged));
        mTabHost.addTab(tabSpecMerged);

        // Abandon tab
        Intent intentAbandon = new Intent().setClass(this, AbandonedTab.class);
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
        Intent intent = new Intent(this, Prefs.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        return true;
    }

    private int findPosition(String[] array, CharSequence query) {
        for (int i = 0; array.length > i; i++) {
            if (query.equals(array[i])) {
                return i;
            }
        }
        return -1;
    }
}