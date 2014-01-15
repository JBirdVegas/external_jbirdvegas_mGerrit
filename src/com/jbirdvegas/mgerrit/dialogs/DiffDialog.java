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
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.FileInfo;
import com.jbirdvegas.mgerrit.objects.Diff;
import com.jbirdvegas.mgerrit.views.DiffTextView;

import org.jetbrains.annotations.Contract;

import java.util.regex.Pattern;

public class DiffDialog extends AlertDialog.Builder {
    private static final String TAG = DiffDialog.class.getSimpleName();
    private static final String DIFF = "\n\nDIFF\n\n";
    private static final boolean DIFF_DEBUG = false;
    private final FileInfo mFileInfo;
    private String mLineSplit = System.getProperty("line.separator");
    private DiffTextView mDiffTextView;
    private DiffFailCallback mDiffFailCallback;

    public interface DiffFailCallback {
        public void killDialogAndErrorOut(Exception e);
    }

    public DiffDialog(Context context, String website, FileInfo fileInfo) {
        super(context);

        context.setTheme(Prefs.getCurrentThemeID(context));

        mFileInfo = fileInfo;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.diff_dialog, null);
        setView(rootView);
        mDiffTextView = (DiffTextView) rootView.findViewById(R.id.diff_view_diff);

        if (DIFF_DEBUG) {
            Log.d(TAG, "Calling url: " + website);
            debugRestDiffApi(context, website, mFileInfo);
        }

        /* we can use volley here because we return does not contain the magic number on the
         * first line. return is just the Base64 formatted return */
        RequestQueue mRequestQueue = Volley.newRequestQueue(context);
        mRequestQueue.add(getBase64StringRequest(website));
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
                        if (base64 == null) {
                            Log.e(TAG, getContext().getString(R.string.return_was_null));
                            return;
                        }
                        String decoded = workAroundBadBase(base64);
                        if (decoded == null) {
                            Log.e(TAG, getContext().getString(R.string.failed_to_decode_base64));
                            return;
                        }

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

    @Contract("null -> null")
    private String workAroundBadBase(String baseString) {
        if (baseString == null) {
            return null;
        }
        String failMessage = "Failed to decode Base64 using: ";
        try {
            return new String(Base64.decode(baseString, Base64.NO_PADDING));
        } catch (IllegalArgumentException badBase) {
            Log.e(TAG, failMessage + "android.util.Base64", badBase);
        }
        return null;
    }

    private void setTextView(String result) {
        Pattern pattern = Pattern.compile("\\Qdiff --git \\E");
        String[] filesChanged = pattern.split(result);
        StringBuilder builder = new StringBuilder(0);
        Diff currentDiff = null;
        for (String change : filesChanged) {
            String concat;
            int index = change.lastIndexOf(mFileInfo.getPath());
            if (index < 0) continue;

            concat = change.substring(2, index).trim().split(" ", 2)[0];
            if (concat.equals(mFileInfo.getPath())) {
                builder.append(DIFF);
                change.replaceAll("\n", mLineSplit);

                currentDiff = new Diff(getContext(), change, mDiffTextView);
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

    private void debugRestDiffApi(Context context, String mUrl, FileInfo mFileInfo) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        Log.d(TAG, "Targeting changed file: " + mFileInfo);
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
                                        + new String(Base64.decode(s, Base64.NO_PADDING | Base64.URL_SAFE))
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