package com.codeslap.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a persistence adapter that uses sqlite database as persistence engine.
 * This is useful to persist collections of beans. To save single objects (objects
 * that don't get repeated, singletons, or any data that don't fit into the tables
 * paradigm), use PreferencesAdapter.
 */
class SqliteAdapterImpl implements SqliteAdapter {
    private final SQLiteDatabase db;

    SqliteAdapterImpl(Context context) {
        db = new SqliteDb(context).open().getWritableDatabase();
    }

    @Override
    public <T> T findFirst(Class<T> clazz, T sample) {
        String where = null;
        ArrayList<String> args = new ArrayList<String>();
        if (sample != null) {
            where = SQLHelper.getWhere(clazz, sample, args, null);// TODO is needed a findFirstWhere with attachment
        }
        Cursor query = db.query(getTableName(clazz), null, where, args.toArray(new String[args.size()]), null, null, null, "1");
        if (query.moveToFirst()) {
            T bean = getBeanFromCursor(clazz, query, new Node(clazz));
            query.close();
            return bean;
        }
        query.close();
        return null;
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        return findAll(clazz, null, null);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz, T where) {
        return findAll(clazz, where, null);
    }

    @Override
    public <T, G> List<T> findAll(Class<T> clazz, T where, G attachedTo) {
        return findAll(clazz, where, attachedTo, null);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz, T where, Constraint constraint) {
        return findAll(clazz, where, null, constraint);
    }

    @Override
    public <T> long store(T bean) {
        return store(bean, null);
    }

    @Override
    public <T, G> long store(T bean, G attachedTo) {
        return store(bean, new Node(bean.getClass()), attachedTo);
    }

    @Override
    public <T> int update(T bean, T sample) {
        if (bean == null) {
            return 0;
        }
        try {
            ContentValues values = getValuesFromBean(bean);
            ArrayList<String> args = new ArrayList<String>();
            String where = SQLHelper.getWhere(bean.getClass(), sample, args, null);// TODO update with attachment?
            return db.update(getTableName(bean.getClass()), values, where, args.toArray(new String[args.size()]));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error inserting: " + e.getMessage());
        }
    }

    @Override
    public <T> int delete(T sample) {
        if (sample == null) {
            return -1;
        }
        // TODO delete in cascade
        ArrayList<String> args = new ArrayList<String>();
        String where = SQLHelper.getWhere(sample.getClass(), sample, args, null); // TODO delete with attachment?
        return db.delete(getTableName(sample.getClass()), where, args.toArray(new String[args.size()]));
    }

    @Override
    public void close() {
        db.close();
    }

    private <T, G> List<T> findAll(Class<T> clazz, T where, G attachedTo, Constraint constraint) {
        Cursor query = getCursorFindAllWhere(clazz, where, attachedTo, constraint);
        List<T> beans = new ArrayList<T>();
        if (query.moveToFirst()) {
            do {
                T bean = getBeanFromCursor(clazz, query, new Node(clazz));
                beans.add(bean);
            } while (query.moveToNext());
        }
        query.close();
        return beans;
    }

    private <T, G> Cursor getCursorFindAllWhere(Class<? extends T> clazz, T sample, G attachedTo, Constraint constraint) {
        String[] selectionArgs = null;
        String where = null;
        if (sample != null || attachedTo != null) {
            ArrayList<String> args = new ArrayList<String>();
            where = SQLHelper.getWhere(clazz, sample, args, attachedTo);
            selectionArgs = args.toArray(new String[args.size()]);
        }
        String orderBy = null;
        String limit = null;
        String groupBy = null;
        if (constraint != null) {
            orderBy = constraint.getOrderBy();
            limit = String.valueOf(constraint.getLimit());
            groupBy = constraint.getGroupBy();
        }
        return db.query(getTableName(clazz), null, where, selectionArgs, groupBy, null, orderBy, limit);
    }

    private <T, G> long store(T bean, Node tree, G attachedTo) {
        return store(bean, tree, null, attachedTo);
    }

    private <T, G> long store(T bean, Node tree, ContentValues initialValues, G attachedTo) {
        // first try to find the bean by id (if its id is not autoincrement)
        // and if it exists, do not insert it, update it
        Class<T> theClass = (Class<T>) bean.getClass();
        if (!Persistence.getAutoIncrementList().contains(theClass)) {
            try {
                // get its ID
                Field theId = theClass.getDeclaredField(SQLHelper.ID);
                theId.setAccessible(true);
                Object beanId = theId.get(bean);

                // create an object of the same type of the bean with the same id to search of it
                Constructor<?> constructor = theClass.getConstructor();
                Object sample = constructor.newInstance();
                theId.set(sample, beanId);

                Object match = findFirst(theClass, (T) sample);
                if (match != null) {
                    // if they are the same, do nothing...
                    if (bean.equals(match)) {
                        return (Long) beanId;
                    }
                    // update the bean using the just create sample
                    update(bean, sample);
                    return (Long) beanId;
                }
            } catch (Exception ignored) {
            }
        }

        try {
            ContentValues values = getValuesFromBean(bean);
            if (initialValues != null) {
                values.putAll(initialValues);
            }

            // if this object is attached to another object, try to get more values
            if (attachedTo != null) {
                ContentValues attachedValues = getValuesFromAttachment(bean, attachedTo);
                if (attachedValues != null) {
                    values.putAll(attachedValues);
                }
            }

            // if the class has an autoincrement, remove the ID
            if (Persistence.getAutoIncrementList().contains(theClass)) {
                values.remove(SQLHelper.ID);
            }

            // insert it into the database
            long id = db.insert(getTableName(theClass), null, values);

            // set the inserted ID to the bean so that children classes can know it
            if (Persistence.getAutoIncrementList().contains(theClass)) {
                try {
                    Field idField = theClass.getDeclaredField(SQLHelper.ID);
                    idField.setAccessible(true);
                    idField.set(bean, id);
                } catch (NoSuchFieldException ignored) {
                }
            }

            insertChildrenOf(bean, tree);
            return id;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error inserting: " + e.getMessage());
        }
    }

