package com.jbirdvegas.mgerrit.caches;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Build;
import com.android.volley.toolbox.ImageLoader;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class from example: http://weakapp0320.blogspot.com/2013/05/imageloader-with-volley-example.html
 */
public class BitmapLruCache implements ImageLoader.ImageCache {
    final LinkedHashMap<String, Bitmap> map;
    private final int maxSize;

    private int size;

    /**
     * Create a cache using an appropriate portion of the available RAM as the maximum size.
     */
    public BitmapLruCache(Context context) {
        this(calculateMaxSize(context));
    }

    /**
     * Create a cache with a given maximum size in bytes.
     */
    public BitmapLruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be positive.");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<String, Bitmap>(0, 0.75f, true);
    }

    private Bitmap get(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        Bitmap mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                return mapValue;
            }
        }

        return null;
    }

    private void set(String key, Bitmap bitmap) {
        if (key == null || bitmap == null) {
            throw new NullPointerException("key == null || bitmap == null");
        }

        Bitmap previous;
        synchronized (this) {
            size += getBitmapBytes(bitmap);
            previous = map.put(key, bitmap);
            if (previous != null) {
                size -= getBitmapBytes(previous);
            }
        }

        trimToSize(maxSize);
    }

    private void trimToSize(int maxSize) {
        while (true) {
            String key;
            Bitmap value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(
                            getClass().getName() + ".sizeOf() is reporting inconsistent results!");
                }

                if (size <= maxSize || map.isEmpty()) {
                    break;
                }

                Map.Entry<String, Bitmap> toEvict = map.entrySet().iterator().next();
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= getBitmapBytes(value);
            }
        }
    }

    /**
     * Clear the cache.
     */
    public final void clear() {
        trimToSize(-1); // -1 will evict 0-sized elements
    }

    private static int calculateMaxSize(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB) {
            memoryClass = ActivityManagerHoneycomb.getLargeMemoryClass(am);
        }
        return 1024 * 1024 * memoryClass / 6;
    }

    @Override
    public Bitmap getBitmap(String url) {
        return get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        set(url, bitmap);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static class ActivityManagerHoneycomb {
        static int getLargeMemoryClass(ActivityManager activityManager) {
            return activityManager.getLargeMemoryClass();
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private static class BitmapHoneycombMR1 {
        static int getByteCount(Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    }

    private static int getBitmapBytes(Bitmap bitmap) {
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            result = BitmapHoneycombMR1.getByteCount(bitmap);
        } else {
            result = bitmap.getRowBytes() * bitmap.getHeight();
        }
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + bitmap);
        }
        return result;
    }
}