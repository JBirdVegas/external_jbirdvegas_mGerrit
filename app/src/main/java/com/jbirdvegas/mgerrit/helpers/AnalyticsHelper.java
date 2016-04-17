package com.jbirdvegas.mgerrit.helpers;

import android.app.Activity;
import android.content.Context;

public class AnalyticsHelper {

    private static AnalyticsHelper sInstance;
    private static AnalyticsSender sAnalyticsSender;

    private AnalyticsHelper() {
        // keep out
    }

    public static AnalyticsHelper getInstance() {
        if (sAnalyticsSender == null) {
            // sender implementation depends on productFlavor.  `googlePlay` implements the analytics where
            // `noAnalytics` stubs all methods to avoid collecting analytics for those who prefer to not be tracked
            sAnalyticsSender = new AnalyticsSenderImpl();
        }
        if (sInstance == null) {
            sInstance = new AnalyticsHelper();
        }
        return sInstance;
    }

    // Return this for method chaining
    public AnalyticsHelper setCustomString(String key, String data) {
        sAnalyticsSender.setCustomString(key, data);
        return this;
    }

    public AnalyticsHelper setCustomInt(String key, int data) {
        sAnalyticsSender.setCustomInt(key, data);
        return this;
    }

    public AnalyticsHelper logException(Exception e) {
        sAnalyticsSender.logException(e);
        return this;
    }

    public AnalyticsHelper sendAnalyticsEvent(Context context, String category, String action, String label, Long value) {
        sAnalyticsSender.sendAnalyticsEvent(context, category, action, label, value);
        return this;
    }

    public AnalyticsHelper startActivity(Activity activity) {
        sAnalyticsSender.startActivity(activity);
        return this;
    }

    public AnalyticsHelper stopActivity(Activity activity) {
        sAnalyticsSender.stopActivity(activity);
        return this;
    }

    public AnalyticsHelper initAnalytics(Context context) {
        sAnalyticsSender.initAnalytics(context);
        return this;
    }
}