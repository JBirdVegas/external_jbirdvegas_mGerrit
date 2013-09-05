package com.jbirdvegas.mgerrit.helpers;

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

import android.os.Environment;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class GerritTeamsHelper {
    private static final String TAG = GerritTeamsHelper.class.getSimpleName();
    private static final String KEY_TEAM_NAME = "team_name";
    private static final String KEY_TEAM_URL = "team_url";
    private static final String KEY_TIMESTAMP = "timestamp";
    private final List<GerritInstance> mInstanceList;
    private final List<String> mGerritNamesList;
    private final List<String> mGerritUrlsList;

    private static final String OUR_DATA = "/data/com.jbirdvegas.mgerrit/gerrits";
    public static File mExternalCacheDir
            = new File(Environment.getDataDirectory().getAbsolutePath() + OUR_DATA);

    public GerritTeamsHelper() {
        ensureDirs();
        mInstanceList = getAllTeams();
        mGerritNamesList = getAllNames();
        mGerritUrlsList = getAllUrls();
    }

    private void ensureDirs() {
        if (!mExternalCacheDir.isDirectory()) {
            mExternalCacheDir.delete();
        }

        if (!mExternalCacheDir.exists()) {
            mExternalCacheDir.mkdirs();
        }
    }

    public List<String> getGerritNamesList() {
        return mGerritNamesList;
    }

    public List<String> getGerritUrlsList() {
        return mGerritUrlsList;
    }

    public class GerritInstance {
        private String mTeamName;
        private String mTeamUrl;
        private long mTimestamp;

        GerritInstance(String teamName, String teamUrl, long timestamp) {
            mTeamName = teamName;
            mTeamUrl = teamUrl;
            mTimestamp = timestamp;
        }

        public GerritInstance(JSONObject jsonObject) {
            try {
                mTeamName = jsonObject.getString(KEY_TEAM_NAME);
                mTeamUrl = jsonObject.getString(KEY_TEAM_URL);
                mTimestamp = jsonObject.getLong(KEY_TIMESTAMP);
            } catch (JSONException e) {
                throw new ExceptionInInitializerError("Failed to parse json into gerrit instance");
            }

        }

        public String getTeamName() {
            return mTeamName;
        }

        public String getTeamUrl() {
            return mTeamUrl;
        }

        public long getTimestamp() {
            return mTimestamp;
        }
    }
    public static void saveTeam(String teamName, String teamUrl) {
        writeTeamToCache(teamName, teamUrl);
    }

    private static void writeTeamToCache(String teamName, String teamUrl) {
        File teamPath = new File(mExternalCacheDir.getAbsolutePath() + '/' + teamName);
        if (teamPath.exists()) {
            teamPath.delete();
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(teamPath));
            try {
                writer.write(new JSONObject()
                        .put(KEY_TEAM_NAME, teamName)
                        .put(KEY_TEAM_URL, teamUrl)
                        .put(KEY_TIMESTAMP, System.currentTimeMillis())
                        .toString());
            } catch (JSONException e) {
                Log.e(TAG, "Failed to encode gerrit info to json", e);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to write file to the cache", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // let it go
                }
            }
        }
    }

    private final List<GerritInstance> getAllTeams() {
        File[] files = mExternalCacheDir.listFiles();
        if (files == null) {
            return new LinkedList<GerritInstance>();
        }
        List<GerritInstance> instances = new LinkedList<GerritInstance>();
        for (File file : files) {
            try {
                instances.add(readFileToGerritInstance(file));
            } catch (JSONException e) {
                Log.e(TAG, "Failed to read stored Gerrit instance", e);
            }
        }
        return instances;
    }

    private final List<String> getAllNames() {
        List<String> names = new LinkedList<String>();
        for (GerritInstance instance : mInstanceList) {
            names.add(instance.getTeamName());
        }
        return names;
    }

    private final List<String> getAllUrls() {
        List<String> urls = new LinkedList<String>();
        for (GerritInstance instance : mInstanceList) {
            urls.add(instance.getTeamUrl());
        }
        return urls;
    }

    private GerritInstance readFileToGerritInstance(File file) throws JSONException {
        FileInputStream inputStream = null;
        BufferedReader reader = null;
        StringBuilder total = new StringBuilder(0);
        try {
            inputStream = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                total.append(line);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found!", e);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read instances file", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to read instance from stream", e);
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // let it go
                }
            }
        }
        return new GerritInstance(new JSONObject(total.toString()));
    }
}