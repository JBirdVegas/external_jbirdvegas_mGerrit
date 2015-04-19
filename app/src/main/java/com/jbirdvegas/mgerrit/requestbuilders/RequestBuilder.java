package com.jbirdvegas.mgerrit.requestbuilders;

import android.content.Context;
import android.os.Parcelable;

import com.jbirdvegas.mgerrit.fragments.PrefsFragment;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;

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
    public RequestBuilder setLimit(int limit) {
        this.mLimit = limit;
        return this;
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
    public String toString() {
        //TODO: Check if current Gerrit ends with /a/ and if so set mAuthenticating to true
        // but don't append it again
        StringBuilder builder = new StringBuilder(0).append(PrefsFragment.getCurrentGerrit(sContext));
        if (isAuthenticated(builder.toString())) {
            mAuthenticating = true;
        } else if (mAuthenticating) {
            builder.append("a/");
        }
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

    // --- Helpers ---

    // Helper method to determine if a URL (as a String) is authenticated
    public static boolean isAuthenticated(String url) {
        try {
            return new URI(url).getPath().startsWith("/a");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
    }
}