    private <T, G> ContentValues getValuesFromAttachment(T bean, G attachedTo) {
        switch (Persistence.getRelationship(attachedTo.getClass(), bean.getClass())) {
            case HAS_MANY: {
                try {
                    HasMany hasMany = Persistence.belongsTo(bean.getClass());
                    Field primaryForeignKey = attachedTo.getClass().getDeclaredField(hasMany.getThrough());
                    primaryForeignKey.setAccessible(true);
                    Object foreignValue = primaryForeignKey.get(attachedTo);
                    return getValuesFromObject(foreignValue, hasMany.getForeignKey());
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private <T> ContentValues getValuesFromBean(T bean) throws IllegalAccessException {
        ContentValues values = new ContentValues();
        Class theClass = bean.getClass();
        // get each field and put its value in a content values object
        Field[] fields = theClass.getDeclaredFields();
        for (Field field : fields) {
            String normalize = SQLHelper.normalize(field.getName());
            Class type = field.getType();
            field.setAccessible(true);

            if (type == int.class || type == Integer.class) {
                values.put(normalize, (Integer) field.get(bean));
            } else if (type == long.class || type == Long.class) {
                if (SQLHelper.ID.equals(field.getName()) && field.get(bean) == null) {
                    // this means we are referring to a primary key that has not been set yet... so do not add it
                    continue;
                }
                values.put(normalize, (Long) field.get(bean));
            } else if (type == boolean.class || type == Boolean.class) {
                values.put(normalize, (Integer) field.get(bean));
            } else if (type == float.class || type == Float.class) {
                values.put(normalize, (Float) field.get(bean));
            } else if (type == double.class || type == Double.class) {
                values.put(normalize, (Double) field.get(bean));
            } else if (type != List.class) {
                values.put(normalize, (String) field.get(bean));
            }
        }
        return values;
    }

    private <T> void insertChildrenOf(T bean, Node tree) throws IllegalAccessException {// bodom
        Class<?> theClass = bean.getClass();
        Field[] fields = theClass.getDeclaredFields();
        List<Field> collectionFields = new ArrayList<Field>();
        for (Field field : fields) {
            if (field.getType() == List.class) {
                collectionFields.add(field);
                field.setAccessible(true);
            }
        }

        for (Field field : collectionFields) {
            ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
            Class<?> collectionClass = (Class<?>) stringListType.getActualTypeArguments()[0];
            Node child = new Node(collectionClass);
            if (tree.addChild(child)) {
                switch (Persistence.getRelationship(theClass, collectionClass)) {
                    case MANY_TO_MANY: {
                        List list = (List) field.get(bean);
                        if (list != null) {
                            for (Object object : list) {
                                long insert = store(object, tree);
                                // insert items in the joined table
                                try {
                                    Field id = theClass.getDeclaredField(SQLHelper.ID);
                                    id.setAccessible(true);
                                    Long beanId = (Long) id.get(bean);

                                    // get the table name and columns
                                    String relationTableName = ManyToMany.getTableName(theClass.getSimpleName(), collectionClass.getSimpleName());
                                    String mainForeignKey = getTableName(theClass) + "_id";
                                    String secondaryForeignKey = getTableName(collectionClass) + "_id";

                                    // if the relation already existed, do not create it again
                                    String[] selectionArgs = new String[2];
                                    selectionArgs[0] = String.valueOf(beanId);
                                    selectionArgs[1] = String.valueOf(insert);
                                    String selection = mainForeignKey + " = ? AND " + secondaryForeignKey + " = ?";
                                    Cursor query = db.query(relationTableName, null, selection, selectionArgs, null, null, null);
                                    int count = query.getCount();
                                    query.close();
                                    if (count <= 0) {
                                        ContentValues joinValues = new ContentValues();
                                        joinValues.put(mainForeignKey, beanId);
                                        joinValues.put(secondaryForeignKey, insert);
                                        db.insert(relationTableName,
                                                null, joinValues);
                                    }
                                } catch (NoSuchFieldException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        break;
                    }
                    case HAS_MANY:
                        List list = (List) field.get(bean);
                        for (Object object : list) {
                            try {
                                // prepare the object by setting the foreign value
                                HasMany hasMany = Persistence.belongsTo(collectionClass);
                                Field primaryForeignKey = theClass.getDeclaredField(hasMany.getThrough());
                                primaryForeignKey.setAccessible(true);
                                Object foreignValue = primaryForeignKey.get(bean);

                                // insert items in the relation table
                                ContentValues relationValues = getValuesFromObject(foreignValue, hasMany.getForeignKey());
                                store(object, tree, relationValues);
                            } catch (NoSuchFieldException ignored) {
                            }
                        }
                        break;
                }
                tree.removeChild(child);
            }
        }
    }

    private ContentValues getValuesFromObject(Object object, String key) {
        if (object == null) {
            return null;
        }
        Class<?> type = object.getClass();
        ContentValues values = new ContentValues();
        if (type == int.class || type == Integer.class) {
            values.put(key, (Integer) object);
        } else if (type == long.class || type == Long.class) {
            values.put(key, (Long) object);
        } else if (type == boolean.class || type == Boolean.class) {
            values.put(key, (Integer) object);
        } else if (type == float.class || type == Float.class) {
            values.put(key, (Float) object);
        } else if (type == double.class || type == Double.class) {
            values.put(key, (Double) object);
        } else {
            values.put(key, object.toString());
        }

        return values;
    }

    private <T> T getBeanFromCursor(Class<? extends T> theClass, Cursor query, Node tree) {
        T bean;
        try {
            Constructor<? extends T> constructor = theClass.getConstructor();
            bean = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize object of type " + theClass);
        }

        // get each field and put its value in a content values object
        Field[] fields = theClass.getDeclaredFields();
        for (Field field : fields) {
            // get the column index
            String normalize = SQLHelper.normalize(field.getName());
            int columnIndex = query.getColumnIndex(normalize);
            // get an object value depending on the type
            Class type = field.getType();
            Object value = null;
            if (columnIndex == -1 && type == List.class) {
                ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
                Class<?> collectionClass = (Class<?>) stringListType.getActualTypeArguments()[0];
                Node node = new Node(collectionClass);
                if (tree.addChild(node)) {
                    switch (Persistence.getRelationship(theClass, collectionClass)) {
                        case MANY_TO_MANY: {
                            // build a query that uses the joining table and the joined object
                            long id = query.getLong(query.getColumnIndex(SQLHelper.ID));
                            String collectionTableName = getTableName(collectionClass);
                            String sql = "SELECT * FROM " + getTableName(collectionClass) +
                                    " WHERE " + SQLHelper.ID + " IN (SELECT " + collectionTableName + "_id FROM " +
                                    ManyToMany.getTableName(theClass.getSimpleName(), collectionClass.getSimpleName()) +
                                    " WHERE " + getTableName(theClass) + "_id = ?)";
                            // execute the query
                            String[] selectionArgs = new String[1];
                            selectionArgs[0] = String.valueOf(id);
                            Cursor join = db.rawQuery(sql, selectionArgs);
                            // set the result to the current field
                            List listValue = new ArrayList();
                            if (join.moveToFirst()) {
                                do {
                                    Object beanFromCursor = getBeanFromCursor(collectionClass, join, tree);
                                    listValue.add(beanFromCursor);
                                } while (join.moveToNext());
                            }
                            value = listValue;
                        }
                        break;
                        case HAS_MANY:
                            // build a query that uses the joining table and the joined object
                            HasMany belongsTo = Persistence.belongsTo(collectionClass);
                            Class<?> containerClass = belongsTo.getClasses()[0];
                            Field throughField;
                            try {
                                throughField = containerClass.getDeclaredField(belongsTo.getThrough());
                            } catch (NoSuchFieldException e) {
                                break;
                            }
                            Object foreignValue = getValueFromCursor(throughField.getType(), belongsTo.getThrough(), query);
                            if (foreignValue != null) {
                                String sql = "SELECT * FROM " + getTableName(collectionClass) +
                                        " WHERE " + belongsTo.getForeignKey() + " = '" + foreignValue + "'";
                                // execute the query and set the result to the current field
                                Cursor join = db.rawQuery(sql, null);
                                List listValue = new ArrayList();
                                if (join.moveToFirst()) {
                                    do {
                                        Object beanFromCursor = getBeanFromCursor(collectionClass, join, tree);
                                        listValue.add(beanFromCursor);
                                    } while (join.moveToNext());
                                }
                                value = listValue;
                            }
                            break;
                    }
                    tree.removeChild(node);
                }
            } else {// do not process collections here
                value = getValueFromCursor(type, field.getName(), query);
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
        return bean;
    }

    private Object getValueFromCursor(Class<?> type, String name, Cursor query) {
        // get the column index
        String normalize = SQLHelper.normalize(name);
        int columnIndex = query.getColumnIndex(normalize);
        // get an object value depending on the type
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
        }
        return value;
    }

    private final <T> String getTableName(Class<? extends T> clazz) {
        return SQLHelper.normalize(clazz.getSimpleName());
    }
}