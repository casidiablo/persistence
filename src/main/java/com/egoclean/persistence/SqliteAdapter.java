package com.egoclean.persistence;

import android.content.Context;

import java.util.List;

/**
 * This is a persistence adapter that uses sqlite database as persistence engine.
 * This is useful to persist collections of beans. To save single objects (objects
 * that don't get repeated, singletons, or any data that don't fit into the tables
 * paradigm), use PreferencesAdapter.
 */
class SqliteAdapter implements PersistenceAdapter {

    private final SqliteDb mSqliteDb;
    private final SqliteDao mDao;

    public SqliteAdapter(Context context) {
        mSqliteDb = new SqliteDb(context).open();
        mDao = new SqliteDao(mSqliteDb);
    }

    @Override
    public <T> void store(T bean) {
        mDao.insert(bean);
    }

    @Override
    public <T> int update(T bean, T sample) {
        T match = findFirst((Class<T>) bean.getClass(), sample);
        if (match == null) {// if there was NOT a matching bean, update it!
            return 0;
        } else { // if there was a matching bean, insert a new record
            return mDao.update(bean, sample);
        }
    }

    @Override
    public void close() {
        mSqliteDb.close();
    }

    @Override
    public <T> T findFirst(Class<T> clazz, T sample) {
        return mDao.findFirstWhere(clazz, sample);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        return mDao.findAll(clazz);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz, T where) {
        if (where == null) {
            return findAll(clazz);
        }
        return mDao.findAll(clazz, where);
    }

    @Override
    public <T> void delete(T where) {
        mDao.delete(where);
    }

}