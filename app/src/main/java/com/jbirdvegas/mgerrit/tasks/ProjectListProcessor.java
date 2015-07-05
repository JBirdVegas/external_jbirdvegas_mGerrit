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

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.DatabaseTable;
import com.jbirdvegas.mgerrit.database.ProjectsTable;
import com.jbirdvegas.mgerrit.database.SyncTime;

import java.util.ArrayList;
import java.util.List;

class ProjectListProcessor extends SyncProcessor<List<ProjectInfo>> {

    ProjectListProcessor(Context context, Intent intent) {
        super(context, intent);
    }

    @Override
    int insert(List<ProjectInfo> projects) {
        return ProjectsTable.insertProjects(getContext(), projects);
    }

    @Override
    boolean isSyncRequired(Context context) {
        long syncInterval = context.getResources().getInteger(R.integer.projects_sync_interval);
        long lastSync = SyncTime.getValueForQuery(context, SyncTime.PROJECTS_LIST_SYNC_TIME, null);
        boolean sync = isInSyncInterval(syncInterval, lastSync);
        // If lastSync was within the sync interval then it was recently synced and we don't need to again
        if (!sync) return true;

        // Better just make sure that there are projects in the database
        return DatabaseTable.isEmpty(context, ProjectsTable.CONTENT_URI);
    }

    @Override
    void doPostProcess(List<ProjectInfo> data) {
        SyncTime.setValue(mContext, SyncTime.PROJECTS_LIST_SYNC_TIME,
                System.currentTimeMillis(), null);
    }

    @Override
    int count(List<ProjectInfo> projects) {
        if (projects != null) return projects.size();
        else return 0;
    }

    @Override
    protected List<ProjectInfo> getData(GerritApi gerritApi)
            throws RestApiException {
        return gerritApi.projects().list().get();
    }
}
