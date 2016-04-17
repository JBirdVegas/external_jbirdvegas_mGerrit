package com.jbirdvegas.mgerrit.helpers;

import android.app.Activity;
import android.content.Context;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.jbirdvegas.mgerrit.R;

public class AnalyticsSenderImpl implements AnalyticsSender {
    @Override
    public void sendAnalyticsEvent(Context context,
                                   String category, String action, String label, Long value) {
        EasyTracker.getInstance(context).send(MapBuilder
                .createEvent(category, action, label, value)
                .build());
        // note this screen as viewed
        EasyTracker.getInstance(context).send(MapBuilder.createAppView().build());
    }

    @Override
    public void startActivity(Activity activity) {
        EasyTracker.getInstance(activity.getApplicationContext()).activityStart(activity);
    }

    @Override
    public void stopActivity(Activity activity) {
        EasyTracker.getInstance(activity.getApplicationContext()).activityStop(activity);
    }

    @Override
    public void initAnalytics(Context context) {
        GoogleAnalytics googleAnalytics = GoogleAnalytics.getInstance(context);

        String trackingId = context.getString(R.string.ga_trackingId);
        Tracker tracker = googleAnalytics.getTracker(trackingId);
        googleAnalytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
        tracker.send(MapBuilder.createAppView().build());
    }
}