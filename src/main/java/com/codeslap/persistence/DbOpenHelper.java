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

import java.util.List;

/**
 * This class will allow you to customize the database creation and, more important,
 * the upgrades you may want to perform.
 *
 * @author cristian
 */
public abstract class DbOpenHelper extends SQLiteOpenHelper {
    private DatabaseSpec mDatabaseSpec;

    public DbOpenHelper(Context context, String name, int version) {
        super(context, name, null, version);
    }

    public void setDatabaseSpec(DatabaseSpec databaseSpec) {
        mDatabaseSpec = databaseSpec;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        if (mDatabaseSpec == null) {
            throw new IllegalStateException("Database specification cannot be null at this point. Let's cry.");
        }

        // if there is something to import before creation, let's do it
        List<Importer> importers = mDatabaseSpec.getBeforeImporters();
        for (Importer importer : importers) {
            importer.execute(sqLiteDatabase);
        }

        List<Class<?>> objects = mDatabaseSpec.getSqliteClasses();
        for (Class<?> clazz : objects) {
            sqLiteDatabase.execSQL(SQLHelper.getCreateTableSentence(clazz, mDatabaseSpec));
        }
        // create all extra table for many to many relations
        List<ManyToMany> sqliteManyToMany = mDatabaseSpec.getSqliteManyToMany();
        for (ManyToMany manyToMany : sqliteManyToMany) {
            sqLiteDatabase.execSQL(manyToMany.getCreateTableStatement());
        }

        // if there is something to import after creation, let's do it
        importers = mDatabaseSpec.getAfterImporters();
        for (Importer importer : importers) {
            importer.execute(sqLiteDatabase);
        }
    }

    public abstract void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion);
}
