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

    <T> T findFirstWhere(Class<? extends T> clazz, Predicate predicate) {
        String order = null;
        String where = null;
        if (predicate != null) {
            order = predicate.getOrder();
            where = predicate.getWhere();
        }
        Cursor query = db.query(getTableName(clazz), null, where, null, null, null, order, "1");
        if (query.moveToFirst()) {
            T bean = getBeanFromCursor(clazz, query);
            query.close();
            return bean;
        }
        query.close();
        return null;
    }

    <T> List<T> findAll(Class<? extends T> clazz, Predicate predicate) {
        Cursor query = getCursorFindAllWhere(clazz, predicate);
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

    <T> List<T> findAll(Class<? extends T> clazz, String[] additionalTables, Predicate predicate) {
        Cursor query = getCursorFindAllWhere(clazz, additionalTables, predicate);
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

    <T> Cursor getCursorFindAllWhere(Class<? extends T> clazz, Predicate predicate) {
        int limitMax = predicate.getLimit();
        String limit = limitMax <= 0 ? null : String.valueOf(limitMax);
        return db.query(getTableName(clazz), null, predicate.getWhere(), null, null, null, predicate.getOrder(), limit);
    }

    <T> Cursor getCursorFindAllWhere(Class<? extends T> clazz, String[] additionalTables, Predicate predicate) {
        if (predicate.getGroupBy() == null || predicate.getGroupBy().length() == 0) {
            throw new IllegalStateException("Creating a query with multiple tables and without group by clause");
        }
        int limitMax = predicate.getLimit();
        String limit = limitMax <= 0 ? null : String.valueOf(limitMax);
        if (additionalTables == null) {
            return db.query(getTableName(clazz), null, predicate.getWhere(), null, null, null, predicate.getOrder(), limit);
        } else {
            String tables = getTableName(clazz);
            for (String additional : additionalTables) {
                if (!getTableName(clazz).equals(additional)) {
                    tables += ", " + additional;
                }
            }
            StringBuilder builder = new StringBuilder();
            builder.append("SELECT ").append(getTableName(clazz)).append(".* FROM ").append(tables);
            builder.append(" WHERE ").append(predicate.getWhere());
            builder.append(" GROUP BY ").append(predicate.getGroupBy());

            if (predicate.getOrder() != null && predicate.getOrder().length() > 0) {
                builder.append(" ORDER BY ").append(predicate.getOrder());
            }
            if (limit != null && limit.length() > 0) {
                builder.append(" LIMIT ").append(limit);
            }
            return db.rawQuery(builder.toString(), null);
        }
    }

    <T> int update(Class<? extends T> clazz, T bean, Predicate predicate) {
        try {
            ContentValues values = getValuesFromBean(bean);
            return db.update(getTableName(clazz), values, predicate.getWhere(), null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error inserting: " + e.getMessage());
        }
    }

    <T> long insert(Class<? extends T> clazz, T bean) {
        try {
            ContentValues values = getValuesFromBean(bean);
            return db.insert(getTableName(clazz), null, values);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error inserting: " + e.getMessage());
        }
    }

    <T> long delete(Class<? extends T> clazz, Predicate predicate) {
        return db.delete(getTableName(clazz), predicate == null ? null : predicate.getWhere(), null);
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
                if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
                    values.put(normalize, (Integer) field.get(bean));
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
