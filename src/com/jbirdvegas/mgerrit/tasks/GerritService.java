package com.jbirdvegas.mgerrit.tasks;

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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.ProjectsTable;
import com.jbirdvegas.mgerrit.database.SyncTime;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.StartingRequest;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.objects.Project;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GerritService extends IntentService {

    public static final String TAG = "GerritService";

    public static final String URL_KEY = "Url";
    public static final String DATA_TYPE_KEY = "Type";

    public static enum DataTypes { Project }

    private String mCurrentUrl;

    public GerritService() {
        super("");
    }

    /**
     * Creates an IntentService.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public GerritService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mCurrentUrl = intent.getStringExtra(URL_KEY);

        int dataType = intent.getIntExtra(DATA_TYPE_KEY, 0);
        if (dataType == DataTypes.Project.ordinal()) {
            checkSyncTime();
        }
    }

    private void checkSyncTime() {
        long syncInterval = getResources().getInteger(R.integer.projects_sync_interval);
        long lastSync = SyncTime.getValue(this, SyncTime.PROJECTS_LIST_SYNC_TIME);

        long timeNow = System.currentTimeMillis();
        if (timeNow - lastSync > syncInterval) {
            fetchData(new ProjectInserter(this));
        }
    }

    private void fetchData(final DatabaseInserter dbInserter) {

        // Won't be able to actually get JSON response back as it
        //  is improperly formed (junk at start), but requesting raw text and
        //  trimming it should be fine.

        RequestQueue queue = Volley.newRequestQueue(this);
        new StartingRequest(this, mCurrentUrl).sendUpdateMessage();
        /* Not sure whether to create a new request type and have that handle the trimming,
         *  JSON parsing and Database insertion
         */
        StringRequest request = new StringRequest(mCurrentUrl,
                new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                String json = s.substring(5);
                dbInserter.insert(json);
                new Finished(GerritService.this, json, GerritService.this.mCurrentUrl);
                SyncTime.setValue(GerritService.this, SyncTime.PROJECTS_LIST_SYNC_TIME,
                        System.currentTimeMillis());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                new ErrorDuringConnection(GerritService.this, volleyError,
                        GerritService.this.mCurrentUrl);
            }
        });
        queue.add(request);
    }

    abstract class DatabaseInserter {
        protected final Context mContext;

        DatabaseInserter(Context context) {
            this.mContext = context;
        }

        abstract void insert(String json);
    }


    class ProjectInserter extends DatabaseInserter {
        ProjectInserter(Context context) {
            super(context);
        }

        @Override
        void insert(String json) {
            List<Project> projectList = new ArrayList<Project>();

            // We cannot use normal Gson parsing here as the keys form part of the data
            // TODO: Write a JSON Volley request parser with head trim support.

            JSONObject projectsJson;
            try {
                projectsJson = new JSONObject(json);

                Iterator stringIterator = projectsJson.keys();
                while (stringIterator.hasNext()) {
                    String path = (String) stringIterator.next();
                    JSONObject projJson = projectsJson.getJSONObject(path);
                    String kind = projJson.getString(JSONCommit.KEY_KIND);
                    String id = projJson.getString(JSONCommit.KEY_ID);
                    projectList.add(Project.getInstance(path, kind, id));
                }
            } catch (JSONException e) {
                e.printStackTrace(); // TODO: Send ParseError Message
            }
            Collections.sort(projectList);
            ProjectsTable.insertProjects(GerritService.this, projectList);
        }
    }
}
