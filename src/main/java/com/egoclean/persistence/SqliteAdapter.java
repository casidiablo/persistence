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
    public <T> void store(Class<? extends T> clazz, T bean, Predicate predicate) {
        if (predicate == null) {
            mDao.insert(clazz, bean);
        } else {
            T match = findFirst(clazz, predicate);
            if (match == null) {// if there was NOT a matching bean, update it!
                mDao.insert(clazz, bean);
            } else { // if there was a matching bean, insert a new record
                mDao.update(clazz, bean, predicate);
            }
        }
    }

    @Override
    public void close() {
        mSqliteDb.close();
    }

    @Override
    public <T> T findFirst(Class<T> clazz, Predicate where) {
        return mDao.findFirstWhere(clazz, where);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        return mDao.findAll(clazz);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz, Predicate where) {
        if (where == null) {
            return findAll(clazz);
        }
        if (where.getExtraTables() != null) {
            return mDao.findAll(clazz, where.getExtraTables(), where);
        }
        return mDao.findAll(clazz, where);
    }

    @Override
    public <T> void delete(Class<T> clazz, Predicate predicate) {
        mDao.delete(clazz, predicate);
    }

}