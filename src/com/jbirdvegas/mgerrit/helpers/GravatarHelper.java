package com.jbirdvegas.mgerrit.helpers;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.jbirdvegas.mgerrit.R;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 5/13/13 1:38 AM
 */
public class GravatarHelper {
    private static final String TAG = GravatarHelper.class.getSimpleName();
    public static final String GRAVATAR_API = "http://www.gravatar.com/avatar/";
    public static final String DEFAULT_AVATAR_SIZE = "80";

    public static void populateProfilePicture(ImageView imageView, String email) {
        String emailMd5 = MD5Helper.md5Hex(email.trim().toLowerCase());
        if (emailMd5 != null) {
            String url = String.format("%s%s?s=%s",
                    GRAVATAR_API,
                    emailMd5,
                    DEFAULT_AVATAR_SIZE);
            Log.d(TAG, "Gravatar url called: " + url);
            UrlImageViewHelper.setUrlDrawable(imageView,
                    url,
                    R.drawable.ic_action_clock,
                    UrlImageViewHelper.CACHE_DURATION_THREE_DAYS);
        } else {
            imageView.setVisibility(View.GONE);
        }
    }
}
