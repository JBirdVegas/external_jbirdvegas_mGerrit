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
    public static final String GA_THEME_SET_ON_OPEN = "ui_theme";
    public static final String ACTION_CHANGELOG_SAVE_FAIL = "changelog_save";
    public static final String EVENT_CHANGELOG_FILE_NULL = "goo_file_null";
    public static final String EVENT_CHANGELOG_SHORT_URL_NULL = "goo_shorturl_null";
    public static final String EVENT_SYNCTIME_CLEAR_FAIL = "delete_url_failed:";

    public static void sendAnalyticsEvent(Context context,
                                          String category, String action, String label, Long value) {
        EasyTracker easyTracker = EasyTracker.getInstance(context);
        easyTracker.send(MapBuilder
                .createEvent(category, action, label, value)
                .build());
        // note this screen as viewed
        easyTracker.send(MapBuilder.createAppView().build());
    }

}
