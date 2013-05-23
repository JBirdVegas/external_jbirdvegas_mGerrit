package com.jbirdvegas.mgerrit.helpers;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5Helper was adapted from Gravatar's documentation
 * <p/>
 * Please see https://en.gravatar.com/site/implement/images/java/
 * for original code
 */
@SuppressWarnings("UtilityClass")
public class MD5Helper {
    private static final String TAG = MD5Helper.class.getSimpleName();

    private MD5Helper() {
        // you have no business here
    }

    public static String hex(byte... array) {
        StringBuilder sb = new StringBuilder(0);
        for (byte anArray : array) {
            sb.append(Integer.toHexString(anArray
                    & 0xFF | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    @SuppressWarnings({"ReturnOfNull", "StaticMethodOnlyUsedInOneClass"})
    public static String md5Hex(String message) {
        try {
            MessageDigest md =
                    MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Failed to find MD5 Algorithm!", e);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnSupported encoding!", e);
        }
        return null;
    }
}