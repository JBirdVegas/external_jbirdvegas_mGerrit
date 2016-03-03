package com.jbirdvegas.mgerrit.cache;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;

import com.android.volley.toolbox.ImageLoader;

import org.jetbrains.annotations.Contract;

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
        this.map = new LinkedHashMap<>(0, 0.75f, true);
    }

    @Contract("null -> fail")
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

    @Contract("null, null -> fail")
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
        int memoryClass = am.getMemoryClass();
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

    private static int getBitmapBytes(Bitmap bitmap) {
        int result = bitmap.getByteCount();
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + bitmap);
        }
        return result;
    }
}