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

import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.objects.ServerVersion;

public class NumberSearch extends SearchKeyword {

    public static final String OP_NAME = "#";

    static {
        registerKeyword(OP_NAME, NumberSearch.class);
        registerKeyword("no", NumberSearch.class);
    }

    public NumberSearch(String param) {
        super(OP_NAME, param);
    }

    @Override
    public String buildSearch() {
        return UserChanges.C_COMMIT_NUMBER + " = ?";
    }

    @Override
    public String getGerritQuery(ServerVersion serverVersion) {
        return toString();
    }

    @Override
    public String toString() {
        return OP_NAME + getParam();
    }

    @Override
    public boolean multipleResults() {
        return false;
    }
}
