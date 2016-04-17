
package com.jbirdvegas.mgerrit.helpers;

import android.app.Activity;
import android.content.Context;

public class AnalyticsSenderImpl implements AnalyticsSender {
    @Override
    public void sendAnalyticsEvent(Context context, String category, String action, String label, Long value) {
        // no opt
    }

    @Override
    public void startActivity(Activity activity) {
        // no opt
    }

    @Override
    public void stopActivity(Activity activity) {
        // no opt
    }

    @Override
    public void initAnalytics(Context context) {
        // no opt
    }

    @Override
    void setCustomString(String key, String data) {
       // no opt
    }

    @Override
    void setCustomInt(String key, int data) {
        // no opt
    }

    @Override
    public void logException(Exception e) {
        // no opt
    }
}