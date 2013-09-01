package com.jbirdvegas.mgerrit.objects;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (p4r4n01d), 2013
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

import com.jbirdvegas.mgerrit.StaticWebAddress;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * A class that helps to deconstruct Gerrit queries and assemble them
 *  when necessary. This allows for setting individual parts of a query
 *  without knowing other query parameters.
 */
public class GerritURL
{
    private static String sGerritBase;
    private static String sProject = "";
    private String mStatus = "";
    private String mEmail = "";
    private String mCommitterState = "";
    private boolean mRequestDetailedAccounts = false;

    public static void setGerrit(String mGerritBase) {
        GerritURL.sGerritBase = mGerritBase;
    }

    public static void setProject(String project) {
        if (project == null) project = "";
        sProject = project;
    }

    public void setStatus(String status) {
        if (status == null) status = "";
        mStatus = status;
    }

    public void setEmail(String email) {
        if (email == null) email = "";
        mEmail = email;
    }

    public void setCommitterState(String committerState) {
        if (committerState == null) committerState = "";
        mCommitterState = committerState;
    }

    public void setRequestDetailedAccounts(boolean requestDetailedAccounts) {
        mRequestDetailedAccounts = requestDetailedAccounts;
    }

    @Override
    public String toString()
    {
        boolean addPlus = false;

        // Sanity checking, this value REALLY should be set.
        if (sGerritBase == null) {
            throw new NullPointerException("Base Gerrit URL is null, did you forget to set one?");
        }

        StringBuilder builder = new StringBuilder(0)
                .append(sGerritBase)
                .append(StaticWebAddress.getQuery())
                .append("(");

        if (!"".equals(mStatus))
        {
            builder.append(JSONCommit.KEY_STATUS)
                    .append(":")
                    .append(mStatus);
            addPlus = true;
        }

        if (!"".equals(mCommitterState) && !"".equals(mEmail))
        {
            if (addPlus) builder.append('+');
            builder.append(mCommitterState)
                    .append(':')
                    .append(mEmail);
            addPlus = true;
        }

        try {
            if (!"".equals(sProject))
            {
                if (addPlus) builder.append('+');
                builder.append(JSONCommit.KEY_PROJECT)
                        .append(":")
                        .append(URLEncoder.encode(sProject, "UTF-8"));
                addPlus = true;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        builder.append(")");

        if (mRequestDetailedAccounts) {
            builder.append(JSONCommit.DETAILED_ACCOUNTS_ARG);
        }

        return builder.toString();
    }
}