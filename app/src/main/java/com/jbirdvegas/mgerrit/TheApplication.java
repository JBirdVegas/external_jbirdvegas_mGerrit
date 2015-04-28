package com.jbirdvegas.mgerrit;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.jbirdvegas.mgerrit.database.DatabaseFactory;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.message.GerritChanged;
import com.jbirdvegas.mgerrit.requestbuilders.RequestBuilder;
import com.jbirdvegas.mgerrit.tasks.GerritService;

import de.greenrobot.event.EventBus;

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
        // Ensure Gerrit URL has a context set
        RequestBuilder.setContext(this);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        // Don't spam logs with no subscriber event messages as some are used only on tablet devices
        EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).installDefaultEventBus();

        requestServerVersion();
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
    public void onGerritChanged(String newGerrit) {
        DatabaseFactory.changeGerrit(this, newGerrit);

        // Unset the project - we don't track these across Gerrit instances
        PrefsFragment.setCurrentProject(this, null);
        PrefsFragment.clearTrackingUser(this);

        EventBus.getDefault().post(new GerritChanged(newGerrit));

        requestServerVersion();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PrefsFragment.GERRIT_URL_KEY)) onGerritChanged(PrefsFragment.getCurrentGerrit(this));
        if (key.equals(PrefsFragment.APP_THEME)) {
            this.setTheme(PrefsFragment.getCurrentThemeID(this));
        }
    }

    /**
     * Starts a request to check the server version
     */
    private void requestServerVersion() {
        Intent it = new Intent(this, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.GetVersion);
        startService(it);
    }
}
