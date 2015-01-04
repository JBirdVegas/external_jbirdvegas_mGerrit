package com.jbirdvegas.mgerrit;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Jon Stanford (JBirdVegas), 2013
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.jbirdvegas.mgerrit.objects.CommitterObject;

import org.jetbrains.annotations.Contract;

import java.util.LinkedList;
import java.util.TimeZone;

public class Prefs extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    public static final String GERRIT_KEY = "gerrit_instances_key";
    private static final String ANIMATION_KEY = "animation_key";
    private static final String SERVER_TIMEZONE_KEY = "server_timezone";
    private static final String LOCAL_TIMEZONE_KEY = "local_timezone";
    public static final String CURRENT_PROJECT = "current_project";
    public static final String TRACKING_USER = "committer_being_tracked";
    public static final String APP_THEME = "app_theme";
    private static final String TABLET_MODE = "tablet_layout_mode";
    private static final String DIFF_DEFAULT = "change_diff";
    private static final String WHITESPACE_STYLE_HIGHLIGHTING = "whitespace_style_highlighting";
    private static final String WHITESPACE_STYLE_HIGHLIGHTING_KEY = "whitespace_tab_code_style_highlighting";

    private Preference mGerritSwitcher;
    private Context mContext;

    public enum DiffModes {
        ASK {
            @Override
            public String getSummary(Context context) {
                return context.getResources().getString(R.string.diff_options_ask);
            }
        }, INTERNAL {
            @Override
            public String getSummary(Context context) {
                return context.getResources().getString(R.string.diff_options_internal);
            }
        }, EXTERNAL {
            @Override
            public String getSummary(Context context) {
                return context.getResources().getString(R.string.diff_options_external);
            }
        };

        public String getSummary(Context context) {
            return null;
        }

        public static DiffModes getMode(Context context, String s) {
            Resources r = context.getResources();
            if (s.equals(r.getString(R.string.diff_option_internal))) {
                return INTERNAL;
            } else if (s.equals(r.getString(R.string.diff_option_external))) {
                return EXTERNAL;
            } else {
                return ASK;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        PreferenceCategory libraries = (PreferenceCategory) findPreference("libraries");
        addLibraries(libraries);

        mContext = getActivity();

        // select gerrit instance
        mGerritSwitcher = findPreference(GERRIT_KEY);
        mGerritSwitcher.setSummary(getCurrentGerrit(getActivity()));
        mGerritSwitcher.setOnPreferenceClickListener(this);
        mGerritSwitcher.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setSummary((CharSequence) o);
                Toast.makeText(preference.getContext(),
                        getString(R.string.using_gerrit_toast) + ' ' + o,
                        Toast.LENGTH_LONG).show();
                return true;
            }
        });
        // Allow disabling of Google Now style animations
        ((CheckBoxPreference) findPreference(ANIMATION_KEY))
                .setChecked(getAnimationPreference(mContext));
        ListPreference serverTimeZoneList = (ListPreference) findPreference(SERVER_TIMEZONE_KEY);
        // Allow changing assumed TimeZone for server
        serverTimeZoneList.setEntryValues(TimeZone.getAvailableIDs());
        LinkedList<CharSequence> timeZones = new LinkedList<>();
        for (String tz : TimeZone.getAvailableIDs()) {
            timeZones.add(TimeZone.getTimeZone(tz).getID());
        }
        CharSequence[] zoneEntries = new CharSequence[timeZones.size()];
        serverTimeZoneList.setEntries(timeZones.toArray(zoneEntries));
        // the local timezone may be inaccurate as provided by TimeZone.getDefault()
        // to account for this inconsistency we allow users the change from the device
        // provided localization to user provided localization
        ListPreference localTimeZoneList = (ListPreference) findPreference(LOCAL_TIMEZONE_KEY);
        localTimeZoneList.setEntries(TimeZone.getAvailableIDs());
        localTimeZoneList.setEntryValues(zoneEntries);

        Preference themeSelector = findPreference(APP_THEME);
        setThemeSummary(themeSelector);
        themeSelector.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                String summary = getReadableThemeName(o.toString());
                if (summary != null){
                    preference.setSummary(summary);
                    getActivity().setTheme(getInternalTheme(o.toString()));
                    getActivity().recreate();
                } else {
                    preference.setSummary("");
                }

                return true;
            }
        });

        Preference diffDefault = findPreference(DIFF_DEFAULT);
        DiffModes mode = getDiffDefault(getActivity());
        diffDefault.setSummary(mode.getSummary(mContext));
        diffDefault.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                DiffModes o = DiffModes.getMode(mContext, newValue.toString());
                preference.setSummary(o.getSummary(mContext));
                return true;
            }
        });

        findPreference(WHITESPACE_STYLE_HIGHLIGHTING_KEY).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setHighlightWhitespaceStyle(getActivity(), ((CheckBoxPreference) preference).isChecked());
                return true;
            }
        });
    }

    /**
     * Used to get current gerrit instance base url
     *
     * @param context needed for SharedPreferences
     * @return url of preferred gerrit instance
     */
    public static String getCurrentGerrit(Context context) {
        String[] gerrits = context.getResources().getStringArray(R.array.gerrit_webaddresses);
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(GERRIT_KEY, gerrits[0]);
    }

    public static void setCurrentGerrit(Context context, String gerritInstanceUrl) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(GERRIT_KEY, gerritInstanceUrl)
                .commit();
    }

    /**
     * handles onClick of open source libraries
     *
     * @param preference library user selected
     * @return true if handled
     */
    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.equals(mGerritSwitcher)) {
            Intent intent = new Intent(mContext, GerritSwitcher.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
            return true;
        }
        return launchWebsite(preference);
    }

    /**
     * reads Preference#getSummary() to launch url in browser
     *
     * @param pref selected library preference
     * @return true if launch was successful
     */
    @Contract("null -> false")
    private boolean launchWebsite(Preference pref) {
        if (pref == null
                || !((String) pref.getSummary()).contains("http")) {
            return false;
        }
        Intent launchWebsite = new Intent(Intent.ACTION_VIEW);
        launchWebsite.setData(Uri.parse((String) pref.getSummary()));
        startActivity(launchWebsite);
        return true;
    }

    /**
     * Adds the list of libraries to the preferences, using string arrays
     *  for the titles and the websites.
     * @param libraries
     */
    private void addLibraries(PreferenceCategory libraries) {
        Context context = getActivity();
        String[] libraryTitles = context.getResources().getStringArray(R.array.library_titles);
        String[] libraryWebsites = context.getResources().getStringArray(R.array.library_websites);

        for (int i = 0; i < libraryTitles.length; i++) {
            Preference pref = new Preference(context);
            pref.setTitle(libraryTitles[i]);
            if (i < libraryWebsites.length) {
                pref.setSummary(libraryWebsites[i]);
            }
            pref.setOnPreferenceClickListener(this);
            libraries.addPreference(pref);
        }
    }

    /**
     * Google Now style animation removal
     * @param context used to access SharedPreferences
     * @return if true to show animations false disables
     *         animations
     */
    public static boolean getAnimationPreference(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(ANIMATION_KEY, true);
    }

    public static TimeZone getServerTimeZone(Context context) {
        return TimeZone.getTimeZone(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SERVER_TIMEZONE_KEY, "PST"));
    }

    public static TimeZone getLocalTimeZone(Context context) {
        return TimeZone.getTimeZone(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(LOCAL_TIMEZONE_KEY, TimeZone.getDefault().getID()));
    }

    public static void setCurrentProject(Context context, String project) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String oldProject = prefs.getString(CURRENT_PROJECT, "");
        if (!oldProject.equals(project)) {
            prefs.edit().putString(CURRENT_PROJECT, project).apply();
        }
    }

    public static String getCurrentProject(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(CURRENT_PROJECT, "");
    }

    public static void setTrackingUser(Context context, CommitterObject committer) {
        setTrackingUser(context, committer.getAccountId());
    }

    /**
     * Set a user to be tracked.
     *  Do not set this to clear the tracked user, use {@link clearTrackingUser(Context)} instead.
     * @param context used to access SharedPreferences
     * @param committer The userid of the user to track
     */
    public static void setTrackingUser(Context context, Integer committer) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int oldCommitter = prefs.getInt(TRACKING_USER, -1);
        if (oldCommitter != committer) {
            prefs.edit().putInt(TRACKING_USER, committer).apply();
        }
    }

    /**
     * Untrack the user currently being tracked
     * @param context used to access SharedPreferences
     */
    public static void clearTrackingUser(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(TRACKING_USER).commit();
    }

    public static Integer getTrackingUser(Context context) {
        int userid = PreferenceManager.getDefaultSharedPreferences(context).getInt(TRACKING_USER, -1);
        if (userid == -1) return null;
        return userid;
    }

    public static String getCurrentTheme(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(APP_THEME,
                context.getResources().getString(R.string.theme_light_value));
    }

    public static int getCurrentThemeID(Context context) {
        String themename = PreferenceManager.getDefaultSharedPreferences(context).getString(APP_THEME,
                context.getResources().getString(R.string.theme_light_value));
        Resources res = context.getResources();
        if (themename.equalsIgnoreCase(res.getString(R.string.theme_dark_value))) {
            return R.style.Theme_Dark;
        } else {
            return R.style.Theme_Light;
        }
    }

    private String getReadableThemeName(String pref) {
        Context context = getActivity();
        String[] entries = context.getResources().getStringArray(R.array.themes_entries);
        String[] entriesValues = context.getResources().getStringArray(R.array.themes_entry_values);
        for (int i = 0; i < entries.length; i++) {
            if (pref.equalsIgnoreCase(entriesValues[i])) {
                return entries[i];
            }
        }
        return null;
    }

    private void setThemeSummary(Preference preference) {
        String summary = getReadableThemeName(getCurrentTheme(getActivity()));
        if (summary != null) {
            preference.setSummary(summary);
        } else {
            preference.setSummary("");
        }
    }

    private int getInternalTheme(String pref) {
        Resources res = getActivity().getResources();
        if (pref.equalsIgnoreCase(res.getString(R.string.theme_dark_value))) {
            return R.style.Theme_Dark;
        } else {
            return R.style.Theme_Light;
        }
    }

    public static boolean isTabletMode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(TABLET_MODE, false);
    }

    public static void setTabletMode(Context context, boolean tabletMode) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(TABLET_MODE, tabletMode).commit();
    }

    public static DiffModes getDiffDefault(Context context) {
        Resources r = context.getResources();
        String soption = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(DIFF_DEFAULT, r.getString(R.string.diff_option_internal));
        return DiffModes.getMode(context, soption);
    }

    public static void setGerritInstanceByName(Context context, String gerrit) {
        String[] gerritNames = context.getResources().getStringArray(R.array.gerrit_names);
        for (int i = 0; i < gerritNames.length; i++) {
            if (gerrit.compareToIgnoreCase(gerritNames[i]) == 0) {
                setCurrentGerrit(context, context.getResources().getStringArray(R.array.gerrit_webaddresses)[i]);
            }
        }
    }

    public static boolean highlightWhitespaceStyle(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(WHITESPACE_STYLE_HIGHLIGHTING, true);
    }

    public static void setHighlightWhitespaceStyle(Context context, boolean shouldHighlight) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(WHITESPACE_STYLE_HIGHLIGHTING, shouldHighlight).commit();
    }
}