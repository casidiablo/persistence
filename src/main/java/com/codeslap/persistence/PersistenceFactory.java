package com.codeslap.persistence;

import android.content.Context;

/**
 * Returns the application persistence adapter. You must close the adapter when you don't need it anymore.
 *
 * @author cristian
 */
public class PersistenceFactory {
    public static SqliteAdapter getSqliteAdapter(Context context, String name) {
        return new SqliteAdapterImpl(context, name);
    }

    public static SqliteAdapter getSqliteAdapter(Context context) {
        return new SqliteAdapterImpl(context, Persistence.sFirstDatabase);
    }

    public static PreferencesAdapter getPreferenceAdapter(Context context, String name) {
        return new PrefsAdapterImpl(context, name);
    }
}
