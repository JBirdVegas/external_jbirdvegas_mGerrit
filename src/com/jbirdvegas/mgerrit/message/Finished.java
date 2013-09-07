package com.jbirdvegas.mgerrit.message;

import android.content.Context;

import com.jbirdvegas.mgerrit.objects.GerritMessage;

/**
* Created by Evan on 30/08/13.
*/
public class Finished extends GerritMessage {

    /* Note: Must have the type declared static and public so receivers can subscribe
     * to this type of message */
    public static final String TYPE = "Finished";
    String mMessage;

    public Finished(Context context, String message, String url) {
        super(context, url);
        this.mMessage = message;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getMessage() {
        return mMessage;
    }
}
