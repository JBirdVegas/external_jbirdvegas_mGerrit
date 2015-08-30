package com.jbirdvegas.mgerrit.objects;

/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2014
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

import android.util.Log;

import java.util.Comparator;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerVersion implements Comparator<ServerVersion> {

    /*** Constants indicating the first release of significant features **/
    // Release where support for getting change diffs was added
    public static final String VERSION_DIFF = "2.8";
    public static final String VERSION_STAR = "2.8";
    /* Release where support for before/after search operators (keywords) was added */
    public static final String VERSION_BEFORE_SEARCH = "2.9";

    String mVersion;

    public ServerVersion(String mVersion) {
        this.mVersion = mVersion;
    }

    /**
     * @param baseVersion The version which added the feature where support is being tested
     * @return Whether the currentVersion supports the feature added in baseVersion. I.e.
     *  currentVersion >= baseVersion
     */
    public boolean isFeatureSupported(String baseVersion) {
        return compare(this, new ServerVersion(baseVersion)) >= 0;
    }

    @Override
    /**
     * Compares the contents of a and b and returns a value indicating which has a
     *  higher version code.
     * @return -1 if version a precedes version b (a < b); 1 if a proceeds b (a > b);
     *  0 if they are the same version (a == b).
     * @throws IllegalArgumentException If either a or b are not valid version codes.
     */
    public int compare(ServerVersion lhs, ServerVersion rhs) {
        String s = "^(\\d+\\.)+\\d*|^\\d+";
        Pattern p = Pattern.compile(s);

        Matcher m1 = p.matcher(lhs.mVersion);
        Matcher m2 = p.matcher(rhs.mVersion);

        String versionA = null, versionB = null;
        if (m1.find()) versionA = m1.group(0);
        if (m2.find()) versionB = m2.group(0);

        if (versionA == null || versionB == null) {
            Log.w("ServerVersion", "One of the version numbers was not valid");
            return -1;
        }

        Scanner s1 = new Scanner(versionA);
        Scanner s2 = new Scanner(versionB);
        s1.useDelimiter("\\.");
        s2.useDelimiter("\\.");

        while (s1.hasNextInt() && s2.hasNextInt()) {
            int v1 = s1.nextInt();
            int v2 = s2.nextInt();
            if (v1 < v2) {
                return -1;
            } else if (v1 > v2) {
                return 1;
            }
        }

        if (s1.hasNextInt()) return 1; //str1 has an additional lower-level version number
        return 0;
    }

    @Override
    public String toString() {
        return mVersion.toString();
    }
}
