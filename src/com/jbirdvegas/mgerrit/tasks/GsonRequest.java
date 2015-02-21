package com.jbirdvegas.mgerrit.tasks;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2013
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

import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Volley adapter for JSON requests that will be parsed into Java objects by Gson.
 * This class was modified slightly from its original source to include trimming support
 * Original source: https://gist.github.com/ficusk/5474673
 */
public class GsonRequest<T> extends Request<T> {
    private final Gson gson;
    private final Class<T> clazz;
    private final Listener<T> listener;
    private final int trim;

    private String _http_username, _http_password;

    /* Set a request timeout of one minute - if we don't hear a response from the server by then it
     *  is too slow */
    public static final int REQUEST_TIMEOUT = 60*1000;

    /**
     * Make a GET request and return a parsed object from JSON.
     *
     * @param url URL of the request to make
     * @param gson A GSON object to use for deserialization
     * @param clazz Relevant class object, for Gson's reflection
     * @param trimStart Number of characters to remove of the head of the response
     *                  before parsing
     */
    public GsonRequest(String url, Gson gson, Class<T> clazz, int trimStart,
            Listener<T> listener, ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.gson = gson;
        this.clazz = clazz;
        this.listener = listener;
        this.trim = trimStart;
        this.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    public void setHttpBasicAuth(String username, String password) {
        _http_username = username;
        _http_password = password;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        // super.getHeaders() returns an empty AbstractMap<K, V> which
        // throw UnsupportedOperation during calls to put(K, V)
        HashMap<String, String> map = new HashMap<>(0);

        Map<String, String> headers = super.getHeaders();
        if (headers != null)
            map.putAll(headers);
        // Always request non-pretty printed JSON responses.
        map.put("Accept", "application/json");

        if (_http_password != null && _http_username != null) {
            String creds = String.format("%s:%s",_http_username, _http_password);
            String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
            headers.put("Authorization", auth);
        }

        return map;
    }

    @Override
    protected void deliverResponse(T response) {
        listener.onResponse(response);
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(
                    response.data, HttpHeaderParser.parseCharset(response.headers));
            if (trim > 0) json = json.substring(trim);
            return Response.success(
                    gson.fromJson(json, clazz), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }
}
