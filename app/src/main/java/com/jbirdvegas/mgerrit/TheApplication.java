package com.jbirdvegas.mgerrit;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;

import com.jbirdvegas.mgerrit.database.DatabaseFactory;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.helpers.ThemeHelper;
import com.jbirdvegas.mgerrit.message.GerritChanged;
import com.jbirdvegas.mgerrit.objects.CacheManager;
import com.jbirdvegas.mgerrit.tasks.GerritService;

import org.greenrobot.eventbus.EventBus;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2013
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
public class TheApplication extends Application
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences mPrefs;

    @Override
    public void onCreate() {
        super.onCreate();
        // Set up Crashlytics, disabled for debug builds
        BuildConfigurations.applicationOnCreate(this);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        // Don't spam logs with no subscriber event messages as some are used only on tablet devices
        EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).installDefaultEventBus();

        CacheManager.init(this);

        requestServerVersion(false);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Callback to be invoked when the Gerrit instance changes in the
     * sharedPreferences
     *
     * @param newGerrit The URL to the new Gerrit instance
     */
    private void onGerritChanged(String newGerrit) {
        DatabaseFactory.changeGerrit(this, newGerrit);

        // Unset the project - we don't track these across Gerrit instances
        PrefsFragment.setCurrentProject(this, null);
        PrefsFragment.clearTrackingUser(this);

        EventBus.getDefault().postSticky(new GerritChanged(newGerrit));

        requestServerVersion(false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PrefsFragment.GERRIT_URL_KEY)) onGerritChanged(PrefsFragment.getCurrentGerrit(this));
        if (key.equals(PrefsFragment.APP_THEME)) {
            ThemeHelper.setTheme(this);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * Starts a request to check the server version
     */
    public void requestServerVersion(boolean force) {
        Intent it = new Intent(this, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.GetVersion);
        it.putExtra(GerritService.FORCE_UPDATE, force);
        startService(it);
    }
}
