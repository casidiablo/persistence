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

import java.util.HashMap;
import java.util.Map;

/**
 * Use this class to configure database or preferences specs.
 */
public class PersistenceConfig {

    private static final Map<String, DatabaseSpec> DB_SPECS = new HashMap<String, DatabaseSpec>();
    private static final Map<String, PrefsPersistence> PREFS = new HashMap<String, PrefsPersistence>();

    /**
     * This is the spec id used by default when registering using this method {@link PersistenceConfig#registerSpec(int)}
     */
    public static final String DEFAULT_SPEC_ID = "com.codeslap.persistence.default_spec";

    /**
     * @param context used to find the databases' path
     * @param name    the name of the database to check
     * @return true if the database already exist
     */
    public static boolean isDatabaseAlreadyCreated(Context context, String name) {
        return context.getDatabasePath(name).exists();
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

    public static void clear() {
        DB_SPECS.clear();
        PREFS.clear();
    }

    /**
     * Creates and registers a database specification
     *
     * @param version database version
     * @return the created database spec if not already created
     */
    public static DatabaseSpec registerSpec(int version) {
        return registerSpec(DEFAULT_SPEC_ID, version);
    }

    /**
     * Creates and registers a database specification
     *
     * @param specId  the specification unique identifier
     * @param version database version
     * @return the created database spec if not already created
     */
    public static DatabaseSpec registerSpec(String specId, int version) {
        if (specId == null) {
            throw new IllegalArgumentException("You must provide a spec id");
        }
        if (DB_SPECS.containsKey(specId)) {
            throw new IllegalArgumentException("There is a specification already created with " + specId + ". Use getDatabaseSpec to retrieve an existent specification.");
        }
        DatabaseSpec databaseSpec = new DatabaseSpec(version);
        DB_SPECS.put(specId, databaseSpec);
        return databaseSpec;
    }

    /**
     * @return default specification id
     */
    public static DatabaseSpec getDatabaseSpec() {
        return getDatabaseSpec(DEFAULT_SPEC_ID);
    }

    /**
     * @param specId the specification id
     * @return the database specification registered with that id if any,  throws an {@link IllegalStateException} otherwise
     */
    public static DatabaseSpec getDatabaseSpec(String specId) {
        if (DB_SPECS.containsKey(specId)) {
            return DB_SPECS.get(specId);
        }
        throw new IllegalStateException("There is no database specification for " + specId + ". Remeber: you must have already register it with registerDatabaseSpec method.");
    }
}
