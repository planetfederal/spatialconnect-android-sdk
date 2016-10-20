package com.boundlessgeo.spaconapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.boundlessgeo.spatialconnect.SpatialConnect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    protected static File remoteConfigFile;
    protected static File localConfigFile;
    private static SpatialConnect sc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            remoteConfigFile = File.createTempFile("config_remote.scfg", null, getCacheDir());
            localConfigFile = File.createTempFile("config_local.scfg", null, getCacheDir());

            // read test scconfig_remote.json file from test resources directory
            //set local test config
            InputStream is = getResources().openRawResource(R.raw.scconfig_local);
            FileOutputStream fos = new FileOutputStream(localConfigFile);
            byte[] data = new byte[is.available()];
            is.read(data);
            fos.write(data);

            //set remote test config
            is = getResources().openRawResource(R.raw.scconfig_remote);
            fos = new FileOutputStream(remoteConfigFile);
            data = new byte[is.available()];

            is.read(data);
            fos.write(data);

            is.close();
            fos.close();

            sc = SpatialConnect.getInstance();
            sc.initialize(this);
            sc.addConfig(remoteConfigFile);
            sc.startAllServices();
            sc.getAuthService().authenticate("admin@something.com", "admin");

        } catch (IOException ex) {
            System.exit(0);
        }
    }
}
