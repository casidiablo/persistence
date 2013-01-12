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

import android.database.sqlite.SQLiteDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author cristian
 */
class StreamImporter implements Importer {
    private final InputStream mInputStream;

    public StreamImporter(InputStream inputStream) {
        mInputStream = inputStream;
    }

    @Override
    public void execute(SQLiteDatabase database) {
        if (mInputStream == null) {
            return;
        }
        // now read the input line by line
        BufferedReader reader = new BufferedReader(new InputStreamReader(mInputStream));
        try {
            String line;
            StringBuilder toExecute = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if ("BEGIN TRANSACTION;".equalsIgnoreCase(line) || "COMMIT;".equalsIgnoreCase(line)) {
                    continue;
                }
                toExecute.append(line);
                if (line.trim().endsWith(";")) {
                    database.execSQL(toExecute.toString());
                    toExecute = new StringBuilder();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
