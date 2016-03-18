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

package com.boundlessgeo.spatialconnect.scutilities;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;

import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteOpenHelper;

import java.io.File;

/**
 * SCSqliteHelper is used to execute SQL requests and manage sqlite databases using the custom build of sqlite
 * embedded with the spatialconnect-android-sdk.
 */
public class SCSqliteHelper extends SQLiteOpenHelper {

    private static SCSqliteHelper INSTANCE;
    private static final String DATABASE_NAME = "spatialconnect.db";
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
     * Get the singleton instance of SCSqliteHelper. Pass the application context (not the Activity's context).
     *
     * @param context the application context
     * @return
     */
    public static synchronized SCSqliteHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SCSqliteHelper(context);
        }
        return INSTANCE;
    }

    // Called when the database connection is being configured.
    // Configure database settings for things like foreign key support, write-ahead logging, etc.
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }


    // Called when the database is created for the FIRST time.
    // If a database already exists on disk with the same DATABASE_NAME, this method will NOT be called.
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_STORES_TABLE = "CREATE TABLE stores(\n" +
                "id INTEGER PRIMARY KEY,\n" +
                "storeId TEXT UNIQUE,\n" +
                "type TEXT,\n" +
                "version TEXT,\n" +
                "name TEXT,\n" +
                "uri TEXT);";
        db.execSQL(CREATE_STORES_TABLE);

        // TODO: do we need to create multiple indexes?  One per vector dataset in gpkg_contents?
        String create_rtree = "CREATE VIRTUAL TABLE demo_index USING rtree(\n" +
                "   id,              -- Integer primary key\n" +
                "   minX, maxX,      -- Minimum and maximum X coordinate\n" +
                "   minY, maxY       -- Minimum and maximum Y coordinate\n" +
                ");";

        db.execSQL(create_rtree);
    }

    // Called when the database needs to be upgraded.
    // This method will only be called if a database already exists on disk with the same DATABASE_NAME,
    // but the DATABASE_VERSION is different than the version of the database that exists on disk.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(SCSqliteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + "demo_index");
        onCreate(db);
    }

    public void addStore(SCStoreConfig store) {
        // persist store config
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("storeID", store.getUniqueID());
            values.put("type", store.getType());
            values.put("version", store.getVersion());
            values.put("name", store.getName());
            values.put("uri", store.getUri());
            // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
            db.insertOrThrow("stores", null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(SCSqliteHelper.class.getSimpleName(), "Error while trying to add post to database");
        } finally {
            db.endTransaction();
        }
    }

    public int getNumberOfStores() {
        int count = 0;
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try {
            Cursor cursor = db.rawQuery("select count(*) from stores", null);
            cursor.moveToFirst();
            count = cursor.getInt(0);
            cursor.close();
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(SCSqliteHelper.class.getSimpleName(), "Error while trying to add post to database");
        } finally {
            db.endTransaction();
        }
        return count;
    }

}
