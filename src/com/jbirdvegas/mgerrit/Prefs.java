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

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.jbirdvegas.mgerrit.objects.CommitterObject;

import java.util.LinkedList;
import java.util.TimeZone;

public class Prefs extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    private static final CharSequence CARDS_UI_KEY = "open_source_lib_cards_ui";
    private static final CharSequence NINE_OLD_ANDROIDS_KEY = "open_source_lib_nine_old_androids";
    private static final CharSequence AOSP_VOLLEY = "open_source_aosp_volley";
    private static final CharSequence APACHE_COMMONS_KEY = "open_source_apache_commons";
    public static final String GERRIT_KEY = "gerrit_instances_key";
    public static final String ANIMATION_KEY = "animation_key";
    private static final String SERVER_TIMEZONE_KEY = "server_timezone";
    private static final String LOCAL_TIMEZONE_KEY = "local_timezone";
    public static final String CURRENT_PROJECT = "current_project";
    public static final String TRACKING_USER = "committer_being_tracked";
    private CheckBoxPreference mAnimation;

    private Preference mGerritSwitcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        // View CardsUI website
        findPreference(CARDS_UI_KEY).setOnPreferenceClickListener(this);
        // View NineOldAndroids website
        findPreference(NINE_OLD_ANDROIDS_KEY).setOnPreferenceClickListener(this);
        // View AOSP's Volley website
        findPreference(AOSP_VOLLEY).setOnPreferenceClickListener(this);
        // View Apache Commons Codec
        findPreference(APACHE_COMMONS_KEY).setOnPreferenceClickListener(this);

        // select gerrit instance
        mGerritSwitcher = findPreference(GERRIT_KEY);
        mGerritSwitcher.setSummary(getCurrentGerrit(getActivity()));
        mGerritSwitcher.setOnPreferenceClickListener(this);
        mGerritSwitcher.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setSummary((CharSequence) o);
                Toast.makeText(preference.getContext(),
                        new StringBuilder(0)
                                .append(getString(R.string.using_gerrit_toast))
                                .append(' ')
                                .append(o)
                                .toString(),
                        Toast.LENGTH_LONG).show();
                return true;
            }
        });
        // Allow disabling of Google Now style animations
        ((CheckBoxPreference) findPreference(ANIMATION_KEY))
                .setChecked(getAnimationPreference(this.getActivity()));
        ListPreference serverTimeZoneList = (ListPreference) findPreference(SERVER_TIMEZONE_KEY);
        // Allow changing assumed TimeZone for server
        serverTimeZoneList.setEntryValues(TimeZone.getAvailableIDs());
        LinkedList<CharSequence> timeZones = new LinkedList<CharSequence>();
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
    }

    /**
     * Used to get current gerrit instance base url
     *
     * @param context needed for SharedPreferences
     * @return url of preferred gerrit instance
     */
    public static String getCurrentGerrit(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(GERRIT_KEY, StaticWebAddress.HTTP_GERRIT_AOKP_CO);
    }

    public static void setCurrentGerrit(Context context, String gerritInstanceUrl) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
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
            DialogFragment newFragment = new GerritSwitcher();
            String tag = getResources().getString(R.string.add_gerrit_team);
            newFragment.show(this.getFragmentManager(), tag);
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
            prefs.edit().putString(CURRENT_PROJECT, project).commit();
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
            prefs.edit().putInt(TRACKING_USER, committer).commit();
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
}
