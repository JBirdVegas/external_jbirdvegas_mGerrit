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
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Toast;
import com.jbirdvegas.mgerrit.objects.CommitterObject;

import java.util.LinkedList;
import java.util.TimeZone;

public class Prefs extends PreferenceActivity implements Preference.OnPreferenceClickListener {
    private static final CharSequence CARDS_UI_KEY = "open_source_lib_cards_ui";
    private static final CharSequence NINE_OLD_ANDROIDS_KEY = "open_source_lib_nine_old_androids";
    private static final CharSequence AOSP_VOLLEY = "open_source_aosp_volley";
    private static final CharSequence APACHE_COMMONS_KEY = "open_source_apache_commons";
    private static final String GERRIT_KEY = "gerrit_instances_key";
    private static final String ANIMATION_KEY = "animation_key";
    private static final String SAVED_GERRIT_INSTANCES_KEY = "saved_gerrit_instances";
    private static final String SERVER_TIMEZONE_KEY = "server_timezone";
    private static final String LOCAL_TIMEZONE_KEY = "local_timezone";
    private static final String CURRENT_PROJECT = "current_project";
    private CheckBoxPreference mAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        // Action bar Up affordance
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // select gerrit instance
        ListPreference gerritList = (ListPreference) findPreference(GERRIT_KEY);
        gerritList.setSummary(gerritList.getValue());
        gerritList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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
                .setChecked(getAnimationPreference(getApplicationContext()));
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

    public static Intent getStalkerIntent(Context activity, CommitterObject committerObject) {
        return new Intent()
                .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                .putExtra(CardsActivity.KEY_DEVELOPER, committerObject)
                .setClass(activity, GerritControllerActivity.class);
    }

    public static Intent getStalkerIntent(Context activity) {
        return new Intent()
                .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                .setClass(activity, GerritControllerActivity.class);
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
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(CURRENT_PROJECT, project).commit();
    }

    public static String getCurrentProject(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(CURRENT_PROJECT, "");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}