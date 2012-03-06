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

import android.database.sqlite.SQLiteDatabase;

/**
 * {@link Importer} implementation that takes the SQL statements directly from a String.
 * @author cristian
 */
class RawImporter implements Importer {
    private final String mSqlStatement;

    RawImporter(String sqlStatements) {
        mSqlStatement = sqlStatements;
    }

    @Override
    public void execute(SQLiteDatabase database) {
        String[] lines = mSqlStatement.split("\n");
        StringBuilder toExecute = new StringBuilder();
        for (String line : lines) {
            if ("BEGIN TRANSACTION;".equalsIgnoreCase(line) || "COMMIT;".equalsIgnoreCase(line)) {
                continue;
            }
            toExecute.append(line);
            if (line.trim().endsWith(";")) {
                database.execSQL(toExecute.toString());
                toExecute = new StringBuilder();
            }
        }
    }
}
