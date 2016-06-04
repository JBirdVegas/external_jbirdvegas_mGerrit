/*
 * Copyright (C) 2016 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2016
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

package com.jbirdvegas.mgerrit.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;

/**
 * Theme specific SharedPreference functionality
 */
public class ThemeHelper {
    public static String getCurrentTheme(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PrefsFragment.APP_THEME,
                context.getResources().getString(R.string.theme_light_value));
    }

    public static boolean usingLightTheme(Context context) {
        String themeName = getCurrentTheme(context);
        return themeName.equals(context.getResources().getString(R.string.theme_light_value));
    }

    /**
     * Set the theme based on what is saved in the preferences.
     * This uses the setDefaultNightMode in the support library to set whether we want to use night
     *  mode or not
     * @param context An activity or application context to apply the theme to
     */
    public static void setTheme(Context context) {
        setTheme(context, getCurrentTheme(context));
    }

    // See: http://android-developers.blogspot.com.au/2016/02/android-support-library-232.html
    public static void setTheme(Context context, String pref) {
        Resources res = context.getResources();
        if (pref.equalsIgnoreCase(res.getString(R.string.theme_dark_value))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (pref.equalsIgnoreCase(res.getString(R.string.theme_light_value))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else  {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        }
        context.setTheme(R.style.Theme_Base);
    }
}
