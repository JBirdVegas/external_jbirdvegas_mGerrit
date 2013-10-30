package com.jbirdvegas.mgerrit.helpers;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2013
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

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import com.jbirdvegas.mgerrit.R;

public class ROMHelper {

    /**
     * Given a build string, determine what ROM the user is running and
     *  ignore things such as the version information.
     */
    public static String determineRom(Context context) {
        String buildVersion = Build.DISPLAY;
        Resources r = context.getResources();
        if (buildVersion.startsWith("cm_") || buildVersion.startsWith("Cyanogenmod")) {
            return r.getString(R.string.cm_rom_name);
        } else if (buildVersion.startsWith("aokp_")) {
            return r.getString(R.string.aokp_rom_name);
        } else if (buildVersion.startsWith("pa_")) {
            return r.getString(R.string.pa_rom_name);
        } else if (buildVersion.startsWith("carbon_")) {
            return r.getString(R.string.carbon_rom_name);
        } else if (buildVersion.startsWith("MIUI")) {
            return r.getString(R.string.miui_rom_name);
        }
        return context.getResources().getString(R.string.other_rom_name);
    }

}
