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
    public static final String TAG = Persistence.class.getSimpleName();

    private static final Map<String, SqlAdapter> QUICK_ADAPTERS = new HashMap<String, SqlAdapter>();

    public static SqlAdapter getSqliteAdapter(Context context, String name) {
        PersistenceLogManager.d(TAG, String.format("Getting database adapter for '%s' database", name));
        return new SqliteAdapterImpl(context, name);
    }

    public static SqlAdapter getSqliteAdapter(Context context) {
        PersistenceLogManager.d(TAG, String.format("Getting database adapter for '%s' database", PersistenceConfig.sFirstDatabase));
        return new SqliteAdapterImpl(context, PersistenceConfig.sFirstDatabase);
    }

    public static SqlAdapter getQuickAdapter(Context context, String name) {
        PersistenceLogManager.d(TAG, String.format("Getting quick database adapter for '%s' database", name));
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
}
