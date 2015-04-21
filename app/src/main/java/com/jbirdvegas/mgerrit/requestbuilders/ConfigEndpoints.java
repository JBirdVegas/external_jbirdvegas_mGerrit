package com.jbirdvegas.mgerrit.requestbuilders;

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

import android.os.Parcel;
import android.os.Parcelable;

public class ConfigEndpoints extends RequestBuilder implements Parcelable {

    String mUrl = "";

    private ConfigEndpoints(String url) {
        mUrl = url;
    }

    public ConfigEndpoints(ConfigEndpoints url) {
        super(url);
        mUrl = url.mUrl;
    }

    public static ConfigEndpoints server_verion() {
        return new ConfigEndpoints("server/version");
    }

    @Override
    public String getPath() {
        StringBuilder builder = new StringBuilder(0).append("config/").append(mUrl);
        return builder.toString();
    }

    // --- Parcelable stuff so we can send this object through intents ---
    public static final Creator<ConfigEndpoints> CREATOR
            = new Creator<ConfigEndpoints>() {
        public ConfigEndpoints createFromParcel(Parcel in) {
            return new ConfigEndpoints(in);
        }

        @Override
        public ConfigEndpoints[] newArray(int size) {
            return new ConfigEndpoints[0];
        }
    };

    public ConfigEndpoints(Parcel in) {
        mUrl = in.readString();
        setAuthenticating(in.readInt() == 1);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mUrl);
        dest.writeInt(isAuthenticating() ? 1 : 0);
    }
}
