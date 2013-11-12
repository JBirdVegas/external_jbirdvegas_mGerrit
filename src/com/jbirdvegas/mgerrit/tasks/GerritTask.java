package com.jbirdvegas.mgerrit.tasks;

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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.jbirdvegas.mgerrit.message.ConnectionEstablished;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.EstablishingConnection;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.HandshakeError;
import com.jbirdvegas.mgerrit.message.InitializingDataTransfer;
import com.jbirdvegas.mgerrit.message.ProgressUpdate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.SSLHandshakeException;

// Probably need to change this from an AsyncTask as it is not always called on the UI thread.
@SuppressWarnings("AccessOfSystemProperties")
public abstract class GerritTask extends AsyncTask<String, String, String> {
    private final boolean CONNECTION_DEBUGGING = false;
    private Exception mGerritException;

    public interface FailedGerritCallback {
        public void deliverCaughtException(Exception failedGerritTaskException);
    }

    public static final String TAG = GerritTask.class.getSimpleName();
    private final boolean DEBUG = true;
    private final long CONNECTION_ESTABLISHED = -1000;
    private final long INITIALIZING_DATA_TRANSFER = -1001;
    private final long ERROR_DURING_CONNECTION = -1002;
    private final long HANDSHAKE_ERROR = -1003;
    private Context mContext;
    private long mCurrentFileLength = -1;
    private String mCurrentUrl;

    public GerritTask(Context context) {
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        new EstablishingConnection(mContext).sendUpdateMessage();
    }

    @Override
    protected String doInBackground(String... strings) {
        mCurrentUrl = strings[0];
        BufferedReader reader = null;
        StringBuilder stringBuilder = new StringBuilder(0);
        try {
            URL url = new URL(strings[0]);
            URLConnection connection = url.openConnection();
            // By default Android will (transparently) attempt to gzip the content
            // passing identity as the encoding will disable compression.
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.connect();
            handleComputationsOffUIThread(CONNECTION_ESTABLISHED);
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            stringBuilder = new StringBuilder(0);
            String line;
            String lineEnding = System.getProperty("line.separator");
            long byteProgressCounter = 0;
            handleComputationsOffUIThread(INITIALIZING_DATA_TRANSFER);
            // Grab the current length to use for calculations
            mCurrentFileLength = connection.getContentLength();
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    byteProgressCounter += line.getBytes().length;
                    if (line.contains(")]}'")) {
                        // remove magic chars
                        stringBuilder.append(line.substring(4));
                    } else {
                        // if no magic we are getting a literal
                        stringBuilder.append(line);
                    }
                    handleComputationsOffUIThread(byteProgressCounter);
                } else {
                    byteProgressCounter += line.getBytes().length;
                    stringBuilder.append(line)
                            .append(lineEnding);
                    handleComputationsOffUIThread(byteProgressCounter);
                }
            }
        } catch (SSLHandshakeException ssl) {
            mGerritException = ssl;
            handleComputationsOffUIThread(HANDSHAKE_ERROR);
        } catch (MalformedURLException e) {
            if (DEBUG) Log.e(TAG, "MalformedURL", e);
            mGerritException = e;
            handleComputationsOffUIThread(ERROR_DURING_CONNECTION);
        } catch (IOException e) {
            if (DEBUG) Log.e(TAG, "Gathering data threw IO", e);
            mGerritException = e;
            handleComputationsOffUIThread(ERROR_DURING_CONNECTION);

        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    // failed to close reader
                }
        }
        return stringBuilder.toString();
    }

    @Override
    protected void onPostExecute(String s) {

        new Finished(mContext, s, mCurrentUrl).sendUpdateMessage();

        // check if we are in production code or debugging mode
        boolean isDebuggable = 0 != (mContext.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE);
        // if we are debugging then dump the response to logcat
        if (isDebuggable || DEBUG) {
            Log.d(TAG, "[DEBUG-MODE] Gerrit instance response: " + s);
        }
        onJSONResult(s);
    }

    public abstract void onJSONResult(String jsonString);

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    private void handleComputationsOffUIThread(Long... values) {

        try {
            int switchable = Integer.parseInt(String.valueOf(values[0]));
            // TODO make text display notices
            switch (switchable) {
                case (int) CONNECTION_ESTABLISHED:
                    new ConnectionEstablished(mContext, mCurrentUrl).sendUpdateMessage();
                    return;
                case (int) INITIALIZING_DATA_TRANSFER:
                    new InitializingDataTransfer(mContext, mCurrentUrl).sendUpdateMessage();
                    return;
                case (int) ERROR_DURING_CONNECTION:
                    new ErrorDuringConnection(mContext, mGerritException, mCurrentUrl).sendUpdateMessage();
                    closeUpShop();
                    return;
                case (int) HANDSHAKE_ERROR:
                    new HandshakeError(mContext, mGerritException, mCurrentUrl).sendUpdateMessage();
                    closeUpShop();
                    return;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse number", e);
        }
        // if we are still here then we just display the progress of download
        // display either generic message or progress % (if available)
        if (mCurrentFileLength != -1) {
            new ProgressUpdate(mContext, mCurrentUrl, values[0], mCurrentFileLength).sendUpdateMessage();
            if (CONNECTION_DEBUGGING) {
                Log.d(TAG, "progress: " + values[0] + " totalSize: " + mCurrentFileLength +
                        " as percent:" + safeLongToInt(values[0] * 100 / mCurrentFileLength));
            }
        }
    }

    // close up the dialogs and end GerritTask
    private void closeUpShop() {
        this.cancel(true);
    }

    /**
     * safely casts long to int
     *
     * @param l long to be transformed
     * @return int value of l
     */
    private int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }
}
