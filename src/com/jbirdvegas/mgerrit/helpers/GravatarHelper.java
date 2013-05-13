package com.jbirdvegas.mgerrit.helpers;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 5/13/13 1:38 AM
 */
public class GravatarHelper {
    private static final String TAG = GravatarHelper.class.getSimpleName();
    public static final String GRAVATAR_API = "http://www.gravatar.com/avatar/";
    public static final String DEFAULT_AVATAR_SIZE = "80";

    public static void populateProfilePicture(final ImageView imageView, String email) {
        String emailMd5 = MD5Helper.md5Hex(email.trim().toLowerCase());
        if (emailMd5 != null) {
            String url = String.format("%s%s?s=%s",
                    GRAVATAR_API,
                    emailMd5,
                    DEFAULT_AVATAR_SIZE);
            Log.d(TAG, "Gravatar url called: " + url);
            imageVolleyRequest(imageView, url).start();
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    private static RequestQueue imageVolleyRequest(final ImageView imageView, String url) {
        RequestQueue imageRequest = Volley.newRequestQueue(imageView.getContext());
        imageRequest.add(new ImageRequest(url, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
            }
        },
                80,
                80,
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
}
