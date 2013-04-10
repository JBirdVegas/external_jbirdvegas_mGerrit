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

import android.os.AsyncTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

@SuppressWarnings("AccessOfSystemProperties")
public abstract class GerritTask extends AsyncTask<String, Void, String> {
    private static final String TAG = GerritTask.class.getSimpleName();

    @Override
    protected String doInBackground(String... strings) {
        BufferedReader in = null;
        StringBuffer sb = new StringBuffer(0);
        BufferedReader inPost = null;
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(strings[0]);
            // only needed for authorized actions
            httpGet.setHeader("Accept-Type", "application/json");
            HttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            inPost = new BufferedReader(new InputStreamReader(entity.getContent()));
            String linePost = "";
            String NLPOST = System.getProperty("line.separator");
            boolean isFirst = true;
            while ((linePost = inPost.readLine()) != null) {
                if (!isFirst)
                    sb.append(linePost + NLPOST);
                isFirst = false;
            }
            if (entity != null) {
                entity.consumeContent();
            }
            httpclient.getConnectionManager().shutdown();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inPost != null) {
                try {
                    inPost.close();
                } catch (IOException e) {
                    // let it go
                }
            }
            return sb.toString();
        }
    }

    @Override
    protected abstract void onPostExecute(String s);
}
