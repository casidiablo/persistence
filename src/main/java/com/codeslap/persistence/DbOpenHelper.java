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

/**
 * This class will allow you to customize the database creation and, more important,
 * the upgrades you may want to perform.
 *
 * @author cristian
 */
public abstract class DbOpenHelper extends SQLiteOpenHelper {
    private final String mName;
    private final int mVersion;

    public DbOpenHelper(Context context, String name, int version) {
        super(context, name, null, version);
        mName = name;
        mVersion = version;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
    }

    public abstract void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion);

    public String getName() {
        return mName;
    }

    public int getVersion() {
        return mVersion;
    }
}
