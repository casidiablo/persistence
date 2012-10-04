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

import static com.codeslap.persistence.PersistenceLogManager.d;

/**
 * Returns the application persistence adapter. You must close the adapter when you don't need it anymore.
 *
 * @author cristian
 */
public class Persistence {
    private static final String TAG = Persistence.class.getSimpleName();
    public static final String DEFAULT_DATABASE_NAME = "com.codeslap.persistence.db";

    /**
     * @param context used to open/create the database
     * @return implementation of the {@link SqlAdapter} that uses the default database name
     *         ({@link #DEFAULT_DATABASE_NAME}) and specification ({@link PersistenceConfig#DEFAULT_SPEC_ID})
     */
    public static SqlAdapter getAdapter(Context context) {
        return getAdapter(context, DEFAULT_DATABASE_NAME, PersistenceConfig.DEFAULT_SPEC_ID);
    }

    /**
     * @param context used to open/create the database
     * @param dbName  database name identifier
     * @return implementation of the {@link SqlAdapter} that uses the default database specification
     *         ({@link PersistenceConfig#DEFAULT_SPEC_ID})
     */
    public static SqlAdapter getAdapter(Context context, String dbName) {
        return getAdapter(context, dbName, PersistenceConfig.DEFAULT_SPEC_ID);
    }

    /**
     * @param context used to open/create the database
     * @param dbName  database name identifier
     * @param specId  database specification
     * @return implementation of the {@link SqlAdapter} using passed parameters
     */
    public static SqlAdapter getAdapter(Context context, String dbName, String specId) {
        d(TAG, String.format("Getting database adapter for \"%s\" database with \"" + specId + "\" spec", dbName));
        return new SqliteAdapterImpl(context, dbName, specId);
    }

    /**
     * @param context used to open/create the database
     * @return implementation of the {@link RawQuery} interface that uses the default database name
     *         ({@link #DEFAULT_DATABASE_NAME}) and specification ({@link PersistenceConfig#DEFAULT_SPEC_ID})
     */
    public static RawQuery getRawQuery(Context context) {
        return new RawQueryImpl(context, DEFAULT_DATABASE_NAME, PersistenceConfig.DEFAULT_SPEC_ID);
    }

    /**
     * @param context used to open/create the database
     * @param name    database name identifier
     * @return an implementation of the {@link RawQuery} interface that uses the default database specification
     *         ({@link PersistenceConfig#DEFAULT_SPEC_ID})
     */
    public static RawQuery getRawQuery(Context context, String name) {
        return new RawQueryImpl(context, name, PersistenceConfig.DEFAULT_SPEC_ID);
    }

    /**
     * @param context used to open/create the database
     * @param name    database name identifier
     * @param specId  database specification
     * @return an implementation of the {@link RawQuery} interface
     */
    public static RawQuery getRawQuery(Context context, String name, String specId) {
        return new RawQueryImpl(context, name, specId);
    }

    /**
     * @param context used to access to the preferences system
     * @param name    the name of the preference file
     * @return an implementation of the PreferencesAdapter interface.
     */
    public static PreferencesAdapter getPreferenceAdapter(Context context, String name) {
        return new PrefsAdapterImpl(context, name);
    }

    /**
     * @param context used to access to the preferences system
     * @return an implementation of the PreferencesAdapter interface pointing to the default sharepreferences
     */
    public static PreferencesAdapter getPreferenceAdapter(Context context) {
        return new PrefsAdapterImpl(context);
    }

    /**
     * Quick way to retrieve an object from the default preferences
     *
     * @param context  used to access to the preferences system
     * @param name     the name of the preference file
     * @param theClass the class to retrieve
     * @return a bean created from the preferences
     */
    public static <T> T quickPref(Context context, String name, Class<T> theClass) {
        PreferencesAdapter adapter = getPreferenceAdapter(context, name);
        return adapter.retrieve(theClass);
    }

    /**
     * Quick way to retrieve an object from the default preferences
     *
     * @param context  used to access to the preferences system
     * @param theClass the class to retrieve
     * @return a bean created from the preferences
     */
    public static <T> T quickPref(Context context, Class<T> theClass) {
        PreferencesAdapter adapter = getPreferenceAdapter(context);
        return adapter.retrieve(theClass);
    }
}
