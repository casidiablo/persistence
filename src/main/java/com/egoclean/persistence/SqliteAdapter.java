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
    public <T> void store(Class<T> clazz, T bean) {
        mDao.insert(clazz, bean);
    }

    @Override
    public <T> int update(Class<T> clazz, T bean, T sample) {
        T match = findFirst(clazz, sample);
        if (match == null) {// if there was NOT a matching bean, update it!
            return 0;
        } else { // if there was a matching bean, insert a new record
            return mDao.update(clazz, bean, sample);
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
    public <T> void delete(Class<T> clazz, T where) {
        mDao.delete(clazz, where);
    }

}