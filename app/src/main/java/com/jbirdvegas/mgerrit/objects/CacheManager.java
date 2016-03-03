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

package com.jbirdvegas.mgerrit.objects;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import com.anupcowkur.reservoir.Reservoir;
import com.anupcowkur.reservoir.ReservoirDeleteCallback;
import com.anupcowkur.reservoir.ReservoirGetCallback;
import com.anupcowkur.reservoir.ReservoirPutCallback;
import com.jbirdvegas.mgerrit.BuildConfig;
import com.jbirdvegas.mgerrit.helpers.MD5Helper;
import com.jbirdvegas.mgerrit.message.CacheDataRetrieved;
import com.jbirdvegas.mgerrit.message.CacheFailure;
import com.vincentbrison.openlibraries.android.dualcache.lib.DualCache;
import com.vincentbrison.openlibraries.android.dualcache.lib.DualCacheBuilder;
import com.vincentbrison.openlibraries.android.dualcache.lib.DualCacheContextUtils;
import com.vincentbrison.openlibraries.android.dualcache.lib.DualCacheLogUtils;
import com.vincentbrison.openlibraries.android.dualcache.lib.Serializer;
import com.vincentbrison.openlibraries.android.dualcache.lib.SizeOf;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.greenrobot.eventbus.EventBus;

/*
 * Wrapper caching functions acting as a facade around the caching libraries used so we can
 *  more easily switch out the caching library or libraries being used
 *
 * We are going to use two different caching libraries here.
 * Reservoir: Great caching library which handles multiple objects really well, but requires that
 *  they can all be serialised into Gson (so no images)
 * DualCache: Handles images very well but requires a new cache for each object stored for proper
 *  serialisation
 */
public class CacheManager<T> {

    public static final String TAG = "CacheManager";
    public static final int CACHE_SIZE = 2048; // in bytes
    private static final String BITMAP_CACHE_NAME = "bitmap_cache";
    private static final int CACHE_VERSION = 1;
    public static final int DISK_MAX_SIZE = 12*1024*1024; // 12MB in bytes

    private final EventBus mEventBus;
    private static DualCache<Bitmap> sBitmapCache;

    public CacheManager() {
        this.mEventBus = EventBus.getDefault();
    }

