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

import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.VolleyError;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.Header;


import java.util.HashMap;
import java.util.Map;

class DigestRetryPolicy extends DefaultRetryPolicy {

    public static final int HTTP_UNAUTHORIZED = 401;
    private int retryCount;
    private Map<String, String> mChallengeHeaders;

    public static String DIGEST_AUTH_HEADER_NAME = "WWW-Authenticate";
    public static String REQUEST_METHOD = "GET";

    public DigestRetryPolicy(int request_timeout) {
        super(request_timeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        retryCount = 1;
    }

    @Override
    public void retry(VolleyError error) throws VolleyError {
        if (error.networkResponse.statusCode == HTTP_UNAUTHORIZED) {
            if (retryCount <= 0) {
                throw error;
            }
            String authHeader = error.networkResponse.headers.get(DIGEST_AUTH_HEADER_NAME);
            if (authHeader != null) {
                mChallengeHeaders = new HashMap<>();
                mChallengeHeaders.put(DIGEST_AUTH_HEADER_NAME, authHeader);
            }
            retryCount--;
        } else {
            throw error;
        }
    }

    public Map<String, String> setHeaders(Map<String, String> headers, String username, String password, String url) throws AuthFailureError {
        if (mChallengeHeaders != null && mChallengeHeaders.containsKey(DIGEST_AUTH_HEADER_NAME)) {
            DigestScheme ds = new DigestScheme();
            try {
                ds.processChallenge(new BasicHeader(DIGEST_AUTH_HEADER_NAME,
                        mChallengeHeaders.get(DIGEST_AUTH_HEADER_NAME)));
            } catch (MalformedChallengeException e) {
                e.printStackTrace();
                throw new AuthFailureError(e.getMessage(), (Exception) e.fillInStackTrace());
            }

            Header header;
            try {
                header = ds.authenticate(new UsernamePasswordCredentials(username, password),
                        new BasicHttpRequest(REQUEST_METHOD, url));
            } catch (AuthenticationException e) {
                e.printStackTrace();
                throw new AuthFailureError();
            }
            headers.put(header.getName(), header.getValue());
        }
        // We will need to retry if we have not received DIGEST_AUTH_HEADER_NAME yet
        return headers;
    }
}
