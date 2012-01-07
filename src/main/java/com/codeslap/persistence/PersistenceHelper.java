package com.codeslap.persistence;

import android.content.Context;

import java.util.List;

/**
 * Helper class that minimize the boring task of creating and closing a sqlite adapter. Useful for cases
 * where we just need to do a simple operation over one bean and want to save time and space
 */
public class PersistenceHelper {
    public static <T> T findFirst(Context context, Class<T> clazz, T where) {
        SqliteAdapter adapter = PersistenceFactory.getSqliteAdapter(context);
        T result = adapter.findFirst(clazz, where);
        adapter.close();
        return result;
    }

    public static <T> int delete(Context context, T where) {
        SqliteAdapter adapter = PersistenceFactory.getSqliteAdapter(context);
        int delete = adapter.delete(where);
        adapter.close();
        return delete;
    }

    public static <T> Object store(Context context, T bean) {
        SqliteAdapter adapter = PersistenceFactory.getSqliteAdapter(context);
        Object id = adapter.store(bean);
        adapter.close();
        return id;
    }

    public static <T> List<T> findAll(Context context, Class<T> clazz) {
        SqliteAdapter persistenceAdapter = PersistenceFactory.getSqliteAdapter(context);
        List<T> feeds = persistenceAdapter.findAll(clazz);
        persistenceAdapter.close();
        return feeds;
    }

    public static <T, G> List<T> findAll(Context context, Class<T> clazz, T o, G attachedTo) {
        SqliteAdapter persistenceAdapter = PersistenceFactory.getSqliteAdapter(context);
        List<T> list = persistenceAdapter.findAll(clazz, o, attachedTo);
        persistenceAdapter.close();
        return list;
    }
}