    @SuppressWarnings("ProhibitedExceptionCaught")
    public static void init(Context context) {
        try {
            Reservoir.init(context, CACHE_SIZE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialise the cache.", e);
        }

        DualCacheContextUtils.setContext(context);
        if (BuildConfig.DEBUG) {
            DualCacheLogUtils.enableLog();
        }
        // Initialize a cache to store images
        sBitmapCache = new DualCacheBuilder<>(BITMAP_CACHE_NAME, CACHE_VERSION, Bitmap.class)
                .useReferenceInRam(CACHE_SIZE, new SizeOf<Bitmap>() {
                    @Override
                    public int sizeOf(Bitmap object) {
                        return object.getByteCount();
                    }
                })
                .useCustomSerializerInDisk(DISK_MAX_SIZE, true, new Serializer<Bitmap>() {
                    @Override
                    public Bitmap fromString(String encodedString) {
                        // See: http://stackoverflow.com/questions/13562429/how-many-ways-to-convert-bitmap-to-string-and-vice-versa
                        try {
                            byte [] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
                            return bitmap;
                        } catch (Exception e) {
                            e.getMessage();
                            return null;
                        }
                    }

                    @Override
                    public String toString(Bitmap bitmap) {
                        // See: http://stackoverflow.com/questions/13562429/how-many-ways-to-convert-bitmap-to-string-and-vice-versa
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
                        byte [] b = baos.toByteArray();
                        String temp = Base64.encodeToString(b, Base64.DEFAULT);
                        return temp;
                    }
                });
    }

    /**
     * Put an object into the cache with the given key.
     * Previously stored object with the same key (if any) will be overwritten.
     * @param key the key string
     * @param object the object to be stored
     * @param async Whether this should be done asynchronously/non-blocking (true) or synchronously/blocking (false)
     */
    @SuppressWarnings("ProhibitedExceptionCaught")
    public static void put(final String key, Object object, final boolean async) {
        if (object.getClass() == Bitmap.class) {
            putImage(key, (Bitmap) object, async);
        } else if (async) {
            Reservoir.putAsync(key, object, new ReservoirPutCallback() {
                @Override
                public void onSuccess() {
                    // success
                }

                @Override
                public void onFailure(Exception e) {
                    EventBus.getDefault().post(new CacheFailure(key, e, true));
                }
            });
        } else {
            try {
                Reservoir.put(key, object);
            } catch (Exception e) {
                EventBus.getDefault().post(new CacheFailure(key, e, true));
            }
        }
    }

    /**
     * Put an image into the cache with the given key.
     * Previously stored object with the same key (if any) will be overwritten.
     * @param key the key string
     * @param bitmap the image to be stored
     * @param async Whether this should be done asynchronously/non-blocking (true) or synchronously/blocking (false)
     */
    public static void putImage(final String key, final Bitmap bitmap, final boolean async) {
        if (async) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sBitmapCache.put(key, bitmap);
                }
            }).start();
        } else {
            sBitmapCache.put(key, bitmap);
        }
    }


    /**
     * Retrieve an object from the cache with the given key.
     * Call with new CacheManager<String>().get(mCacheKey, String.class, true)
     * @param key the key string
     * @param clazz the class of the object to be retrieved
     * @param async Whether this should be done asynchronously/non-blocking (true) or synchronously/blocking (false)
     */
    @SuppressWarnings("ProhibitedExceptionCaught")
    public T get(final String key, final Class<T> clazz, boolean async) {
        if (clazz == Bitmap.class) return (T) getImage(key, async);

        if (async) {
            Reservoir.getAsync(key, clazz, new ReservoirGetCallback<T>() {
                @Override
                public void onSuccess(T data) {
                    mEventBus.post(new CacheDataRetrieved<>(clazz, key, data));
                }

                @Override
                public void onFailure(Exception e) {
                    mEventBus.post(new CacheFailure(key, e, false));
                }
            });
        } else {
            try {
                return Reservoir.get(key, clazz);
            } catch (Exception e) {
                mEventBus.post(new CacheFailure(key, e, false));
            }
        }
        return null;
    }

    /**
     * Retrieve an image from the cache with the given key.
     * Call with new CacheManager<String>().getImage(mCacheKey, true)
     * @param key the key string
     * @param async Whether this should be done asynchronously/non-blocking (true) or synchronously/blocking (false)
     */
    public Bitmap getImage(@NonNull final String key, final boolean async) {
        if (async) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap data = sBitmapCache.get(key);
                    if (data != null) {
                        mEventBus.post(new CacheDataRetrieved<>(Bitmap.class, key, data));
                    } else {
                        mEventBus.post(new CacheFailure(key, null, false));
                    }
                }
            }).start();
        } else {
            try {
                return sBitmapCache.get(key);
            } catch (NullPointerException e) {
                mEventBus.post(new CacheFailure(key, e, false));
            }
        }
        return null;
    }

    public static Boolean remove(final String key, boolean async) {
        if (async) {
            Reservoir.deleteAsync(key, new ReservoirDeleteCallback() {
                @Override
                public void onSuccess() {
                    // success
                }

                @Override
                public void onFailure(Exception e) {
                    //error
                }
            });
        } else {
            try {
                Reservoir.delete(key);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return null;
    }

    public static Boolean removeImage(final String key, boolean async) {
        if (async) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sBitmapCache.delete(key);
                }
            }).start();
        } else {
            sBitmapCache.delete(key);
            return true;
        }
        return null;
    }

    public static String getCommentKey(final String changeId) {
        return "comment." + changeId;
    }

    public static String getDiffKey(final int changeNumber, final int psNumber) {
        return "diff~" + changeNumber + "~" + psNumber;
    }

    public static String getImageKey(int changeNumber, int patchsetNumber, String path) {
        // Since the DualCache proposes rather constictive restrictions on the key, we need to
        //  hash the key we would use to filter out the characters it does not allow
        return MD5Helper.md5Hex(path + "@@" + changeNumber + "#" + patchsetNumber);
    }
}
