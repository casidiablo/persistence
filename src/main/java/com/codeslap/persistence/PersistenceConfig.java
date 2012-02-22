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

import java.util.HashMap;
import java.util.Map;

public class PersistenceConfig {

    private static final Map<String, SqlPersistence> SQL = new HashMap<String, SqlPersistence>();
    private static final Map<String, PrefsPersistence> PREFS = new HashMap<String, PrefsPersistence>();
    static String sFirstDatabase;

    public static SqlPersistence getDatabase(String name, int version) {
        if (name == null) {
            throw new IllegalArgumentException("You must provide a valid database name");
        }
        if (sFirstDatabase == null) {
            sFirstDatabase = name;
        }
        if (SQL.containsKey(name)) {
            return SQL.get(name);
        }
        SqlPersistence sqlPersistence = new SqlPersistence(name, version);
        SQL.put(name, sqlPersistence);
        return sqlPersistence;
    }

    static SqlPersistence getDatabase(String name) {
        if (SQL.containsKey(name)) {
            return SQL.get(name);
        }
        throw new IllegalStateException(String.format("There is no sql persistence for \"%s\"", name));
    }

    public static PrefsPersistence getPreference(String name) {
        if (PREFS.containsKey(name)) {
            return PREFS.get(name);
        }
        PrefsPersistence persistence = new PrefsPersistence();
        PREFS.put(name, persistence);
        return persistence;
    }

    public static PrefsPersistence getPreference() {
        return getPreference(PreferencesAdapter.DEFAULT_PREFS);
    }
}
