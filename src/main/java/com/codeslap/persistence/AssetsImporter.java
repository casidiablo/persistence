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
import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link Importer} implementation that takes the SQL statements from an asset resource
 * @author cristian
 */
class AssetsImporter implements Importer {

    public static final String TAG = AssetsImporter.class.toString();

    private final Context mContext;
    private final String mPath;

    AssetsImporter(Context context, String path) {
        mContext = context;
        mPath = path;
    }

    @Override
    public void execute(SQLiteDatabase database) {
        long init = System.currentTimeMillis();
        PersistenceLogManager.d(TAG, String.format("Importing '%s'...", mPath));

        new StreamImporter(getInputStream()).execute(database);

        long end = System.currentTimeMillis();
        PersistenceLogManager.d(TAG, String.format("Took %dms to import '%s'", end - init, mPath));
    }

    private InputStream getInputStream() {
        InputStream is = null;
        try {
            is = mContext.getAssets().open(mPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return is;
    }
}
