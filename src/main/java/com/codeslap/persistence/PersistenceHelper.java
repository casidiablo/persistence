package com.codeslap.persistence;

import android.content.Context;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Helper class that minimize the boring task of creating and closing a sqlite adapter. Useful for cases
 * where we just need to do a simple operation over one bean and want to save time and space
 */
public class PersistenceHelper {
    public static <T> T findFirst(Context context, T where) {
        return findFirst(context, Persistence.sFirstDatabase, where);
    }

    public static <T> T findFirst(Context context, String dbName, T where) {
        SqliteAdapter adapter = PersistenceFactory.getSqliteAdapter(context, dbName);
        T result = adapter.findFirst(where);
        adapter.close();
        return result;
    }

    public static <T> int delete(Context context, T where) {
        return delete(context, Persistence.sFirstDatabase, where);
    }

    public static <T> int delete(Context context, String dbName, T where) {
        SqliteAdapter adapter = PersistenceFactory.getSqliteAdapter(context, dbName);
        int delete = adapter.delete(where);
        adapter.close();
        return delete;
    }

    public static <T> Object store(Context context, T bean) {
        return store(context, Persistence.sFirstDatabase, bean);
    }

    public static <T> Object store(Context context, String dbName, T bean) {
        SqliteAdapter adapter = PersistenceFactory.getSqliteAdapter(context, dbName);
        Object id = adapter.store(bean);
        adapter.close();
        return id;
    }

    public static <T> void storeCollection(Context context, List<T> beans) {
        storeCollection(context, Persistence.sFirstDatabase, beans);
    }

    public static <T> void storeCollection(Context context, String dbName, List<T> beans) {
        SqliteAdapter adapter = PersistenceFactory.getSqliteAdapter(context, dbName);
        for (T bean : beans) {
            adapter.store(bean);
        }
        adapter.close();
    }

    /**
     * Stores the collection and removes the ones that are not in the collection
     *
     * @param context used to get a reference to the sqlite db
     * @param beans   the collection to store
     * @param <T>     the type of the collection
     */
    public static <T> void storeUniqueCollection(Context context, List<T> beans) {
        storeUniqueCollection(context, Persistence.sFirstDatabase, beans);
    }

    public static <T> void storeUniqueCollection(Context context, String dbName, List<T> beans) {
        SqliteAdapter adapter = PersistenceFactory.getSqliteAdapter(context, dbName);
        if (beans.size() > 0) {
            try {
                Class<T> theClass = (Class<T>) beans.get(0).getClass();
                Field id = theClass.getDeclaredField("id");
                id.setAccessible(true);
                List<T> allStored = adapter.findAll(theClass);
                for (T stored : allStored) {
                    boolean contained = false;
                    for (T bean : beans) {
                        Object storedId = id.get(stored);
                        Object storedBean = id.get(bean);
                        if (storedId != null && storedBean != null && storedId.equals(storedBean)) {
                            contained = true;
                        }
                    }
                    if (!contained) {
                        adapter.delete(stored);
                    }
                }
            } catch (Exception ignored) {
                // not a big deal
            }
        }
        for (T bean : beans) {
            adapter.store(bean);
        }
        adapter.close();
    }

    public static <T> List<T> findAll(Context context, Class<T> clazz) {
        return findAll(context, Persistence.sFirstDatabase, clazz);
    }

    public static <T> List<T> findAll(Context context, String dbName, Class<T> clazz) {
        SqliteAdapter persistenceAdapter = PersistenceFactory.getSqliteAdapter(context, dbName);
        List<T> feeds = persistenceAdapter.findAll(clazz);
        persistenceAdapter.close();
        return feeds;
    }

    public static <T> List<T> findAll(Context context, T where) {
        return findAll(context, Persistence.sFirstDatabase, where);
    }

    public static <T> List<T> findAll(Context context, String dbName, T where) {
        SqliteAdapter persistenceAdapter = PersistenceFactory.getSqliteAdapter(context, dbName);
        List<T> feeds = persistenceAdapter.findAll(where);
        persistenceAdapter.close();
        return feeds;
    }

    public static <T, G> List<T> findAll(Context context, T bean, G attachedTo) {
        return findAll(context, Persistence.sFirstDatabase, bean, attachedTo);
    }

    public static <T, G> List<T> findAll(Context context, String dbName, T bean, G attachedTo) {
        SqliteAdapter persistenceAdapter = PersistenceFactory.getSqliteAdapter(context, dbName);
        List<T> list = persistenceAdapter.findAll(bean, attachedTo);
        persistenceAdapter.close();
        return list;
    }

    public static <T> int count(Context context, T bean) {
        return count(context, Persistence.sFirstDatabase, bean);
    }

    public static <T> int count(Context context, String dbName, T bean) {
        SqliteAdapter adapter = PersistenceFactory.getSqliteAdapter(context, dbName);
        int count = adapter.count(bean);
        adapter.close();
        return count;
    }
}
