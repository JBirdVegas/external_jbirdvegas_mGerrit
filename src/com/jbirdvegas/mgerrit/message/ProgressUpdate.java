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
public class ProgressUpdate extends GerritMessage {

    /* Note: Must have the type declared static and public so receivers can subscribe
     * to this type of message */
    public static final String TYPE = "Progress Update";
    private final long mProgress;
    private final long mFileLength;

    public ProgressUpdate(Context context, String url, long progress, long fileLength) {
        super(context, url);
        this.mProgress = progress;
        this.mFileLength = fileLength;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getMessage() {
        return getContext().getString(R.string.downloading_status);
    }

    @Override
    protected Intent packMessage(Map<String, String> map) {
        Intent intent = new Intent(getType());

        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            intent.putExtra(entry.getKey(), entry.getValue());
            intent.putExtra(URL, mUrl);
            intent.putExtra(GerritMessage.PROGRESS, mProgress);
            intent.putExtra(GerritMessage.FILE_LENGTH, mFileLength);
        }
        return intent;
    }
}
