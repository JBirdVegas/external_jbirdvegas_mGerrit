package com.jbirdvegas.mgerrit.dialogs;

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

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.Base64Coder;
import com.jbirdvegas.mgerrit.objects.ChangedFile;
import com.jbirdvegas.mgerrit.objects.Diff;
import org.apache.commons.codec.binary.ApacheBase64;

import java.util.regex.Pattern;

public class DiffDialog extends AlertDialog.Builder {
    private static final String TAG = DiffDialog.class.getSimpleName();
    private static final String DIFF = "\n\nDIFF\n\n";
    private static final boolean DIFF_DEBUG = false;
    private final String mUrl;
    private final RequestQueue mRequestQueue;
    private View mRootView;
    private final ChangedFile mChangedFile;
    private String mLineSplit = System.getProperty("line.separator");
    private LayoutInflater mInflater;
    private TextView mDiffTextView;
    private DiffFailCallback mDiffFailCallback;

    public interface DiffFailCallback {
        public void killDialogAndErrorOut(Exception e);
    }

    public DiffDialog(Context context, String website, ChangedFile changedFile) {
        super(context);
        mRequestQueue = Volley.newRequestQueue(context);
        mUrl = website;
        mChangedFile = changedFile;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRootView = mInflater.inflate(R.layout.diff_dialog, null);
        setView(mRootView);
        mDiffTextView = (TextView) mRootView.findViewById(R.id.diff_view_diff);
        mDiffTextView.setText(R.string.loading);
        mDiffTextView.setTextSize(18f);
        Log.d(TAG, "Calling url: " + mUrl);
        if (DIFF_DEBUG) {
            debugRestDiffApi(context, mUrl, mChangedFile);
        }
        // we can use volley here because we return
        // does not contain the magic number on the
        // first line. return is just the Base64 formatted
        // return
        mRequestQueue.add(getBase64StringRequest(mUrl));
        mRequestQueue.start();
    }

    public DiffDialog addExceptionCallback(DiffFailCallback failCallback) {
        mDiffFailCallback = failCallback;
        return this;
    }

    private StringRequest getBase64StringRequest(final String weburl) {
        return new StringRequest(weburl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String base64) {
                        String decoded = workAroundBadBase(base64);
                        if (DIFF_DEBUG) {
                            Log.d(TAG, "[DEBUG-MODE]\n"
                                    + "url: " + weburl
                                    + "\n==================================="
                                    + decoded
                                    + "====================================");
                        }
                        setTextView(decoded);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.e(TAG, "Failed to download the diff", volleyError);
                        if (mDiffFailCallback != null) {
                            mDiffFailCallback.killDialogAndErrorOut(volleyError);
                        }
                    }
                }
        );
    }

    private String workAroundBadBase(String baseString) {
        if (baseString == null) {
            return getContext().getString(R.string.return_was_null);
        }
        String failMessage = "Failed to decode Base64 using: ";
        try {
            return new String(ApacheBase64.decodeBase64(baseString));
        } catch (IllegalArgumentException badBase) {
            Log.e(TAG, failMessage + "org.apache.commons.codec.binary.ApacheBase64", badBase);
        }
        try {
            return new String(Base64.decode(baseString.getBytes(), Base64.URL_SAFE | Base64.NO_PADDING));
        } catch (IllegalArgumentException badBase) {
            Log.e(TAG, failMessage + "android.util.Base64", badBase);
        }
        try {
            return new String(Base64Coder.decode(baseString));
        } catch (IllegalArgumentException badBase) {
            Log.e(TAG, failMessage + "com.jbirdvegas.mgerrit.helpers.Base64Coder", badBase);
        }
        return getContext().getString(R.string.failed_to_decode_base64);
    }

    private void setTextView(String result) {
        Pattern pattern = Pattern.compile("\\Qdiff --git \\E");
        String[] filesChanged = pattern.split(result);
        StringBuilder builder = new StringBuilder(0);
        Diff currentDiff = null;
        for (String change : filesChanged) {
            String concat;
            try {
                concat = change.substring(2, change.lastIndexOf(mChangedFile.getPath())).trim();
                concat = concat.split(" ")[0];
            } catch (StringIndexOutOfBoundsException notFound) {
                Log.d(TAG, notFound.getMessage());
                continue;
            }
            if (concat.equals(mChangedFile.getPath())) {
                builder.append(DIFF);
                change.replaceAll("\n", mLineSplit);
                currentDiff = new Diff(getContext(), change);
                builder.append(change);
            }
        }
        if (builder.length() == 0) {
            builder.append("Diff not found!");
        }
        // reset text size to default
        mDiffTextView.setTextAppearance(getContext(), android.R.style.TextAppearance_DeviceDefault_Small);
        mDiffTextView.setTypeface(Typeface.MONOSPACE);
        // rebuild text; required to respect the \n
        SpannableString spannableString = currentDiff.getColorizedSpan();
        if (spannableString != null) {
            mDiffTextView.setText(currentDiff.getColorizedSpan(), TextView.BufferType.SPANNABLE);
        } else {
            mDiffTextView.setText("Failed to load diff :(");
        }

    }

    private void debugRestDiffApi(Context context, String mUrl, ChangedFile mChangedFile) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        Log.d(TAG, "Targeting changed file: " + mChangedFile);
        requestQueue.add(getDebugRequest(mUrl, "/a"));
        requestQueue.add(getDebugRequest(mUrl, "/b"));
        requestQueue.add(getDebugRequest(mUrl, "/ab"));
        requestQueue.add(getDebugRequest(mUrl, "/"));
        requestQueue.start();
    }

    private Request getDebugRequest(String url, String arg) {
        // seems a bug prevents the args from being respected???
        // See here:
        // https://groups.google.com/forum/?fromgroups#!topic/repo-discuss/xmFCHbD4Z0Q
        String limiter = "&o=context:2";

        final String args = arg + limiter;
        final String weburl = url + args;
        Request debugRequest =
                new StringRequest(weburl,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String s) {
                                Log.d(TAG, "[DEBUG-MODE]\n" +
                                        "Decoded Response for args {" + args + '}'
                                        + "\n"
                                        + "url: " + weburl
                                        + "\n==================================="
                                        + new String(Base64.decode(s, Base64.URL_SAFE | Base64.NO_PADDING))
                                        + "===================================="
                                );
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                Log.e(TAG, "Debuging Volley Failed!!!", volleyError);
                            }
                        }
                );
        return debugRequest;
    }
}