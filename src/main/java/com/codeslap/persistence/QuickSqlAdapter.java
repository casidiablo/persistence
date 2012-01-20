package com.codeslap.persistence;

import android.content.Context;

import java.util.List;

class QuickSqlAdapter implements SqlAdapter {

    private final Context mContext;
    private final String mDbName;

    QuickSqlAdapter(Context context, String dbName) {
        mContext = context;
        mDbName = dbName;
    }

    @Override
    public <T> Object store(T object) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        Object id = adapter.store(object);
        adapter.close();
        return id;
    }

    @Override
    public <T, G> Object store(T object, G attachedTo) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        Object id = adapter.store(object, attachedTo);
        adapter.close();
        return id;
    }

    @Override
    public <T> void storeCollection(List<T> collection) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        adapter.storeCollection(collection);
        adapter.close();
    }

    @Override
    public <T> void storeUniqueCollection(List<T> collection) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        adapter.storeUniqueCollection(collection);
        adapter.close();
    }

    @Override
    public <T> int update(T object, T where) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        int count = adapter.update(object, where);
        adapter.close();
        return count;
    }

    @Override
    public <T> int update(T object, String where, String[] whereArgs) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        int count = adapter.update(object, where, whereArgs);
        adapter.close();
        return count;
    }

    @Override
    public <T> T findFirst(T where) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        T result = adapter.findFirst(where);
        adapter.close();
        return result;
    }

    @Override
    public <T> T findFirst(Class<T> clazz, String where, String[] whereArgs) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        T result = adapter.findFirst(clazz, where, whereArgs);
        adapter.close();
        return result;
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        SqlAdapter persistenceAdapter = Persistence.getSqliteAdapter(mContext, mDbName);
        List<T> feeds = persistenceAdapter.findAll(clazz);
        persistenceAdapter.close();
        return feeds;
    }

    @Override
    public <T> List<T> findAll(T where) {
        SqlAdapter persistenceAdapter = Persistence.getSqliteAdapter(mContext, mDbName);
        List<T> feeds = persistenceAdapter.findAll(where);
        persistenceAdapter.close();
        return feeds;
    }

    @Override
    public <T> List<T> findAll(T where, Constraint constraint) {
        SqlAdapter persistenceAdapter = Persistence.getSqliteAdapter(mContext, mDbName);
        List<T> feeds = persistenceAdapter.findAll(where, constraint);
        persistenceAdapter.close();
        return feeds;
    }

    @Override
    public <T, G> List<T> findAll(T where, G attachedTo) {
        SqlAdapter persistenceAdapter = Persistence.getSqliteAdapter(mContext, mDbName);
        List<T> feeds = persistenceAdapter.findAll(where, attachedTo);
        persistenceAdapter.close();
        return feeds;
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz, String where, String[] whereArgs) {
        SqlAdapter persistenceAdapter = Persistence.getSqliteAdapter(mContext, mDbName);
        List<T> feeds = persistenceAdapter.findAll(clazz, where, whereArgs);
        persistenceAdapter.close();
        return feeds;
    }

    @Override
    public <T> int delete(T where) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        int delete = adapter.delete(where);
        adapter.close();
        return delete;
    }

    @Override
    public <T> int delete(Class<T> clazz, String where, String[] whereArgs) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        int delete = adapter.delete(clazz, where, whereArgs);
        adapter.close();
        return delete;
    }

    @Override
    public <T> int count(T where) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        int count = adapter.count(where);
        adapter.close();
        return count;
    }

    @Override
    public <T> int count(Class<T> clazz) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        int count = adapter.count(clazz);
        adapter.close();
        return count;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("QuickSqlAdapter does not have an implementation of this method");
    }
}
