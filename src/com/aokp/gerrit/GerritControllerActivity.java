package com.aokp.gerrit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class GerritControllerActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Setup tabs //
        TabHost host = (TabHost) findViewById(R.id.gerrit_tab_host);
        // Review tab
        Intent intentReview = new Intent().setClass(this, ReviewActivity.class);
        TabHost.TabSpec tabSpecReview = host
                .newTabSpec(getString(R.string.reviewable))
                .setContent(intentReview);
        host.addTab(tabSpecReview);

        // Merged tab
        Intent intentMerged = new Intent().setClass(this, MergedActivity.class);
        TabHost.TabSpec tabSpecMerged = host
                .newTabSpec(getString(R.string.merged))
                .setContent(intentMerged);
        host.addTab(tabSpecMerged);

        // Abandon tab
        Intent intentAbandon = new Intent().setClass(this, AbandonedActivity.class);
        TabHost.TabSpec tabSpecAbandon = host
                .newTabSpec(getString(R.string.abandoned))
                .setContent(intentAbandon);
        host.addTab(tabSpecAbandon);
    }
}