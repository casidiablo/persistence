package com.egoclean.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
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
                result.add(getBeanFromCursor(clazz, query, new ArrayList<Class<?>>()));
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
            T bean = getBeanFromCursor(clazz, query, new ArrayList<Class<?>>());
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
                T bean = getBeanFromCursor(clazz, query, new ArrayList<Class<?>>());
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
            ContentValues values = getValuesFromBean(bean, true);
            return db.update(getTableName(bean.getClass()), values, SQLHelper.getWhere(sample), null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error inserting: " + e.getMessage());
        }
    }

    <T> long insert(T bean) {
        return insert(bean, true);
    }

    <T> long insert(T bean, boolean deep) {
        try {
            ContentValues values = getValuesFromBean(bean, deep);
            return db.insert(getTableName(bean.getClass()), null, values);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error inserting: " + e.getMessage());
        }
    }

    <T> long delete(T sample) {
        return db.delete(getTableName(sample.getClass()), SQLHelper.getWhere(sample), null);
    }

    private <T> ContentValues getValuesFromBean(T bean, boolean deep) throws IllegalAccessException {
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
                } else if (type == List.class) {
                    if (deep) {
                        ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
                        Class<?> collectionClass = (Class<?>) stringListType.getActualTypeArguments()[0];
                        // insert items in the relation table
                        List list = (List) field.get(bean);
                        for (Object object : list) {
                            final boolean goDeep = false;
                            long insert = insert(object, goDeep);
                            // insert items in the joined table
                            try {
                                Field id = clazz.getDeclaredField(SQLHelper.ID);
                                id.setAccessible(true);
                                Long beanId = (Long) id.get(bean);

                                ContentValues joinValues = new ContentValues();
                                joinValues.put(getTableName(clazz) + "_id", beanId);
                                joinValues.put(getTableName(collectionClass) + "_id", insert);

                                db.insert(ManyToMany.getTableName(clazz.getSimpleName(), collectionClass.getSimpleName()),
                                        null, joinValues);
                            } catch (NoSuchFieldException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    values.put(normalize, (String) field.get(bean));
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return values;
    }

    <T> T getBeanFromCursor(Class<? extends T> clazz, Cursor query, List<Class<?>> toSkip) {
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
                Object value = null;
                if (type == int.class || type == Integer.class) {
                    value = query.getInt(columnIndex);
                } else if (type == long.class || type == Long.class) {
                    value = query.getLong(columnIndex);
                } else if (type == boolean.class || type == Boolean.class) {
                    value = query.getInt(columnIndex) == 1;
                } else if (type == float.class || type == Float.class || type == double.class || type == Double.class) {
                    value = query.getFloat(columnIndex);
                } else if (type == String.class) {
                    value = query.getString(columnIndex);
                } else if (columnIndex == -1 && type == List.class) {// it could be a collection
                    ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
                    Class<?> collectionClass = (Class<?>) stringListType.getActualTypeArguments()[0];
                    if (!toSkip.contains(theClass)) {
                        switch (Persistence.getRelationship(theClass, collectionClass)) {
                            case MANY_TO_MANY:
                                // build a query that uses the joining table and the joined object

                                long id = query.getLong(query.getColumnIndex(SQLHelper.ID));

                                String collectionTableName = getTableName(collectionClass);
                                String sql = "SELECT * FROM " + getTableName(collectionClass) +
                                        " WHERE " + SQLHelper.ID + " IN (SELECT " + collectionTableName + "_id FROM " +
                                        ManyToMany.getTableName(theClass.getSimpleName(), collectionClass.getSimpleName()) +
                                        " WHERE " + getTableName(theClass) + "_id = '" + id + "')";
                                // execute the query
                                Cursor join = db.rawQuery(sql, null);
                                // set the result to the current field
                                List listValue = new ArrayList();
                                if (join.moveToFirst()) {
                                    do {
                                        if (!toSkip.contains(theClass)) {
                                            toSkip.add(theClass);
                                        }
                                        Object beanFromCursor = getBeanFromCursor(collectionClass, join, toSkip);
                                        listValue.add(beanFromCursor);
                                    } while (join.moveToNext());
                                }
                                value = listValue;
                                break;
                        }
                    }
                }
                try {
                    if (value != null) {
                        field.setAccessible(true);
                        field.set(bean, value);
                    }
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
