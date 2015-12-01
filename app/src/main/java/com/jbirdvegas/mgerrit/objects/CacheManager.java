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
import android.util.Log;

import com.anupcowkur.reservoir.Reservoir;
import com.anupcowkur.reservoir.ReservoirGetCallback;
import com.anupcowkur.reservoir.ReservoirPutCallback;
import com.jbirdvegas.mgerrit.message.CacheDataRetreived;
import com.jbirdvegas.mgerrit.message.CacheFailure;

import de.greenrobot.event.EventBus;

public class CacheManager<T> {

    public static final String TAG = "CacheManager";
    public static final int CACHE_SIZE = 2048; // in bytes

    private final EventBus mEventBus;

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
        if (async) {
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
     * Retreive an object into the cache with the given key.
     * Call with new CacheManager<MyClass>(key, MyClass.class, async)
     * @param key the key string
     * @param clazz the class of the object to be retrieved
     * @param async Whether this should be done asynchronously/non-blocking (true) or synchronously/blocking (false)
     */
    @SuppressWarnings("ProhibitedExceptionCaught")
    public T get(final String key, final Class<T> clazz, boolean async) {
        if (async) {
            Reservoir.getAsync(key, clazz, new ReservoirGetCallback<T>() {
                @Override
                public void onSuccess(T data) {
                    mEventBus.post(new CacheDataRetreived<T>(clazz, key, data));
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
}
