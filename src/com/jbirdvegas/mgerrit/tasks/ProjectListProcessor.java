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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.ProjectsTable;
import com.jbirdvegas.mgerrit.database.SyncTime;
import com.jbirdvegas.mgerrit.objects.Project;
import com.jbirdvegas.mgerrit.objects.Projects;

import java.util.Collections;
import java.util.List;

class ProjectListProcessor extends SyncProcessor<Projects> {

    ProjectListProcessor(Context context, String url) {
        super(context, url);
    }

    @Override
    void insert(Projects projects) {
        List<Project> projectList = projects.getAsList();
        Collections.sort(projectList);
        ProjectsTable.insertProjects(getContext(), projectList);
    }

    @Override
    boolean isSyncRequired() {
        long syncInterval = getContext().getResources().getInteger(R.integer.projects_sync_interval);
        long lastSync = SyncTime.getValue(getContext(), SyncTime.PROJECTS_LIST_SYNC_TIME);
        long timeNow = System.currentTimeMillis();
        return (timeNow - lastSync > syncInterval);
    }

    @Override
    Class<Projects> getType() {
        return Projects.class;
    }
}
