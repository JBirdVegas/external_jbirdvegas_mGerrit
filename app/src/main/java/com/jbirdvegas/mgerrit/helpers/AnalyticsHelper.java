package com.jbirdvegas.mgerrit.helpers;

import android.app.Activity;
import android.content.Context;

public class AnalyticsHelper {
    private static AnalyticsHelper mInstance;
    private static AnalyticsSender mHelper;

    private AnalyticsHelper() {
        // keep out
    }

    public static AnalyticsHelper getInstance() {
        if (mHelper == null) {
            // sender implementation depends on productFlavor.  `googlePlay` implements the analytics where
            // `noAnalytics` stubs all methods to avoid collecting analytics for those who prefer to not be tracked
            mHelper = new AnalyticsSenderImpl();
        }
        if (mInstance == null) {
            mInstance = new AnalyticsHelper();
        }
        return mInstance;
    }

    public void sendAnalyticsEvent(Context context, String category, String action, String label, Long value) {
        mHelper.sendAnalyticsEvent(context, category, action, label, value);
    }

    public void startActivity(Activity activity) {
        mHelper.startActivity(activity);
    }

    public void stopActivity(Activity activity) {
        mHelper.stopActivity(activity);
    }

    public void initAnalytics(Context context) {
        mHelper.initAnalytics(context);
    }
}