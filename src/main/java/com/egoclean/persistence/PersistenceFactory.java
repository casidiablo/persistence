package com.egoclean.persistence;

import android.content.Context;

/**
 * Returns the application persistence adapter. You must close the adapter when you don't need it anymore.
 *
 * @author cristian
 */
public class PersistenceFactory {
    public static PersistenceAdapter getAdapter(Context context) {
        return new GenericPersistenceAdapter(context);
    }
}
