package com.jbirdvegas.mgerrit.objects;

import android.content.Context;
import android.os.Parcelable;
import android.util.Pair;

import com.jbirdvegas.mgerrit.Prefs;

import org.jetbrains.annotations.Nullable;

/**
 * A class that helps to deconstruct Gerrit queries and assemble them
 *  when necessary. This allows for setting individual parts of a query
 *  without knowing other query parameters.
 */
public abstract class RequestBuilder implements Parcelable
{
    protected static Context sContext;
    private int mLimit;
    private boolean mAuthenticating;

    public static void setContext(Context context) {
        RequestBuilder.sContext = context;
    }

    public RequestBuilder() { }

    public RequestBuilder(RequestBuilder url) {
        mLimit = url.mLimit;
        mAuthenticating = url.mAuthenticating;
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

    public boolean isAuthenticating() {
        return mAuthenticating;
    }

    public void setAuthenticating(boolean authenticate) {
        this.mAuthenticating = authenticate;
    }

    @Override
    @Nullable
    public String toString()
    {
        StringBuilder builder = new StringBuilder(0).append(Prefs.getCurrentGerrit(sContext));
        if (mAuthenticating) builder.append("a/");
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

    public String getQuery() {
        return null;
    }

    public String getStatus() {
        return null;
    }
}
