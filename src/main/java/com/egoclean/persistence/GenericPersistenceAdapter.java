package com.egoclean.persistence;

import android.content.Context;

import java.util.List;

/**
 * This class uses two different adapters in order to unify and abstract the way in which persistence is done.
 *
 * @author cristian
 */
class GenericPersistenceAdapter implements PersistenceAdapter {
    private final SqliteAdapter mSqliteAdapter;
    private final PreferencesAdapter mPrefsAdapter;

    GenericPersistenceAdapter(Context context) {
        mSqliteAdapter = new SqliteAdapter(context);
        mPrefsAdapter = new PreferencesAdapter(context);
    }

    @Override
    public <T> void store(Class<? extends T> clazz, T bean, Predicate predicate) {
        try {
            mPrefsAdapter.store(clazz, bean, predicate);
        } catch (DaoNotFoundException e) {
            mSqliteAdapter.store(clazz, bean, predicate);
        }
    }

    @Override
    public void close() {
        mSqliteAdapter.close();
        mPrefsAdapter.close();
    }

    @Override
    public <T> T findFirst(Class<T> clazz, Predicate where) {
        try {
            return mPrefsAdapter.findFirst(clazz, where);
        } catch (DaoNotFoundException e) {
            return mSqliteAdapter.findFirst(clazz, where);
        }
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        return mSqliteAdapter.findAll(clazz);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz, Predicate where) {
        return mSqliteAdapter.findAll(clazz, where);
    }

    @Override
    public <T> void delete(Class<T> clazz, Predicate where) {
        try {
            mPrefsAdapter.delete(clazz, where);
        } catch (DaoNotFoundException e) {
            mSqliteAdapter.delete(clazz, where);
        }
    }
}
