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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that will hold and manage the SQLiteDatabase object, needed to
 * persist and retrieve data. This will also create the database if it's not
 * created yet, or update it in case the version number changes.
 */
class SqliteDb {

    private static final String TAG = SqliteDb.class.getSimpleName();

    private SQLiteDatabase mSqLiteDatabase;
    private final Helper mDbHelper;

    private static class Helper extends SQLiteOpenHelper {

        private final String mName;

        public Helper(Context context, String name, int version) {
            super(context, name, null, version);
            mName = name;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // create all tables for registered classes
            SqlPersistence sqlPersistence = PersistenceConfig.getDatabase(mName);

            List<Class<?>> objects = sqlPersistence.getSqliteClasses();
            for (Class<?> clazz : objects) {
                db.execSQL(SQLHelper.getCreateTableSentence(mName, clazz));
            }
            // create all extra table for many to many relations
            List<ManyToMany> sqliteManyToMany = sqlPersistence.getSqliteManyToMany();
            for (ManyToMany manyToMany : sqliteManyToMany) {
                db.execSQL(manyToMany.getCreateTableStatement());
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            // TODO define a way to migrate data
        }

    }

    private static final Map<String, SqliteDb> instances = new HashMap<String, SqliteDb>();

    private SqliteDb(Context context, String name, int version) {
        mDbHelper = new Helper(context, name, version);
        mSqLiteDatabase = mDbHelper.getWritableDatabase();
        PersistenceLogManager.d(TAG, String.format("Opening \"%s\" database... Open: %s", name, mSqLiteDatabase.isOpen()));
    }

    static SqliteDb getInstance(Context context, String name, int version) {
        String key = name + version;
        if (!instances.containsKey(key)) {
            instances.put(key, new SqliteDb(context, name, version));
        }
        return instances.get(key);
    }

    public SQLiteDatabase getDatabase() {
        if (mSqLiteDatabase.isOpen()) {
            return mSqLiteDatabase;
        }
        return mSqLiteDatabase = mDbHelper.getWritableDatabase();
    }
}
