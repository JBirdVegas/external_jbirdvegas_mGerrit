package com.jbirdvegas.mgerrit.objects;

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

public class CommitterObject {
    private final String mName;
    private final String mEmail;
    private final String mDate;
    private final String mTimezone;

    private CommitterObject(String name,
                           String email,
                           String date,
                           String timezone) {
        mName = name;
        mEmail = email;
        mDate = date;
        mTimezone = timezone;
    }

    public static CommitterObject getInstance(String name,
                              String email,
                              String date,
                              String timezone) {
        return new CommitterObject(name, email, date, timezone);
    }

    public String getName() {
        return mName;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getDate() {
        return mDate;
    }

    public String getTimezone() {
        return mTimezone;
    }
}
