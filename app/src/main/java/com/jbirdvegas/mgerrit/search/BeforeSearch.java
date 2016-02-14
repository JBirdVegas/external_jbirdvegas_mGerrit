package com.jbirdvegas.mgerrit.search;

import com.jbirdvegas.mgerrit.objects.ServerVersion;

import org.joda.time.DateTime;

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
public class BeforeSearch extends AgeSearch {

    public static final String OP_NAME = "before";

    static {
        registerKeyword(OP_NAME, BeforeSearch.class);
        registerKeyword("until", BeforeSearch.class);
    }

    public BeforeSearch(String param) {
        super(param, "<=");
    }

    public BeforeSearch(DateTime dateTime) {
        super(dateTime, "<=");
    }

    @Override
    public String toString() {
        return super.toString(OP_NAME);
    }

    @Override
    public String getGerritQuery(ServerVersion serverVersion) {
        return _getGerritQuery(this, serverVersion);
    }

    public static String _getGerritQuery(AgeSearch ageSearch, ServerVersion serverVersion) {
        DateTime dateTime = ageSearch.getDateTime();
        if (serverVersion != null &&
                serverVersion.isFeatureSupported(ServerVersion.VERSION_BEFORE_SEARCH)) {
            if (dateTime == null) {
                dateTime = AgeSearch.getDateTimeFromPeriod(ageSearch.getPeriod());
            }

            return "before:{" + sGerritFormat.print(dateTime) +'}';
        }
        // Need to leave off the operator and make sure we are using relative format
        /* Gerrit only supports specifying one time unit, so we will normalize the period
         *  into days.
         * After search on the server only supports the parameter in timestamps */
        return AgeSearch.OP_NAME + ":" + String.valueOf(ageSearch.toDays()) + "d";
    }
}
