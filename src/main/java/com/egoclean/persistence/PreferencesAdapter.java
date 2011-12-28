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

    private final PrefDao mDao;

    public PreferencesAdapter(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        mDao = new PrefDao(preferences);
    }

    @Override
    public <T> void store(T bean) {
        mDao.persist(bean);
    }

    @Override
    public <T, G> void store(T bean, G attachedTo) {
        mDao.persist(bean);
    }

    @Override
    public <T> int update(T bean, T where) {
        store(bean);
        return 1;
    }

    @Override
    public void close() {
        // no needed
    }

    @Override
    public <T> T findFirst(Class<T> clazz, T where) {
        return mDao.find(clazz);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        throw new UnsupportedOperationException("PreferencesAdapter works for single objects. Not collections of them.");
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz, T where) {
        throw new UnsupportedOperationException("PreferencesAdapter works for single objects. Not collections of them.");
    }

    @Override
    public <T, G> List<T> findAll(Class<T> clazz, T where, G attached) {
        throw new UnsupportedOperationException("PreferencesAdapter works for single objects. Not collections of them.");
    }

    @Override
    public <T> void delete(T where) {
        mDao.delete(where == null ? null : where.getClass());
    }
}
