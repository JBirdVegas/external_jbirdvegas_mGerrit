package com.jbirdvegas.mgerrit.search;

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

import com.jbirdvegas.mgerrit.objects.ServerVersion;

import org.joda.time.Instant;

public class AfterSearch extends AgeSearch {

    public static final String OP_NAME = "after";

    static {
        registerKeyword(OP_NAME, AfterSearch.class);
        registerKeyword("since", AfterSearch.class);
    }

    public AfterSearch(String param) {
        super(param, ">=");
    }

    public AfterSearch(long timestamp) {
        super(timestamp, ">=");
    }

    @Override
    public String toString() {
        Instant instant = getInstant();
        if (instant == null) {
            instant = AgeSearch.getInstantFromPeriod(getPeriod());
        }
        return OP_NAME + ":" + instant.toString();
    }

    @Override
    public String getGerritQuery(ServerVersion serverVersion) {
        return _getGerritQuery(this, serverVersion);
    }

    protected static String _getGerritQuery(AgeSearch ageSearch, ServerVersion serverVersion) {
        Instant instant = ageSearch.getInstant();
        if (serverVersion != null &&
                serverVersion.isFeatureSupported(ServerVersion.VERSION_BEFORE_SEARCH)) {
            if (instant == null) {
                instant = AgeSearch.getInstantFromPeriod(ageSearch.getPeriod());
            }

            return "after:{" + sInstantFormatter.print(instant) +'}';
        }
        return "";
    }
}
