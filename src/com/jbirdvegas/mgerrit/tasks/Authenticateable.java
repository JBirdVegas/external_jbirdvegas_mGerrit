package com.jbirdvegas.mgerrit.tasks;

/*
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2015
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

import android.support.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for request authentication logic.
 *
 * TODO: Come up with better name
 */
public abstract class Authenticateable<T> extends Request<T> {

    /* Set a request timeout of one minute - if we don't hear a response from the server by then it
 *  is too slow */
    public static final int REQUEST_TIMEOUT = 60*1000;

    private DigestRetryPolicy retryPolicy;
    private String _http_username, _http_password;

    public Authenticateable(int method, String url, Response.ErrorListener listener) {
        super(method, url, listener);
        this.retryPolicy = new DigestRetryPolicy(REQUEST_TIMEOUT);
        this.setRetryPolicy(retryPolicy);
    }

    public void setHttpBasicAuth(String username, String password) {
        _http_username = username;
        _http_password = password;
    }

    @Override
    @NonNull // This method will not return a null value where the super might
    public Map<String, String> getHeaders() throws AuthFailureError {
        // super.getHeaders() returns an empty AbstractMap<K, V> which
        // throw UnsupportedOperation during calls to put(K, V)
        HashMap<String, String> map = new HashMap<>(0);

        Map<String, String> headers = super.getHeaders();
        if (headers != null) map.putAll(headers);

        if (_http_password != null && _http_username != null) {
            map.putAll(retryPolicy.setHeaders(map, _http_username, _http_password, getUrl()));
        }

        return map;
    }
}
