package com.egoclean.persistence;

import android.content.Context;

import java.util.List;

/**
 * This class uses two different adapters in order to unify and abstract the way in which persistence is done.
 *
 * @author cristian
 */
class GenericPersistenceAdapter implements PersistenceAdapter {
    private final PersistenceAdapter mSqliteAdapter;
    private final PersistenceAdapter mPrefsAdapter;

    GenericPersistenceAdapter(Context context) {
        mSqliteAdapter = new SqliteAdapter(context);
        mPrefsAdapter = new PreferencesAdapter(context);
    }

    @Override
    public <T> void store(T bean) {
        getPersister(bean.getClass()).store(bean);
    }

    @Override
    public <T> int update(T bean, T predicate) {
        return getPersister(bean.getClass()).update(bean, predicate);
    }

    @Override
    public void close() {
        mSqliteAdapter.close();
        mPrefsAdapter.close();
    }

    @Override
    public <T> T findFirst(Class<T> clazz, T where) {
        return getPersister(clazz).findFirst(clazz, where);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        return mSqliteAdapter.findAll(clazz);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz, T where) {
        return mSqliteAdapter.findAll(clazz, where);
    }

    @Override
    public <T> void delete(T where) {
        getPersister(where.getClass()).delete(where);
    }

    private <T> PersistenceAdapter getPersister(Class<T> clazz) {
        switch (Persistence.getPersistenceType(clazz)) {
            case SQLITE:
                return mSqliteAdapter;
            case PREFERENCES:
                return mPrefsAdapter;
            case UNKNOWN:
            default:
                throw new RuntimeException("Could not find how to store object of type " + clazz);
        }
    }
}
