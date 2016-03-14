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
            mHelper = new AnalyticsSenderImpl();
        }
        if (mInstance == null) {
            mInstance = new AnalyticsHelper();
        }
        return mInstance;
    }

    public void sendAnalyticsEvent(Context context, String category, String action, String label, Long value) {
        getInstance().mHelper.sendAnalyticsEvent(context, category, action, label, value);
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