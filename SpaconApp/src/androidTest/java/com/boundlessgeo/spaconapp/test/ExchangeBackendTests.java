package com.boundlessgeo.spaconapp.test;

import android.app.Activity;
import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import com.boundlessgeo.spaconapp.MainActivity;
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.services.SCServiceStatus;
import com.boundlessgeo.spatialconnect.services.authService.SCAuthService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ExchangeBackendTests {

    protected static Activity activity = null;
    protected static Context testContext = null;
    protected static File remoteConfigFile;
    private static SpatialConnect sc;

    @ClassRule
    public static ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(MainActivity.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Set the activity and test context
        activity = rule.getActivity();
        testContext = activity.createPackageContext(
                "com.boundlessgeo.spaconapp.test",
                Context.CONTEXT_IGNORE_SECURITY
        );
        try {
            remoteConfigFile = File.createTempFile(
                "exchange_remote.json", null, activity.getCacheDir()
            );
            // read test config file from test resources directory into remoteConfigFile object
            InputStream is = testContext.getResources().openRawResource(R.raw.exchange_remote);
            FileOutputStream fos = new FileOutputStream(remoteConfigFile);
            byte[] data = new byte[is.available()];
            is.read(data);
            fos.write(data);
            is.close();
            fos.close();

            sc = SpatialConnect.getInstance();
            sc.initialize(activity);
            sc.getConfigService().addConfigFilePath(remoteConfigFile.getAbsolutePath());
            sc.startAllServices();
        } catch (IOException ex) {
            System.exit(0);
        }
    }

    @After
    public void afterTest() throws Exception {
    }

    @Test
    public void testExchangeConfigDoesNotRequireMqtt() {
        Assert.assertTrue("Backend Service should be running",
            sc.getBackendService().getStatus().equals(SCServiceStatus.SC_SERVICE_RUNNING));
        TestSubscriber authSubscriber = new TestSubscriber();
        sc.getAuthService().getLoginStatus().subscribe(authSubscriber);
        // assert that we have not authenticated yet
        authSubscriber.assertValue(SCAuthService.SCAuthStatus.NOT_AUTHENTICATED.value());
    }
}
