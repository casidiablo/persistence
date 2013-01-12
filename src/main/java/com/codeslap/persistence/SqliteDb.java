/*
 * Copyright 2013 CodeSlap
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
import android.database.SQLException;
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
    private final DbOpenHelper mDbHelper;

    private SqliteDb(Context context, String name, DatabaseSpec databaseSpec) {
        if (databaseSpec.mDbOpenHelperBuilder != null) {
            mDbHelper = databaseSpec.mDbOpenHelperBuilder.buildOpenHelper(context, name, databaseSpec.getVersion());
        } else {
            mDbHelper = new DefaultOpenHelper(context, name, databaseSpec.getVersion());
        }
        mDbHelper.setDatabaseSpec(databaseSpec);
        PersistenceLogManager.d(TAG, String.format("Opening \"%s\" database...", name));
    }

    static synchronized SqliteDb getInstance(Context context, String name, DatabaseSpec databaseSpec) {
        String key = name + databaseSpec.getVersion();
        if (!instances.containsKey(key)) {
            instances.put(key, new SqliteDb(context, name, databaseSpec));
        }
        return instances.get(key);
    }

    public SQLiteDatabase getDatabase() {
        return mDbHelper.getWritableDatabase();
    }

    private static class DefaultOpenHelper extends DbOpenHelper {
        public DefaultOpenHelper(Context context, String name, int version) {
            super(context, name, version);
        }

        @Override
        public void onUpgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Here we will delete the whole database and recreate it
            Cursor cursor = db.rawQuery("SELECT 'DROP TABLE ' || name || ';' FROM sqlite_master WHERE type = 'table' " +
                    "AND name != 'sqlite_sequence';", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        db.execSQL(cursor.getString(0));
                    } catch (SQLException e) {
                        // lets ignore errors while purging the database
                    }
                } while (cursor.moveToNext());
            }
            try {
                db.execSQL("DELETE FROM sqlite_sequence");
            } catch (SQLException e) {
                // sometimes the sqlite_sequence has not been created yet
            }
            onCreate(db);
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
