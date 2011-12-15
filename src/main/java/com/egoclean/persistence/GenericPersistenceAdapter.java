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
        switch (Persistence.getPersistenceType(clazz)) {
            case SQLITE:
                mSqliteAdapter.store(clazz, bean, predicate);
                break;
            case PREFERENCES:
                mPrefsAdapter.store(clazz, bean, predicate);
                break;
            case UNKNOWN:
                throw new RuntimeException("Could not find how to store object of type " + clazz);
        }
    }

    @Override
    public void close() {
        mSqliteAdapter.close();
        mPrefsAdapter.close();
    }

    @Override
    public <T> T findFirst(Class<T> clazz, Predicate where) {
        switch (Persistence.getPersistenceType(clazz)) {
            case SQLITE:
                return mSqliteAdapter.findFirst(clazz, where);
            case PREFERENCES:
                return mPrefsAdapter.findFirst(clazz, where);
            case UNKNOWN:
            default:
                throw new RuntimeException("Could not find how to store object of type " + clazz);
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
        switch (Persistence.getPersistenceType(clazz)) {
            case SQLITE:
                mSqliteAdapter.delete(clazz, where);
                break;
            case PREFERENCES:
                mPrefsAdapter.delete(clazz, where);
                break;
            case UNKNOWN:
                throw new RuntimeException("Could not find how to store object of type " + clazz);
        }
    }
}
