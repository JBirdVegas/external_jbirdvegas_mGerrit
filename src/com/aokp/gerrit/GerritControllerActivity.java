package com.aokp.gerrit;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class GerritControllerActivity extends TabActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Setup tabs //
        TabHost host = getTabHost();
        // Review tab
        Intent intentReview = new Intent().setClass(this, ReviewTab.class);
        TabHost.TabSpec tabSpecReview = host
                .newTabSpec(getString(R.string.reviewable))
                .setContent(intentReview)
                .setIndicator(getString(R.string.reviewable));
        host.addTab(tabSpecReview);

        // Merged tab
        Intent intentMerged = new Intent().setClass(this, MergedTab.class);
        TabHost.TabSpec tabSpecMerged = host
                .newTabSpec(getString(R.string.merged))
                .setContent(intentMerged)
                .setIndicator(getString(R.string.merged));
        host.addTab(tabSpecMerged);

        // Abandon tab
        Intent intentAbandon = new Intent().setClass(this, AbandonedTab.class);
        TabHost.TabSpec tabSpecAbandon = host
                .newTabSpec(getString(R.string.abandoned))
                .setContent(intentAbandon)
                .setIndicator(getString(R.string.abandoned));
        host.addTab(tabSpecAbandon);
    }
}