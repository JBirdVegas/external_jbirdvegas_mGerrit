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

import android.content.Intent;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.VolleyError;

import org.apache.http.Header;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Retry and reply to authentication challenge (using Apache HTTP methods) if the authentication is
 *  not successful.
 * We will always try an unauthenticated request first so we can receive the challenge.
 * See: https://gist.githubusercontent.com/yamanetoshi/402a9ea071b71afb6639/raw/gistfile1.txt
 */
class DigestRetryPolicy extends DefaultRetryPolicy {

    public static final int HTTP_UNAUTHORIZED = 401;
    private final Request mRequest;
    private int MAX_RETRY_COUNT = 1;
    private Map<String, String> mChallengeHeaders;

    public static String DIGEST_AUTH_HEADER_NAME = "WWW-Authenticate";
    private Intent mResolutionIntent;

    public DigestRetryPolicy(int request_timeout, Request request) {
        super(request_timeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        MAX_RETRY_COUNT = 1;
        mRequest = request;
    }

    @Override
    public void retry(VolleyError error) throws VolleyError {
        if (error.networkResponse.statusCode == HTTP_UNAUTHORIZED) {
            if (MAX_RETRY_COUNT <= 0) {
                throw error;
            }
            String authHeader = error.networkResponse.headers.get(DIGEST_AUTH_HEADER_NAME);
            if (authHeader != null) {
                mChallengeHeaders = new HashMap<>();
                mChallengeHeaders.put(DIGEST_AUTH_HEADER_NAME, authHeader);
            }
            MAX_RETRY_COUNT--;
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
                throwException(e);
            }

            try {
                Header header = ds.authenticate(new UsernamePasswordCredentials(username, password),
                        new BasicHttpRequest(getRequestMethodString(), url));
                headers.put(header.getName(), header.getValue());
            } catch (AuthenticationException e) {
                throwException(e);
            }
        }
        // We will need to retry if we have not received DIGEST_AUTH_HEADER_NAME yet
        return headers;
    }

    public void setResolutionIntent(Intent intent) {
        mResolutionIntent = intent;
    }

    // Takes an exception and throws it. Guaranteed to throw an exception
    private void throwException(Exception e) throws AuthFailureError {
        if (mResolutionIntent != null) {
            throw new AuthFailureError(mResolutionIntent);
        } else {
            throw new AuthFailureError(e.getMessage(), (Exception) e.fillInStackTrace());
        }
    }

    private String getRequestMethodString() {
        switch (mRequest.getMethod()) {
            case Method.GET:
                return "GET";
            case Method.POST:
                return "POST";
            case Method.PUT:
                return "PUT";
            case Method.PATCH:
                return "PATCH";
            case Method.DELETE:
                return "DELETE";
            case Method.HEAD:
                return "HEAD";
            case Method.OPTIONS:
                return "OPTIONS";
            case Method.TRACE:
                return "OPTIONS";
            default:
                return "GET";
        }
    }
}
