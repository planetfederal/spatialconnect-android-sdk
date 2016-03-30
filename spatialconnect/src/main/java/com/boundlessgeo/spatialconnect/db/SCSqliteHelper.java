/*
 * Copyright 2016 Boundless, http://boundlessgeo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License
 */

package com.boundlessgeo.spatialconnect.db;

import android.content.Context;

import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteOpenHelper;

import java.io.File;

/**
 * SCSqliteHelper is used to execute SQL requests and manage sqlite databases using the custom build of sqlite
 * embedded with the spatialconnect-android-sdk.
 */
public class SCSqliteHelper extends SQLiteOpenHelper {

    private static SCSqliteHelper INSTANCE;
    private static final String DATABASE_NAME = "spatialconnect";
    private static final int DATABASE_VERSION = 1;
    private Context context;

    static {
        System.loadLibrary("sqliteX");
    }

    private SCSqliteHelper(Context context) {
        // http://stackoverflow.com/questions/26642797/android-custom-sqlite-build-cannot-open-database
        super(context, context.getDatabasePath(DATABASE_NAME).getPath(), null, DATABASE_VERSION);
        this.context = context;
        File dbDirectory = context.getDatabasePath(DATABASE_NAME).getParentFile();
        if (!dbDirectory.exists()) {
            // force initialization if database directory doesn't exist
            dbDirectory.mkdir();
            getReadableDatabase();
        }
    }

    /**
     * Get the singleton instance of SCSqliteHelper for a given database.  Remember to pass the application context
     * (not the Activity's context).
     *
     * @param context      the application context
     * @return
     */
    public static synchronized SCSqliteHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SCSqliteHelper(context);
        }
        return INSTANCE;
    }

    /**
     * Called when the database connection is being configured.  Configure database settings for things like foreign
     * key support, write-ahead logging, etc.
     *
     * @param db The database.
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Called when the database is created for the FIRST time.  If a database already exists on disk with the same
     * DATABASE_NAME, this method will NOT be called.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_STORES_TABLE = "CREATE TABLE IF NOT EXISTS stores(\n" +
                "id INTEGER PRIMARY KEY,\n" +
                "storeId TEXT UNIQUE,\n" +
                "type TEXT,\n" +
                "version TEXT,\n" +
                "name TEXT,\n" +
                "uri TEXT);";
        db.execSQL(CREATE_STORES_TABLE);
    }

    /**
     * Called when the database needs to be upgraded.  This method will only be called if a database already exists on
     * disk with the same DATABASE_NAME, but the DATABASE_VERSION is different than the version of the database that
     * exists on disk.
     *
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: implement me
    }
}
