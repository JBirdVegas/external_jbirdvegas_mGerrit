package com.aokp.gerrit.tasks;

import android.os.AsyncTask;
import android.util.Log;
import com.aokp.gerrit.CardsActivity;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/1/13
 * Time: 9:29 AM
 */
public abstract class GerritTask extends AsyncTask<String, Void, String> {
    private static final String TAG = GerritTask.class.getSimpleName();

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "preExecute::GerritTask");
    }

    @Override
    protected String doInBackground(String... strings) {
        BufferedReader in = null;
        StringBuffer sb = new StringBuffer();
        BufferedReader inPost = null;
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpGet httpost = new HttpGet(CardsActivity.GERRIT_WEBADDRESS + strings[0]);
            httpost.setHeader("Accept-Type", "application/json");
            HttpResponse response = httpclient.execute(httpost);
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
            inPost.close();
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
            return sb.toString();
        }
    }

    @Override
    protected abstract void onPostExecute(String s);
}
