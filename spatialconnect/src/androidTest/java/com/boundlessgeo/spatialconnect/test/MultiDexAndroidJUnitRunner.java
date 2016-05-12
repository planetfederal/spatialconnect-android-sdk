package com.boundlessgeo.spatialconnect.test;

import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.support.test.runner.AndroidJUnitRunner;

// http://stackoverflow.com/questions/32957741/android-illegalstateexception-no-instrumentation-registered-must-run-under-a-re/34590175#34590175
public class MultiDexAndroidJUnitRunner extends AndroidJUnitRunner {

    @Override
    public void onCreate(Bundle arguments) {
        MultiDex.install(getTargetContext());
        super.onCreate(arguments);
    }

}
