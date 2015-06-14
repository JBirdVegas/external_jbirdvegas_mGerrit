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

import android.content.Context;
import android.content.Intent;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.DatabaseTable;
import com.jbirdvegas.mgerrit.database.ProjectsTable;
import com.jbirdvegas.mgerrit.database.SyncTime;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.objects.EventQueue;
import com.jbirdvegas.mgerrit.objects.GerritMessage;
import com.jbirdvegas.mgerrit.objects.Project;
import com.jbirdvegas.mgerrit.requestbuilders.ProjectEndpoints;
import com.jbirdvegas.mgerrit.objects.Projects;
import com.jbirdvegas.mgerrit.requestbuilders.RequestBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ProjectListProcessor extends SyncProcessor<Projects> {

    private final RequestBuilder mUrl;
    private final Intent mIntent;

    ProjectListProcessor(Context context, Intent intent) {
        super(context, intent);
        mUrl = ProjectEndpoints.get();
        mIntent = intent;
    }

    @Override
    int insert(Projects projects) {
        return ProjectsTable.insertProjects(getContext(), projects);
    }

    @Override
    boolean isSyncRequired(Context context) {
        long syncInterval = context.getResources().getInteger(R.integer.projects_sync_interval);
        long lastSync = SyncTime.getValueForQuery(context, SyncTime.PROJECTS_LIST_SYNC_TIME, mUrl.toString());
        boolean sync = isInSyncInterval(syncInterval, lastSync);
        // If lastSync was within the sync interval then it was recently synced and we don't need to again
        if (!sync) return true;

        // Better just make sure that there are projects in the database
        return DatabaseTable.isEmpty(context, ProjectsTable.CONTENT_URI);
    }

    @Override
    Class<Projects> getType() {
        return Projects.class;
    }

    @Override
    void doPostProcess(Projects data) {
        SyncTime.setValue(mContext, SyncTime.PROJECTS_LIST_SYNC_TIME,
                System.currentTimeMillis(), mUrl.toString());
    }

    @Override
    int count(Projects projects) {
        if (projects != null) return projects.size();
        else return 0;
    }

    @Override
    protected void fetchData(RequestQueue queue) {
        Response.Listener<Projects> listener = getListener(mUrl.toString());

        GerritApi gerritApi = getGerritApiInstance(true);
        try {
            List<ProjectInfo> pl = gerritApi.projects().list().get();
            Projects projects = new Projects(pl);
            listener.onResponse(projects);
        } catch (RestApiException e) {
            GerritMessage ev = new ErrorDuringConnection(mIntent, mUrl.toString(), null, e);
            // Make sure the sign in activity (if started above) will receive the ErrorDuringConnection message by making it sticky
            EventQueue.getInstance().enqueue(ev, true);
        }
    }
}
