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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;

import org.sqlite.database.SQLException;
import org.sqlite.database.sqlite.SQLiteDatabase;

public class SCStoreConfigDAO {

    private SQLiteDatabase db;
    private SCSqliteHelper dbHelper;

    public SCStoreConfigDAO(Context context) {
        dbHelper = SCSqliteHelper.getInstance(context);
    }

    public void open() throws SQLException {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void addStore(SCStoreConfig store) {
        this.open();
        // persist store config
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
        }
        catch (Exception e) {
            Log.d(SCSqliteHelper.class.getSimpleName(), "Error while trying to add store to database");
        }
        finally {
            db.endTransaction();
            this.close();
        }
    }

    public int getNumberOfStores() {
        this.open();
        int count = 0;
        db.beginTransaction();
        try {
            Cursor cursor = db.rawQuery("select count(*) from stores", null);
            cursor.moveToFirst();
            count = cursor.getInt(0);
            cursor.close();
            db.setTransactionSuccessful();
        }
        catch (Exception e) {
            Log.d(SCSqliteHelper.class.getSimpleName(), "Error while trying to get number of stores");
        }
        finally {
            db.endTransaction();
            this.close();
        }
        return count;
    }
}
