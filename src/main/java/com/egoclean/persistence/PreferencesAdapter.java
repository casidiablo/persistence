package com.egoclean.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

/**
 * This adapter is used to persist and retrieve single beans. This is an alternative
 * to the SqliteAdapter which is more useful when we want to save collection of
 * beans that can be organized in tables.
 */
class PreferencesAdapter implements PersistenceAdapter {

    private final SharedPreferences mPreferences;
    private final PrefDao mDao;

    public PreferencesAdapter(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mDao = new PrefDao(mPreferences);
    }

    @Override
    public <T> void store(Class<? extends T> clazz, T bean, Predicate predicate) {
        mDao.persist(clazz, bean);
    }

    @Override
    public void close() {
        // no needed
    }

    @Override
    public <T> T findFirst(Class<T> clazz, Predicate where) {
        return mDao.find(clazz);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        throw new UnsupportedOperationException("PreferencesAdapter works for single objects. Not collections of them.");
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz, Predicate where) {
        throw new UnsupportedOperationException("PreferencesAdapter works for single objects. Not collections of them.");
    }

    @Override
    public <T> void delete(Class<T> clazz, Predicate predicate) {
        mDao.delete(clazz);
    }
}
