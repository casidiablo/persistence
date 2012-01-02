package com.egoclean.persistence;

import android.content.Context;

/**
 * Returns the application persistence adapter. You must close the adapter when you don't need it anymore.
 *
 * @author cristian
 */
public class PersistenceFactory {
    public static SqliteAdapter getSqliteAdapter(Context context) {
        return new SqliteAdapterImpl(context);
    }

    public static PreferencesAdapter getPreferenceAdapter(Context context) {
        return new PreferencesAdapterImpl(context);
    }
}
