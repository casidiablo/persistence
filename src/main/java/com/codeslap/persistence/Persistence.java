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

import java.util.HashMap;
import java.util.Map;

/**
 * Returns the application persistence adapter. You must close the adapter when you don't need it anymore.
 *
 * @author cristian
 */
public class Persistence {
    private static final String TAG = Persistence.class.getSimpleName();

    private static final Map<String, SqlAdapter> QUICK_ADAPTERS = new HashMap<String, SqlAdapter>();

    public static SqlAdapter getSqliteAdapter(Context context, String dbName) {
        PersistenceLogManager.d(TAG, String.format("Getting database adapter for \"%s\" database", dbName));
        return new SqliteAdapterImpl(context, dbName);
    }

    public static SqlAdapter getSqliteAdapter(Context context) {
        PersistenceLogManager.d(TAG, String.format("Getting database adapter for \"%s\" database", PersistenceConfig.sFirstDatabase));
        return new SqliteAdapterImpl(context, PersistenceConfig.sFirstDatabase);
    }

    public static SqlAdapter getQuickAdapter(Context context, String name) {
        PersistenceLogManager.d(TAG, String.format("Getting quick database adapter for \"%s\" database", name));
        if (!QUICK_ADAPTERS.containsKey(name)) {
            QUICK_ADAPTERS.put(name, new QuickSqlAdapter(context, name));
        }
        return QUICK_ADAPTERS.get(name);
    }

    public static SqlAdapter quick(Context context) {
        return getQuickAdapter(context, PersistenceConfig.sFirstDatabase);
    }

    public static PreferencesAdapter getPreferenceAdapter(Context context, String name) {
        return new PrefsAdapterImpl(context, name);
    }

    public static PreferencesAdapter getPreferenceAdapter(Context context) {
        return new PrefsAdapterImpl(context);
    }
}
