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
import android.database.Cursor;
import android.util.Log;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import java.util.Arrays;
import java.util.List;
import org.sqlite.database.DatabaseErrorHandler;
import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteOpenHelper;

import java.io.File;

import rx.schedulers.Schedulers;

/**
 * SCSqliteHelper is used to expose the custom build of SQLite embedded with the spatialconnect-android-sdk.  It
 * provides a {@link BriteDatabase} object which can be used to provide observable sql queries.
 */
public class SCSqliteHelper extends SQLiteOpenHelper {

    /**
     * A list of keywords used by Sqlite that we cannot use as table or column names.
     * https://www.sqlite.org/lang_keywords.html
     */
    public static final List<String> SQLITE_KEYWORDS = Arrays.asList(("ABORT,ACTION,ADD,AFTER,"
        + "ALL,ALTER,ANALYZE,AND,AS,ASC,ATTACH,AUTOINCREMENT,BEFORE,BEGIN,BETWEEN,BY,CASCADE,"
        + "CASE,CAST,CHECK,COLLATE,COLUMN,COMMIT,CONFLICT,CONSTRAINT,CREATE,CROSS,CURRENT_DATE"
        + ",CURRENT_TIME,CURRENT_TIMESTAMP,DATABASE,DEFAULT,DEFERRABLE,DEFERRED,DELETE,DESC"
        + ",DETACH,DISTINCT,DROP,EACH,ELSE,END,ESCAPE,EXCEPT,EXCLUSIVE,EXISTS,EXPLAIN,FAIL,FOR"
        + ",FOREIGN,FROM,FULL,GLOB,GROUP,HAVING,IF,IGNORE,IMMEDIATE,IN,INDEX,INDEXED,INITIALLY"
        + ",INNER,INSERT,INSTEAD,INTERSECT,INTO,IS,ISNULL,JOIN,KEY,LEFT,LIKE,LIMIT,MATCH,NATURAL"
        + ",NO,NOT,NOTNULL,NULL,OF,OFFSET,ON,OR,ORDER,OUTER,PLAN,PRAGMA,PRIMARY,QUERY,RAISE"
        + ",RECURSIVE,REFERENCES,REGEXP,REINDEX,RELEASE,RENAME,REPLACE,RESTRICT,RIGHT,ROLLBACK,"
        + "ROW,SAVEPOINT,SELECT,SET,TABLE,TEMP,TEMPORARY,THEN,TO,TRANSACTION,TRIGGER,UNION,"
        + "UNIQUE,UPDATE,USING,VACUUM,VALUES,VIEW,VIRTUAL,WHEN,WHERE,WITH,WITHOUT")
        .split(","));
    private static final int DATABASE_VERSION = 1;
    private SqlBrite sqlBrite = SqlBrite.create(new SqlBrite.Logger() {
        @Override
        public void log(String message) {
            Log.d("SQLBRITE", message);
        }
    });
    private BriteDatabase db = sqlBrite.wrapDatabaseHelper(this, Schedulers.io());

    static {
        System.loadLibrary("gpkg");
        System.loadLibrary("sqliteX");
    }

    /**
     * Creates a new instance of {@link SCSqliteHelper} used to interact with the database specified by the parameter
     * <b>databaseName</b>.
     *
     * @param context
     * @param databaseName
     */
    public SCSqliteHelper(Context context, String databaseName) {
        // http://stackoverflow.com/questions/26642797/android-custom-sqlite-build-cannot-open-database
        super(context, context.getDatabasePath(databaseName).getPath(), null, DATABASE_VERSION, new DatabaseErrorHandler() {
            @Override
            public void onCorruption(SQLiteDatabase dbObj) {
                throw new RuntimeException("Database is corrupted!  Cannot create SCSqliteHelper.");
            }
        });
        File dbDirectory = context.getDatabasePath(databaseName).getParentFile();
        if (!dbDirectory.exists()) {
            // force initialization if database directory doesn't exist
            // TODO: consider doing this in a background thread so it doesn't block
            dbDirectory.mkdir();
            getWritableDatabase();
        }
//        db.setLoggingEnabled(true);
    }

    /**
     * Returns the sqlbrite database instance.
     */
    public BriteDatabase db() {
        return this.db;
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
        // we aren't creating databases when using SCSqliteHelper so we don't need to implement this
    }

    /**
     * Called when the database needs to be upgraded.  This method will only be called if a database already exists on
     * disk with the same DATABASE_NAME, but the DATABASE_VERSION is different than the version of the database that
     * exists on disk.
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // we aren't updating databases when using SCSqliteHelper so we don't need to implement this
    }

    public static String getString(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndexOrThrow(columnName));
    }

    public static boolean getBoolean(Cursor cursor, String columnName) {
        return getInt(cursor, columnName) == 1;
    }

    public static long getLong(Cursor cursor, String columnName) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(columnName));
    }

    public static int getInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(columnName));
    }

    public static double getDouble(Cursor cursor, String columnName) {
        return cursor.getDouble(cursor.getColumnIndexOrThrow(columnName));
    }

    public static byte[] getBlob(Cursor cursor, String columnName) {
        return cursor.getBlob(cursor.getColumnIndexOrThrow(columnName));
    }
}
