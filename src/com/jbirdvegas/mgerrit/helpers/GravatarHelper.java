package com.jbirdvegas.mgerrit.helpers;

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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.jbirdvegas.mgerrit.R;

public class GravatarHelper {
    private static final String TAG = GravatarHelper.class.getSimpleName();
    public static final String GRAVATAR_API = "http://www.gravatar.com/avatar/";
    public static final String DEFAULT_AVATAR_SIZE = "80";

    public static void populateProfilePicture(final ImageView imageView, String email,
                                              RequestQueue imageRequest) {
        String url = getGravatarUrl(email);

        if (url != null) {
            Log.d(TAG, "Gravatar url called: " + url);
            GravatarHelper.imageVolleyRequest(imageView, url, imageRequest).start();
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    public static String getGravatarUrl(String email) {
        if (email != null) {
            String emailMd5 = MD5Helper.md5Hex(email.trim().toLowerCase());
            if (emailMd5 != null) {
                return String.format("%s%s?s=%s",
                        GRAVATAR_API,
                        emailMd5,
                        DEFAULT_AVATAR_SIZE);
            }
        }
        return null;

        /* This only works when the server has a Gravatar plugin installed.
         * StringBuilder builder = new StringBuilder()
         *       .append(Prefs.getCurrentGerrit(context))
         *       .append(accountID)
         *       .append("/avatar?s=").append(DEFAULT_AVATAR_SIZE);
         * return builder.toString();*/
    }

    private static RequestQueue imageVolleyRequest(final ImageView imageView, String url,
                                                   RequestQueue imageRequest) {
        imageRequest.add(new ImageRequest(url, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
            }
        },
                1028,
                1028,
                Bitmap.Config.ARGB_8888,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.e(TAG, "http Volley request failed!", volleyError);
                    }
                }
        ));
        return imageRequest;
    }

    public static void attachGravatarToTextView(final TextView textView, String email,
                                                RequestQueue imageRequest) {
        String url = getGravatarUrl(email);
        if (url == null) return;

        imageRequest.add(new ImageRequest(url, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap bitmap) {
                textView.setCompoundDrawablesWithIntrinsicBounds(
                        new BitmapDrawable(bitmap),
                        null,
                        null,
                        null);
                Context context = textView.getContext();
                textView.setCompoundDrawablePadding(
                        Math.round(
                                context.getResources().getDimension(
                                        R.dimen.gravatar_image_padding)));
            }
        },
                // set a basic max height/width
                // but the textview handles the actual resizing
                80, 80,
                Bitmap.Config.ARGB_8888,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        // don't do anything just show the textview as is
                        Log.e(TAG, "http Volley request failed!", volleyError);
                    }
                }
        ));
    }
}
