/*
 * Copyright 2012 CodeSlap
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeslap.persistence;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that will hold and manage the SQLiteDatabase object, needed to
 * persist and retrieve data. This will also create the database if it's not
 * created yet, or update it in case the version number changes.
 */
class SqliteDb {
    private static final String TAG = SqliteDb.class.getSimpleName();
    private static final Map<String, SqliteDb> instances = new HashMap<String, SqliteDb>();
    private SQLiteDatabase mSqLiteDatabase;
    private final DbOpenHelper mDbHelper;

    private SqliteDb(Context context, SqlPersistence sqlPersistence) {
        String name = sqlPersistence.getName();
        if (sqlPersistence.getOpenHelper() == null) {
            mDbHelper = new Helper(context, name, sqlPersistence.getVersion());
        } else {
            mDbHelper = sqlPersistence.getOpenHelper();
        }
        mSqLiteDatabase = mDbHelper.getWritableDatabase();
        PersistenceLogManager.d(TAG, String.format("Opening \"%s\" database... Open: %s", name, mSqLiteDatabase.isOpen()));
    }

    static SqliteDb getInstance(Context context, SqlPersistence sqlPersistence) {
        String key = sqlPersistence.getName() + sqlPersistence.getVersion();
        if (!instances.containsKey(key)) {
            instances.put(key, new SqliteDb(context, sqlPersistence));
        }
        return instances.get(key);
    }

    public SQLiteDatabase getDatabase() {
        if (mSqLiteDatabase.isOpen()) {
            return mSqLiteDatabase;
        }
        return mSqLiteDatabase = mDbHelper.getWritableDatabase();
    }

    private static class Helper extends DbOpenHelper {
        public Helper(Context context, String name, int version) {
            super(context, name, version);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Here we will the whole database and recreate it
            Cursor c = db.rawQuery("SELECT 'DROP TABLE ' || name || ';' FROM sqlite_master WHERE type = 'table' " +
                    "AND name != 'sqlite_sequence';", null);
            if (c != null && c.moveToFirst()) {
                do {
                    db.execSQL(c.getString(0));
                } while (c.moveToNext());
            }
            db.execSQL("DELETE FROM sqlite_sequence");
            onCreate(db);
        }
    }
}
