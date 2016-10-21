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
    }
}
