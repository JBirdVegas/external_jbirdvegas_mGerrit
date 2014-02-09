package com.jbirdvegas.mgerrit.tasks;

import android.content.Context;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.jbirdvegas.mgerrit.Prefs;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.zip.ZipInputStream;

/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2014
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
public class ZipRequest extends Request<String> {

    // Time until cache will be hit, but also refreshed on background
    private static final long CACHE_REFRESH_TIME = 5 * 60 * 1000;
    // Time until cache entry expires completely
    private static final long CACHE_EXPIRES_TIME = 24 * 60 * 60 * 1000;

    /** Decoding lock so that we don't decode more than one archive at a time (to avoid OOM's) */
    private static final Object sDecodeLock = new Object();

    private Response.Listener<String> mListener;

    /**
     * Creates a new request to download a file compressed into a Zip archive.
     *  This downloads and decodes a change diff that was compressed into an archive.
     *  A single file is expected inside the archive. Assumes the zip file uses
     *  UTF-8 encoding.
     *
     * @param url URL of the file to download
     * @param listener Listener to receive the decoded diff string
     * @param errorListener Error listener, or null to ignore errors
     */
    public ZipRequest(Context context, Integer changeNumber, Integer patchSetNumber,
                      Response.Listener<String> listener,
                      Response.ErrorListener errorListener) {
        super(Method.GET, getUrl(context, changeNumber, patchSetNumber), errorListener);
        mListener = listener;
    }

    private static String getUrl(Context context, Integer changeNumber, Integer patchSetNumber) {
        String ps;
        if (patchSetNumber == null || patchSetNumber < 1) ps = "current";
        else ps = String.valueOf(patchSetNumber);
        return String.format("%schanges/%d/revisions/%s/patch?zip",
                Prefs.getCurrentGerrit(context),
                changeNumber, ps);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        // Serialize all decode on a global lock to reduce concurrent heap usage.
        synchronized (sDecodeLock) {
            try {
                return doParse(response);
            } catch (OutOfMemoryError e) {
                VolleyLog.e("Caught OOM for %d byte file, url=%s", response.data.length, getUrl());
                return Response.error(new ParseError(e));
            }
        }
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }

    private Response<String> doParse(NetworkResponse response) {
        byte[] response_data = response.data;
        StringBuilder builder = new StringBuilder();

        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(response_data));
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(zis, "UTF-8"));
            if (zis.getNextEntry() != null) {
                String temp;
                while ((temp = in.readLine()) != null) {
                    builder.append(temp).append(System.getProperty("line.separator"));
                }
            }
            in.close();
            return Response.success(builder.toString(),
                    parseIgnoreCacheHeaders(response));
        } catch (IOException e) {
            return Response.error(new ParseError(e));
        }
    }

    /**
     * Extracts a {@link Cache.Entry} from a {@link NetworkResponse}.
     * Cache-control headers are ignored.
     * @param response The network response to parse headers from
     * @return a cache entry for the given response, or null if the response is not cacheable.
     * @see http://stackoverflow.com/questions/16781244/android-volley-jsonobjectrequest-caching
     */
    public static Cache.Entry parseIgnoreCacheHeaders(NetworkResponse response) {
        long now = System.currentTimeMillis();

        Map<String, String> headers = response.headers;
        long serverDate = 0;
        String headerValue;

        headerValue = headers.get("Date");
        if (headerValue != null) {
            serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
        }

        final long softExpire = now + CACHE_REFRESH_TIME;
        final long ttl = now + CACHE_EXPIRES_TIME;

        Cache.Entry entry = new Cache.Entry();
        entry.data = response.data;
        entry.etag = null; // Not worried about etag
        entry.softTtl = softExpire;
        entry.ttl = ttl;
        entry.serverDate = serverDate;
        entry.responseHeaders = headers;
        return entry;
    }
}
