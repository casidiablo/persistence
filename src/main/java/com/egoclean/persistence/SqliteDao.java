package com.egoclean.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * This generic class provides some Sqlite logic that allows us to persist
 * and retrieve an object of any type. If you want to persist a bean into
 * the sqlite database, you must create a SqliteDao for that specific
 * class and implement the getValuesFromBean and getBeanFromCursor methods.
 */
class SqliteDao {
    private final SQLiteDatabase db;

    SqliteDao(SqliteDb database) {
        db = database.getWritableDatabase();
    }

    <T> List<T> findAll(Class<? extends T> clazz) {
        List<T> result = new ArrayList<T>();
        Cursor query = db.query(getTableName(clazz), null, null, null, null, null, null);
        if (query.moveToFirst()) {
            do {
                result.add(getBeanFromCursor(clazz, query));
            } while (query.moveToNext());
        }
        query.close();
        return result;
    }

    <T> T findFirstWhere(Class<? extends T> clazz, T sample) {
        String where = null;
        if (sample != null) {
            where = SQLHelper.getWhere(sample);
        }
        Cursor query = db.query(getTableName(clazz), null, where, null, null, null, null, "1");
        if (query.moveToFirst()) {
            T bean = getBeanFromCursor(clazz, query);
            query.close();
            return bean;
        }
        query.close();
        return null;
    }

    <T> List<T> findAll(Class<? extends T> clazz, T where) {
        Cursor query = getCursorFindAllWhere(clazz, where);
        List<T> beans = new ArrayList<T>();
        if (query.moveToFirst()) {
            do {
                T bean = getBeanFromCursor(clazz, query);
                beans.add(bean);
            } while (query.moveToNext());
        }
        query.close();
        return beans;
    }

    <T> Cursor getCursorFindAllWhere(Class<? extends T> clazz, T sample) {
        return db.query(getTableName(clazz), null, SQLHelper.getWhere(sample), null, null, null, null, null);
    }

    <T> int update(T bean, T sample) {
        try {
            ContentValues values = getValuesFromBean(bean);
            return db.update(getTableName(bean.getClass()), values, SQLHelper.getWhere(sample), null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error inserting: " + e.getMessage());
        }
    }

    <T> long insert(T bean) {
        try {
            ContentValues values = getValuesFromBean(bean);
            return db.insert(getTableName(bean.getClass()), null, values);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error inserting: " + e.getMessage());
        }
    }

    <T> long delete(T sample) {
        return db.delete(getTableName(sample.getClass()), SQLHelper.getWhere(sample), null);
    }

    <T> ContentValues getValuesFromBean(T bean) throws IllegalAccessException {
        ContentValues values = new ContentValues();

        // loop through the whole class hierarchy
        Class clazz = bean.getClass();
        do {
            // get each field and put its value in a content values object
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                String normalize = SqlUtils.normalize(field.getName());
                Class type = field.getType();
                field.setAccessible(true);
                if (type == int.class || type == Integer.class) {
                    values.put(normalize, (Integer) field.get(bean));
                } else if (type == long.class || type == Long.class) {
                    if (SQLHelper.ID.equals(field.getName()) && ((Long) field.get(bean)) == 0) {
                        // this means we are referring to a primary key that has not been set yet... so do not add it
                        continue;
                    }
                    values.put(normalize, (Long) field.get(bean));
                } else if (type == boolean.class || type == Boolean.class) {
                    values.put(normalize, (Integer) field.get(bean));
                } else if (type == float.class || type == Float.class || type == double.class || type == Double.class) {
                    values.put(normalize, (Float) field.get(bean));
                } else {
                    values.put(normalize, (String) field.get(bean));
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return values;
    }

    <T> T getBeanFromCursor(Class<? extends T> clazz, Cursor query) {
        // loop through the whole class hierarchy
        T bean;
        try {
            Constructor<? extends T> constructor = clazz.getConstructor();
            bean = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize object of type " + clazz);
        }

        Class<?> theClass = clazz;
        do {
            // get each field and put its value in a content values object
            Field[] fields = theClass.getDeclaredFields();
            for (Field field : fields) {
                // get the column index
                String normalize = SqlUtils.normalize(field.getName());
                int columnIndex = query.getColumnIndex(normalize);
                // get an object value depending on the type
                Class type = field.getType();
                Object value;
                if (type == int.class || type == Integer.class) {
                    value = query.getInt(columnIndex);
                } else if (type == long.class || type == Long.class) {
                    value = query.getLong(columnIndex);
                } else if (type == boolean.class || type == Boolean.class) {
                    value = query.getInt(columnIndex) == 1;
                } else if (type == float.class || type == Float.class || type == double.class || type == Double.class) {
                    value = query.getFloat(columnIndex);
                } else {
                    value = query.getString(columnIndex);
                }
                try {
                    field.setAccessible(true);
                    field.set(bean, value);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("An error occurred setting value to '%s', (%s): %s%n", field, value, e.getMessage()));
                }
            }
            theClass = theClass.getSuperclass();
        } while (theClass != Object.class);
        return bean;
    }

    final <T> String getTableName(Class<? extends T> clazz) {
        return SqlUtils.normalize(clazz.getSimpleName());
    }
}
