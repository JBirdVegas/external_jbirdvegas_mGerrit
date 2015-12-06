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

package com.jbirdvegas.mgerrit.message;

public class CacheDataRetrieved<T> {

    final Class<T> mTypeClass;
    private final String mKey;
    private final T mData;

    public CacheDataRetrieved(Class<T> typeClass, String key, T data) {
        this.mTypeClass = typeClass;
        this.mKey = key;
        this.mData = data;
    }

    public String getKey() {
        return mKey;
    }

    public T getData() {
        return mData;
    }
}
