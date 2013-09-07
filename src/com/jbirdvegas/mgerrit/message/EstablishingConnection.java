package com.jbirdvegas.mgerrit.message;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.GerritMessage;

import java.util.Iterator;
import java.util.Map;

/**
* Created by Evan on 30/08/13.
*/
public class EstablishingConnection extends GerritMessage {

    /* Note: Must have the type declared static and public so receivers can subscribe
     * to this type of message */
    public static final String TYPE = "Establishing Connection";

    public EstablishingConnection(Context context) {
        super(context, null);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getMessage() {
        return getContext().getString(R.string.connection_established);
    }

    @Override
    protected Intent packMessage(Map<String, String> map) {
        Intent intent = new Intent(getType());

        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        return intent;
    }
}
