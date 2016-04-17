package com.jbirdvegas.mgerrit;

import android.app.Application;

public class BuildConfigurations {
    public static void applicationOnCreate(Application application) {
        // do nothing.  Crashlytics is only for googlePlay builds
    }
}
