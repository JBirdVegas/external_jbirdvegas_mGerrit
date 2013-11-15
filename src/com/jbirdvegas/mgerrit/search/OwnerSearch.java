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

import android.util.Pair;

import com.jbirdvegas.mgerrit.database.UserChanges;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OwnerSearch extends SearchKeyword {

    public static final String OP_NAME = "owner";

    static {
        registerKeyword(OP_NAME, OwnerSearch.class);
    }

    public OwnerSearch(String param) {
        super(OP_NAME, param);
    }

    @Override
    public String buildSearch() {
        String param = getParam();
        if (param.contains("<") && param.contains(">")) {
            // Appears to be a multi-part email
            return UserChanges.C_NAME  + " = ? AND " + UserChanges.C_EMAIL + " = ?";
        } else if (param.contains("@")) {
            // Appears to be an email address
            return UserChanges.C_EMAIL + " = ?";
        } else if (param.matches("^-?\\d+$")) {
            // Appears to be a User ID
            return UserChanges.C_OWNER + " = ?";
        } else {
            return UserChanges.C_NAME + " = ?";
        }
    }

    @Override
    public String[] getEscapeArgument() {
        String param = getParam();
        if (param.contains("<") && param.contains(">")) {
            Pair<String, String> pair = processMultiPartOwner();
            return new String[] { pair.first, pair.second };
        }
        return super.getEscapeArgument();
    }

    /**
     * Process Strings such as "John Doe <john@example.com>"
     * @return A pair in the form (username, email). For the
     *  above example it would be (John Doe, john@example.com)
     */
    private Pair<String, String> processMultiPartOwner() {
        String owner = getParam();
        Pattern r_username = Pattern.compile("[^<>]+");
        Matcher m = r_username.matcher(owner);
        int i = 0;
        String [] user_email = new String[2];
        while (m.find() && i < 2) {
            user_email[i] = m.group(0).trim();
            i++;
        }
        return new Pair<>(user_email[0], user_email[1]);
    }
}
