package com.jbirdvegas.mgerrit.tasks;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
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

    public static final int BUFFER = 2048;

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
    public ZipRequest(String url, Response.Listener<String> listener,
                      Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        mListener = listener;
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

        try {
            if (zis.getNextEntry() != null) {
                byte data[] = new byte[BUFFER];
                while (zis.read(data, 0, BUFFER) != -1) {
                    // Use UTF-8 decoding
                    String temp = new String(data, "UTF-8");
                    builder.append(temp);
                }
                zis.close();
                return Response.success(builder.toString(),
                        HttpHeaderParser.parseCacheHeaders(response));
            }
        } catch (IOException e) {
            return Response.error(new ParseError(e));
        }
        return Response.error(new ParseError());
    }
}
