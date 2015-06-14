package com.jbirdvegas.mgerrit.objects;

/*
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2015
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

import com.google.gerrit.extensions.common.AccountInfo;

public class UserAccountInfo extends AccountInfo {
    public String password;

    public UserAccountInfo(AccountInfo ai) {
        this(ai._accountId, ai.name, ai.email, ai.username, null);
    }

    public UserAccountInfo(int id, String email, String name, String username, String password) {
        super(id);
        this.email = email;
        this.name = name;
        this.username = username;
        this.password = password;
    }

    public int getAccountId() {
        return _accountId;
    }
}
