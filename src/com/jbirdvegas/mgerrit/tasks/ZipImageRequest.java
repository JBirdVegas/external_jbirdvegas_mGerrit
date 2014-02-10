package com.jbirdvegas.mgerrit.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.jbirdvegas.mgerrit.helpers.Tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.zip.ZipInputStream;

/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
 *  Author: Jon Stanford (JBirdVegas), 2014
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
public class ZipImageRequest extends Request<Bitmap> {
    /** Decoding lock so that we don't decode more than one archive at a time (to avoid OOM's) */
    private static final Object sDecodeLock = new Object();

    private Response.Listener<Bitmap> mListener;

    // Time until cache will be hit, but also refreshed on background
    private static final long CACHE_REFRESH_TIME = 5 * 60 * 1000;
    // Time until cache entry expires completely
    private static final long CACHE_EXPIRES_TIME = 24 * 60 * 60 * 1000;

    /**
     * Creates a new request to download a file compressed into a Zip archive.
     *  This downloads and decodes a change diff that was compressed into an archive.
     *  A single file is expected inside the archive. Assumes the zip file is an
     *  image.
     *
     * @param url URL of the file to download
     * @param listener Listener to receive the decoded diff string
     * @param errorListener Error listener, or null to ignore errors
     */
    public ZipImageRequest(String url,
                           Response.Listener<Bitmap> listener,
                           Response.ErrorListener errorListener)
            throws UnsupportedEncodingException {
        super(Method.GET, url, errorListener);
        mListener = listener;
    }

    @Override
    protected Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
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

    private Response<Bitmap> doParse(NetworkResponse response) {
        byte[] response_data = response.data;
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(response_data));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            int bytesRead;
            byte[] buffer = new byte[8192];
            if (zis.getNextEntry() != null) {
                while ((bytesRead = zis.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
            return getBitmap(output.toByteArray(), response);
        } catch (IOException e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(Bitmap bitmap) {
        mListener.onResponse(bitmap);
    }

    private Response<Bitmap> getBitmap(byte[] data, NetworkResponse response) {
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        if (bitmap == null) {
            return Response.error(new ParseError(response));
        } else {
            return Response.success(bitmap, Tools.parseIgnoreCacheHeaders(response,
                    CACHE_REFRESH_TIME, CACHE_EXPIRES_TIME));
        }
    }
}