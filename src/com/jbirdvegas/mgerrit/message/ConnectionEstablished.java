package com.jbirdvegas.mgerrit.message;

import android.content.Context;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.GerritMessage;

/**
* Created by Evan on 30/08/13.
*/
public class ConnectionEstablished extends GerritMessage {

    /* Note: Must have the type declared static and public so receivers can subscribe
     * to this type of message */
    public static final String TYPE = "Connection Established";

    public ConnectionEstablished(Context context, String url) {
        super(context, url);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getMessage() {
        return getContext().getString(R.string.connection_established);
    }
}
