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

    public void sendAnalyticsEvent(Context context, String category, String action, String label, Long value) {
        sAnalyticsSender.sendAnalyticsEvent(context, category, action, label, value);
    }

    public void startActivity(Activity activity) {
        sAnalyticsSender.startActivity(activity);
    }

    public void stopActivity(Activity activity) {
        sAnalyticsSender.stopActivity(activity);
    }

    public void initAnalytics(Context context) {
        sAnalyticsSender.initAnalytics(context);
    }
}