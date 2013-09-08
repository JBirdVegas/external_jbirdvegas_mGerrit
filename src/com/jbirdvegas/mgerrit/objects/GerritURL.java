package com.jbirdvegas.mgerrit.objects;

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
    private boolean mListProjects = false;

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

    // Setting this will ignore all change related parts of the query URL
    public void listProjects() {
        mListProjects = true;
    }

    @Override
    public String toString()
    {
        boolean addPlus = false;

        // Sanity checking, this value REALLY should be set.
        if (sGerritBase == null) {
            throw new NullPointerException("Base Gerrit URL is null, did you forget to set one?");
        }

        if (mListProjects) {
            return new StringBuilder(0)
                    .append(sGerritBase)
                    .append("projects/?d")
                    .toString();
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

    public boolean equals(String str) {
        return this.toString().equals(str);
    }
}
