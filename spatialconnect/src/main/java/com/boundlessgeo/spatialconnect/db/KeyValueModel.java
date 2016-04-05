package com.boundlessgeo.spatialconnect.db;

import android.content.ContentValues;

public interface KeyValueModel {

    String TABLE_NAME = "kvp";

    String _ID = "_id";

    String KEY = "key";

    String VALUE = "value";

    String VALUE_TYPE = "value_type";

    String CREATE_TABLE = ""
            + "CREATE TABLE kvp (\n"
            + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
            + "  key TEXT UNIQUE NOT NULL,\n"
            + "  value BLOB NOT NULL,\n"
            + "  value_type INTEGER NOT NULL\n"
            + ")";

    String SELECT_BY_KEY = ""
            + "SELECT *\n"
            + "FROM kvp\n"
            + "WHERE key = ?";

    String SELECT_BY_KEY_LIKE = ""
            + "SELECT *\n"
            + "FROM kvp\n"
            + "WHERE key LIKE ?";

    long _id();

    String key();

    byte[] value();

    int value_type();

    class KeyValueMarshal<T extends KeyValueMarshal<T>> {
        protected ContentValues contentValues = new ContentValues();

        public KeyValueMarshal() {
        }

        public KeyValueMarshal(KeyValueModel copy) {
            this._id(copy._id());
            this.key(copy.key());
            this.value(copy.value());
            this.value_type(copy.value_type());
        }

        public final ContentValues asContentValues() {
            return contentValues;
        }

        public T _id(long _id) {
            contentValues.put(_ID, _id);
            return (T) this;
        }

        public T key(String key) {
            contentValues.put(KEY, key);
            return (T) this;
        }

        public T value(byte[] value) {
            contentValues.put(VALUE, value);
            return (T) this;
        }

        public T value_type(int value_type) {
            contentValues.put(VALUE_TYPE, value_type);
            return (T) this;
        }
    }
}