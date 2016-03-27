package com.jbirdvegas.mgerrit.helpers;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 * Author: Jon Stanford (JBirdVegas), 2013
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

public class AnalyticsHelper {
    public static final String GA_PERFORMANCE = "performance";
    public static final String GA_TIME_TO_LOAD = "time_to_load";
    public static final String GA_CARDS_LOAD_TIME = "cards_loading";
    public static final String GA_APP_OPEN = "app_open";
    public static final String GA_ROM_VERSION = "rom_version";
    public static final String GA_LOG_FAIL = "failure";
    public static final String GA_FAIL_UI = "fail_ui";
    public static final String GA_AUTHORISED_ACTION = "Authorised Action";
    public static final String GA_THEME_SET_ON_OPEN = "ui_theme";
    public static final String EVENT_CHANGE_COMMENT_ADDED = "Change comment added";
    public static final String EVENT_LOGGED_IN = "User signed in";

    public static final String C_SERVER_VERSION = "Server version";
    public static final String C_CHANGE_ID = "Change ID";
    public static final String C_CHANGE_NUMBER = "Change number";


    public static void sendAnalyticsEvent(Context context,
                                          String category, String action, String label, Long value) {
        EasyTracker easyTracker = EasyTracker.getInstance(context);
        easyTracker.send(MapBuilder
                .createEvent(category, action, label, value)
                .build());
        // note this screen as viewed
        easyTracker.send(MapBuilder.createAppView().build());
    }

    public static void setCustomString(String key, String data) {
        Crashlytics.setString(key, data);
    }

    public static void setCustomInt(String key, int data) {
        Crashlytics.setInt(key, data);
    }
}
