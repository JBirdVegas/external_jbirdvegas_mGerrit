package com.jbirdvegas.mgerrit.objects;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.database.Config;
import com.jbirdvegas.mgerrit.search.SearchKeyword;

import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A class that helps to deconstruct Gerrit queries and assemble them
 *  when necessary. This allows for setting individual parts of a query
 *  without knowing other query parameters.
 */
public abstract class RequestBuilder implements Parcelable
{
    protected static Context sContext;
    private Pair<String, String> mAuthDetails;
    private int mLimit;

    public static void setContext(Context context) {
        RequestBuilder.sContext = context;
    }

    /**
     * @param limit The maximum number of changes to include in the response
     */
    public void setLimit(int limit) {
        this.mLimit = limit;
    }

    public int getLimit() {
        return mLimit;
    }

    public void setAuthenticationDetails(String username, String password) {
        mAuthDetails = new Pair<>(username, password);
    }

    @Override
    @Nullable
    public String toString()
    {
        StringBuilder builder = new StringBuilder(0).append(Prefs.getCurrentGerrit(sContext));
        builder.append(getPath());
        return builder.toString();
    }

    public boolean equals(String str) {
        return str != null && str.equals(this.toString());
    }

    // --- Parcelable stuff so we can send this object through intents ---
    @Override
    public int describeContents() {
        return 0;
    }

    public abstract String getPath();

    public abstract String getQuery();
}
