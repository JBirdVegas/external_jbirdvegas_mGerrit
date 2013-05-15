package com.jbirdvegas.mgerrit;

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

public class StaticWebAddress {
    private static String GERRIT_INSTANCE_WEBSITE = null;
    public static final String HTTP_GERRIT_AOKP_CO = "http://gerrit.aokp.co/";
    private static final String CHANGES_QUERY = "changes/?q=";
    private static String STATUS_QUERY = CHANGES_QUERY + "status:";

    public static String getGERRIT_INSTANCE_WEBSITE() {
        if (GERRIT_INSTANCE_WEBSITE == null) {
            return HTTP_GERRIT_AOKP_CO;
        } else
            return GERRIT_INSTANCE_WEBSITE;
    }

    public static String getStatusQuery() {
        return STATUS_QUERY;
    }

    public static void setGERRIT_INSTANCE_WEBSITE(String gerrit_instance_website) {
        GERRIT_INSTANCE_WEBSITE = gerrit_instance_website;
    }

    public static String getChangesQuery() {
        return getGERRIT_INSTANCE_WEBSITE() + CHANGES_QUERY;
    }

    public static String getQuery() {
        return CHANGES_QUERY;
    }
}
