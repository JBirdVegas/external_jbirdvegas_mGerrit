package com.jbirdvegas.mgerrit.search;

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

import com.jbirdvegas.mgerrit.database.UserChanges;

public class TopicSearch extends SearchKeyword {

    public static final String OP_NAME = "topic";

    static {
        registerKeyword(OP_NAME, TopicSearch.class);
    }

    public TopicSearch(String param) {
        super(OP_NAME, param);
    }

    @Override
    public String buildSearch() {
        return UserChanges.C_TOPIC + " LIKE ?";
    }

    @Override
    public String[] getEscapeArgument() {
        return new String[] { getParam() + '%' };
    }
}
