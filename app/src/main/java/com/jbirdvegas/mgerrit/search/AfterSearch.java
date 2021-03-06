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

import org.joda.time.DateTime;

public class AfterSearch extends AgeSearch {

    public static final String OP_NAME = "after";

    static {
        registerKeyword(OP_NAME, AfterSearch.class);
        registerKeyword("since", AfterSearch.class);
    }

    public AfterSearch(String param) {
        super(param, ">=");
    }

    public AfterSearch(DateTime instant) {
        super(instant, ">=");
    }

    @Override
    public String toString() {
        return super.toString(OP_NAME);
    }

    @Override
    public String getGerritQuery(ServerVersion serverVersion) {
        return _getGerritQuery(this, serverVersion);
    }

    static String _getGerritQuery(AgeSearch ageSearch, ServerVersion serverVersion) {
        DateTime instant = ageSearch.getDateTime();
        if (serverVersion != null &&
                serverVersion.isFeatureSupported(ServerVersion.VERSION_BEFORE_SEARCH)) {
            if (instant == null) {
                instant = AgeSearch.getDateTimeFromPeriod(ageSearch.getPeriod());
            }

            return "after:{" + sGerritFormat.print(instant) +'}';
        }
        return "";
    }
}
