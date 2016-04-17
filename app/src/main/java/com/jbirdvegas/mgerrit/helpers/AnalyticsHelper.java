package com.jbirdvegas.mgerrit.helpers;

import android.app.Activity;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

public class AnalyticsHelper {
    public static final String GA_APP_OPEN = "app_open";
    public static final String GA_ROM_VERSION = "rom_version";
    public static final String GA_AUTHORISED_ACTION = "Authorised Action";
    public static final String GA_THEME_SET_ON_OPEN = "ui_theme";
    public static final String EVENT_CHANGE_COMMENT_ADDED = "Change comment added";
    public static final String EVENT_LOGGED_IN = "User signed in";

    public static final String C_SERVER_VERSION = "Server version";
    public static final String C_CHANGE_ID = "Change ID";
    public static final String C_CHANGE_NUMBER = "Change number";
    public static final String C_GERRIT_INSTANCE = "Gerrit Instance";

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

    public static void setCustomString(String key, String data) {
        Crashlytics.setString(key, data);
    }

    public static void setCustomInt(String key, int data) {
        Crashlytics.setInt(key, data);
    }
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