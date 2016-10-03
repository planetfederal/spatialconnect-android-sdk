package com.boundlessgeo.spatialconnect.db;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.Map;

/**
 * The SCKVPStore provides the interface to the key value store, which is backed by SQLite.  It
 * handles connecting and
 * disconnecting to the database as well as reading and writing key value pairs to the store.
 */
public class SCKVPStore extends SQLiteOpenHelper {

  public static final int DATABASE_VERSION = 1;
  public static final String DATABASE_NAME = "kvp.db";
  public static final String LOG_TAG = SCKVPStore.class.getSimpleName();

  public SCKVPStore(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  /**
   * Put a key/value pair into the store.
   */
  public void put(String key, Object value) {
    SQLiteDatabase db = getWritableDatabase();
    try {
      db.insertOrThrow(KeyValueModel.TABLE_NAME, null,
          new KeyValueModel.KeyValueMarshal<>().key(key)
              .value(serialize(value))
              .value_type(getValueType(value))
              .asContentValues());
    } catch (SQLException ex) {
      // This can happen if a unique constraint is violated
      Log.w(LOG_TAG,
          "Couldn't put k/v pair " + key + " / " + value.toString() + ".\nb/c " + ex.getMessage());
    }
  }

  /**
   * Get the value for the key.
   *
   * @return a Map with the key and value
   */
  public Map<String, Object> getValueForKey(String key) {
    HashMap<String, Object> result = new HashMap<>();
    Cursor cursor = null;
    SQLiteDatabase db = getReadableDatabase();
    try {
      cursor = db.rawQuery(KeyValueModel.SELECT_BY_KEY, new String[] { key });
      while (cursor.moveToNext()) {
        Object value = getValue(cursor);
        if (value != null) {
          result.put(cursor.getString(cursor.getColumnIndex(KeyValueModel.KEY)), value);
        }
      }
    } finally {
      db.close();
      cursor.close();
    }
    return result;
  }

  /**
   * Get all values for the key prefix.  A LIKE query is used to match the given key prefix.
   *
   * @return a Map containing all key/value pairs that match the key prefix
   */
  public Map<String, Object> getValuesForKeyPrefix(String key) {
    HashMap<String, Object> result = new HashMap<>();
    Cursor cursor = null;
    SQLiteDatabase db = getReadableDatabase();
    try {
      cursor = db.rawQuery(KeyValueModel.SELECT_BY_KEY_LIKE, new String[] { key + "%" });
      while (cursor.moveToNext()) {
        Object value = getValue(cursor);
        if (value != null) {
          result.put(cursor.getString(cursor.getColumnIndex(KeyValueModel.KEY)), value);
        }
      }
    } finally {
      db.close();
      cursor.close();
    }
    return result;
  }

  @Override public void onCreate(SQLiteDatabase db) {
    db.execSQL(KeyValueModel.CREATE_TABLE);
  }

  @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    //TODO: implement me
  }

  /**
   * Helper method that will take the blob value and return the deserialized Object.
   */
  private Object getValue(Cursor cursor) {
    return deserialize(cursor.getBlob(cursor.getColumnIndex(KeyValueModel.VALUE)));
  }

  /**
   * Utility method to serialize and object into a byte array for storage as a BLOB in the db.
   */
  private byte[] serialize(Object value) {
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    ObjectOutputStream o = null;
    try {
      o = new ObjectOutputStream(b);
      // only store Longs and Shorts as Integers
      if (value instanceof Long || value instanceof Short) {
        o.writeObject(Integer.valueOf(value.toString()));
      }
      // only store Floats as Doublesx
      else if (value instanceof Float) {
        o.writeObject(Double.valueOf(value.toString()));
      } else {
        o.writeObject(value);
      }
      return b.toByteArray();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        b.close();
        o.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  /**
   * Utility method to deserialize the blob from the database into an Object.
   *
   * @param value the byte array containing the serial
   */
  private Object deserialize(byte[] value) {
    ByteArrayInputStream b = new ByteArrayInputStream(value);
    ObjectInputStream o = null;
    try {
      o = new ObjectInputStream(b);
      return o.readObject();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (OptionalDataException e) {
      e.printStackTrace();
    } catch (StreamCorruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        o.close();
        b.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  /**
   * Utility method to determine the int to store for the value's type.
   */
  private int getValueType(Object value) {
    if (value instanceof String) {
      return 1;
    } else if (value instanceof Integer || value instanceof Short || value instanceof Long) {
      return 3;
    } else if (value instanceof Boolean) {
      return 4;
    } else if (value instanceof Float || value instanceof Double) {
      return 5;
    }
    // default is a byte array stored as a BLOB type
    return 2;
  }
}
