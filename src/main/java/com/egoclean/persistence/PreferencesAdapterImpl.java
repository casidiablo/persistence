package com.egoclean.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * This adapter is used to persist and retrieve single beans. This is an alternative
 * to the SqliteAdapter which is more useful when we want to save collection of
 * beans that can be organized in tables.
 */
class PreferencesAdapterImpl implements PreferencesAdapter {

    private final PrefDao mDao;

    public PreferencesAdapterImpl(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        mDao = new PrefDao(preferences);
    }

    @Override
    public <T> void store(T bean) {
        mDao.persist(bean);
    }

    @Override
    public <T> T retrieve(Class<T> clazz){
        return null;
    }

    @Override
    public <T> void delete(Class<T> clazz) {
        mDao.delete(clazz);
    }
}
