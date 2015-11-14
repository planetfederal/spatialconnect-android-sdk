package com.boundlessgeo.spatialconnect.test;

import android.app.Activity;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import com.boundlessgeo.spatialconnect.SpatialConnectActivity;

public class BaseTestCase extends ActivityInstrumentationTestCase2<SpatialConnectActivity> {

    /**
     * Activity
     */
    protected Activity activity = null;

    /**
     * Test context
     */
    protected Context testContext = null;

    /**
     * Constructor
     */
    public BaseTestCase() {
        super(SpatialConnectActivity.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Set the activity and test context
        activity = getActivity();
        testContext = activity.createPackageContext(
                "com.boundlessgeo.spatialconnect.test",
                Context.CONTEXT_IGNORE_SECURITY
        );
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
