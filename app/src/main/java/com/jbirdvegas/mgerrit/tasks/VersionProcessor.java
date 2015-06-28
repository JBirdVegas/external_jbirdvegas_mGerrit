package com.jbirdvegas.mgerrit.tasks;

import android.content.Context;
import android.content.Intent;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.database.Config;
import com.jbirdvegas.mgerrit.requestbuilders.ConfigEndpoints;

import java.util.regex.Matcher;

/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2014
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
public class VersionProcessor extends SyncProcessor<String> {

    VersionProcessor(Context context, Intent intent) {
        super(context, intent);
    }

    @Override
    int insert(String data) {
        // Trim off the junk beginning and the quotes around the version number
        Pattern p = Pattern.compile("\"([^\"]+)\"");
        Matcher m = p.matcher(data);
        if (m.find()) {
            Config.setValue(getContext(), Config.KEY_VERSION, m.group(1));
        } else {
            Config.setValue(getContext(), Config.KEY_VERSION, data);
        }
        return 1;
    }

    @Override
    boolean isSyncRequired(Context context) {
        // Look up the database to see if we have previously saved the version
        return Config.getValue(context, Config.KEY_VERSION) == null;
    }

    @Override
    Class<String> getType() {
        return String.class;
    }

    @Override
    int count(String version) {
        if (version != null) return 1;
        else return 0;
    }

    @Override
    String getData(GerritApi gerritApi) throws RestApiException {
        String version = gerritApi.config().server().getVersion();
        if ("<2.8".equals(version)) version = Config.VERSION_DEFAULT;
        return version;
    }

    @Override
    protected void fetchData() {
        GerritApi gerritApi = getGerritApiInstance(true);
        try {
            onResponse(getData(gerritApi));
        } catch (RestApiException e) {
            onResponse(Config.VERSION_DEFAULT);
            handleRestApiException(e);
        }
    }
}
