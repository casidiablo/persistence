package com.codeslap.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * This is a persistence adapter that uses sqlite database as persistence engine.
 * This is useful to persist collections of beans. To save single objects (objects
 * that don't get repeated, singletons, or any data that don't fit into the tables
 * paradigm), use PreferencesAdapter.
 */
class SqliteAdapterImpl implements SqlAdapter {
    private static final Map<String, FieldCache> FIELDS_CACHE = new HashMap<String, FieldCache>();
    private static final Map<Class<?>, String> TABLE_NAMES = new HashMap<Class<?>, String>();
    private final SQLiteDatabase mDb;

    private final SqlPersistence mPersistence;
    private final Map<String, DatabaseUtils.InsertHelper> mInsertHelperMap;

    SqliteAdapterImpl(Context context, String name) {
        mPersistence = Persistence.getDatabase(name);
        SqliteDb sqliteDb = new SqliteDb(context);
        sqliteDb.open(mPersistence.getName(), mPersistence.getVersion());
        mDb = sqliteDb.getWritableDatabase();
        mInsertHelperMap = new HashMap<String, DatabaseUtils.InsertHelper>();
    }

    @Override
    public <T> T findFirst(T sample) {
        Class<T> clazz = (Class<T>) sample.getClass();
        ArrayList<String> args = new ArrayList<String>();
        String where = SQLHelper.getWhere(mPersistence.getName(), clazz, sample, args, null);// TODO is needed a findFirstWhere with attachment
        Cursor query = mDb.query(getTableName(clazz), null, where, args.toArray(new String[args.size()]), null, null, null, "1");
        return findFirstFromCursor(clazz, query);
    }

    @Override
    public <T> T findFirst(Class<T> clazz, String where, String[] whereArgs) {
        Cursor query = mDb.query(getTableName(clazz), null, where, whereArgs, null, null, null, "1");
        return findFirstFromCursor(clazz, query);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        return findAll(null, null);
    }

    @Override
    public <T> List<T> findAll(T where) {
        return findAll(where, null);
    }

    @Override
    public <T, G> List<T> findAll(T where, G attachedTo) {
        return findAll((Class<T>) where.getClass(), where, attachedTo, null);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz, String where, String[] whereArgs) {
        Cursor query = getCursorFindAllWhere(clazz, where, whereArgs);
        return findAllFromCursor(clazz, query);
    }

    @Override
    public <T> List<T> findAll(T where, Constraint constraint) {
        Class<T> clazz = (Class<T>) where.getClass();
        return findAll(clazz, where, null, constraint);
    }

    @Override
    public <T> Object store(T bean) {
        return store(bean, null);
    }

    @Override
    public <T, G> Object store(T bean, G attachedTo) {
        return store(bean, new Node(bean.getClass()), attachedTo);
    }

    @Override
    public <T> void storeCollection(List<T> collection) {
        for (T object : collection) {
            store(object);
        }
    }

    @Override
    public <T> void storeUniqueCollection(List<T> collection) {
        if (collection.size() > 0) {
            try {
                Class<T> theClass = (Class<T>) collection.get(0).getClass();
                Field id = theClass.getDeclaredField("id");
                id.setAccessible(true);
                List<T> allStored = findAll(theClass);
                for (T stored : allStored) {
                    boolean contained = false;
                    for (T object : collection) {
                        Object storedId = id.get(stored);
                        Object storedObject = id.get(object);
                        if (storedId != null && storedObject != null && storedId.equals(storedObject)) {
                            contained = true;
                        }
                    }
                    if (!contained) {
                        delete(stored);
                    }
                }
            } catch (Exception ignored) {
                // not a big deal
            }
        }
        for (T object : collection) {
            store(object);
        }
    }

    @Override
    public <T> int update(T object, T sample) {
        if (object == null) {
            return 0;
        }
        try {
            ContentValues values = getValuesFromBean(object);
            ArrayList<String> args = new ArrayList<String>();
            String where = SQLHelper.getWhere(mPersistence.getName(), object.getClass(), sample, args, null);// TODO update with attachment?
            return mDb.update(getTableName(object.getClass()), values, where, args.toArray(new String[args.size()]));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error inserting: " + e.getMessage());
        }
    }

    @Override
    public <T> int update(T object, String where, String[] whereArgs) {
        if (object == null) {
            return 0;
        }
        try {
            ContentValues values = getValuesFromBean(object);
            return mDb.update(getTableName(object.getClass()), values, where, whereArgs);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error inserting: " + e.getMessage());
        }
    }

    private DatabaseUtils.InsertHelper getInsertHelper(Class<?> theClass) {
        DatabaseUtils.InsertHelper helper;
        if (mInsertHelperMap.containsKey(getTableName(theClass))) {
            helper = mInsertHelperMap.get(getTableName(theClass));
        } else {
            helper = new DatabaseUtils.InsertHelper(mDb, getTableName(theClass));
            mInsertHelperMap.put(getTableName(theClass), helper);
        }
        return helper;
    }

    @Override
    public <T> int delete(T sample) {
        if (sample == null) {
            return -1;
        }
        // TODO delete in cascade
        ArrayList<String> args = new ArrayList<String>();
        String where = SQLHelper.getWhere(mPersistence.getName(), sample.getClass(), sample, args, null); // TODO delete with attachment?
        return mDb.delete(getTableName(sample.getClass()), where, args.toArray(new String[args.size()]));
    }

    @Override
    public <T> int delete(Class<T> clazz, String where, String[] whereArgs) {
        // TODO delete in cascade
        return mDb.delete(getTableName(clazz), where, whereArgs);
    }

    @Override
    public <T> int count(T bean) {
        Cursor query = getCursorFindAllWhere(bean.getClass(), bean, null, null);
        int count = query.getCount();
        query.close();
        return count;
    }

    @Override
    public <T> int count(Class<T> clazz) {
        Cursor query = getCursorFindAllWhere(clazz, null, null, null);
        int count = query.getCount();
        query.close();
        return count;
    }

    @Override
    public void close() {
        for (String key : mInsertHelperMap.keySet()) {
            mInsertHelperMap.get(key).close();
        }
        mDb.close();
    }

    private <T, G> List<T> findAll(Class<T> clazz, T where, G attachedTo, Constraint constraint) {
        Cursor query = getCursorFindAllWhere(clazz, where, attachedTo, constraint);
        return findAllFromCursor(clazz, query);
    }

    private <T> List<T> findAllFromCursor(Class<T> clazz, Cursor query) {
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
            where = SQLHelper.getWhere(mPersistence.getName(), clazz, sample, args, attachedTo);
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
        return mDb.query(getTableName(clazz), null, where, selectionArgs, groupBy, null, orderBy, limit);
    }

    private <T> Cursor getCursorFindAllWhere(Class<? extends T> clazz, String where, String[] args) {
        return mDb.query(getTableName(clazz), null, where, args, null, null, null, null);
    }

    private <T, G> Object store(T bean, Node tree, G attachedTo) {
        // first try to find the bean by id (if its id is not autoincrement)
        // and if it exists, do not insert it, update it
        Class<T> theClass = (Class<T>) bean.getClass();
        if (!mPersistence.getAutoIncrementList().contains(theClass)) {
            try {
                // get its ID
                Field theId = theClass.getDeclaredField(SQLHelper.ID);
                theId.setAccessible(true);
                Object beanId = theId.get(bean);

                // create an object of the same type of the bean with the same id to search of it
                Constructor<?> constructor = theClass.getConstructor();
                Object sample = constructor.newInstance();
                theId.set(sample, beanId);

                Object match = findFirst((T) sample);
                if (match != null) {
                    // if they are the same, do nothing...
                    if (bean.equals(match)) {
                        return beanId;
                    }
                    // update the bean using the just create sample
                    update(bean, sample);
                    return beanId;
                }
            } catch (Exception ignored) {
            }
        }

        try {
            DatabaseUtils.InsertHelper helper = getInsertHelper(theClass);
            helper.prepareForInsert();
            matchValues(helper, bean);

            // if this object is attached to another object, try to get more values
            if (attachedTo != null) {
                ContentValues attachedValues = getValuesFromAttachment(bean, attachedTo);
                if (attachedValues != null) {
                    for (Map.Entry<String, Object> stringObjectEntry : attachedValues.valueSet()) {
                        int columnIndex = helper.getColumnIndex(stringObjectEntry.getKey());
                        helper.bind(columnIndex, stringObjectEntry.getValue().toString());
                    }
                }
            }

            // if the class has an autoincrement, remove the ID
            if (mPersistence.getAutoIncrementList().contains(theClass)) {
                helper.bindNull(helper.getColumnIndex(SQLHelper.ID));
            }

            // insert it into the database
            long id = helper.execute();
            // set the inserted ID to the bean so that children classes can know it
            if (mPersistence.getAutoIncrementList().contains(theClass)) {
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
        switch (mPersistence.getRelationship(attachedTo.getClass(), bean.getClass())) {
            case HAS_MANY: {
                try {
                    HasMany hasMany = mPersistence.belongsTo(bean.getClass());
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

            if (SQLHelper.ID.equals(field.getName()) && field.get(bean) == null) {
                // this means we are referring to a primary key that has not been set yet... so do not add it
                continue;
            }
            if (type == int.class || type == Integer.class) {
                values.put(normalize, (Integer) field.get(bean));
            } else if (type == long.class || type == Long.class) {
                values.put(normalize, (Long) field.get(bean));
            } else if (type == boolean.class || type == Boolean.class) {
                values.put(normalize, (Integer) field.get(bean));
            } else if (type == float.class || type == Float.class) {
                values.put(normalize, (Float) field.get(bean));
            } else if (type == double.class || type == Double.class) {
                values.put(normalize, (Double) field.get(bean));
            } else if (type != List.class) {
                try {
                    values.put(normalize, (String) field.get(bean));
                } catch (ClassCastException ignored) {
                    // only some types are supported... I won't store any more than that
                }
            }
        }
        return values;
    }

    private <T> void matchValues(DatabaseUtils.InsertHelper helper, T bean) throws IllegalAccessException {
        Class theClass = bean.getClass();
        if (!FIELDS_CACHE.containsKey(theClass.toString())) {
            Field[] fields = theClass.getDeclaredFields();
            FieldCache cache = new FieldCache();
            cache.fields = new ArrayList<Field>();
            cache.types = new ArrayList<Class<?>>();
            cache.indexes = new ArrayList<Integer>();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    String columnName = SQLHelper.normalize(field.getName());
                    int columnIndex = helper.getColumnIndex(columnName);

                    cache.fields.add(field);
                    cache.indexes.add(columnIndex);
                    cache.types.add(field.getType());
                } catch (Exception ignored) {
                }
            }
            FIELDS_CACHE.put(theClass.toString(), cache);
        }   // get each field and put its value in a content values object
        FieldCache cache = FIELDS_CACHE.get(theClass.toString());
        List<Field> fields = cache.fields;
        for (int i = 0, fieldsSize = fields.size(); i < fieldsSize; i++) {
            Field field = fields.get(i);
            Class type = cache.types.get(i);

            if (SQLHelper.ID.equals(field.getName()) && field.get(bean) == null) {
                // this means we are referring to a primary key that has not been set yet... so do not add it
                continue;
            }
            int index = cache.indexes.get(i);
            if (type == int.class || type == Integer.class) {
                helper.bind(index, (Integer) field.get(bean));
            } else if (type == long.class || type == Long.class) {
                helper.bind(index, (Long) field.get(bean));
            } else if (type == boolean.class || type == Boolean.class) {
                helper.bind(index, (Boolean) field.get(bean));
            } else if (type == float.class || type == Float.class) {
                helper.bind(index, (Float) field.get(bean));
            } else if (type == double.class || type == Double.class) {
                helper.bind(index, (Double) field.get(bean));
            } else if (type != List.class) {
                try {
                    helper.bind(index, (String) field.get(bean));
                } catch (ClassCastException ignored) {
                    // only some types are supported... I won't store any more than that
                }
            }
        }
    }


    private static class FieldCache {

        List<Field> fields;
        List<Class<?>> types;
        List<Integer> indexes;
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
                switch (mPersistence.getRelationship(theClass, collectionClass)) {
                    case MANY_TO_MANY: {
                        List list = (List) field.get(bean);
                        if (list != null) {
                            for (Object object : list) {
                                Object insert = store(object, tree);
                                // insert items in the joined table
                                try {
                                    Field id = theClass.getDeclaredField(SQLHelper.ID);
                                    id.setAccessible(true);
                                    Object beanId = id.get(bean);

                                    // get the table name and columns
                                    String relationTableName = ManyToMany.getTableName(theClass.getSimpleName(), collectionClass.getSimpleName());
                                    String mainForeignKey = getTableName(theClass) + "_id";
                                    String secondaryForeignKey = getTableName(collectionClass) + "_id";

                                    // if the relation already existed, do not create it again
                                    String[] selectionArgs = new String[2];
                                    selectionArgs[0] = String.valueOf(beanId);
                                    selectionArgs[1] = String.valueOf(insert);
                                    String selection = mainForeignKey + " = ? AND " + secondaryForeignKey + " = ?";
                                    Cursor query = mDb.query(relationTableName, null, selection, selectionArgs, null, null, null);
                                    int count = query.getCount();
                                    query.close();
                                    if (count <= 0) {
                                        ContentValues joinValues = new ContentValues();
                                        joinValues.put(mainForeignKey, String.valueOf(beanId));
                                        joinValues.put(secondaryForeignKey, String.valueOf(insert));
                                        mDb.insert(relationTableName,
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
                                HasMany hasMany = mPersistence.belongsTo(collectionClass);
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

    private <T> T findFirstFromCursor(Class<T> clazz, Cursor query) {
        if (query.moveToFirst()) {
            T bean = getBeanFromCursor(clazz, query, new Node(clazz));
            query.close();
            return bean;
        }
        query.close();
        return null;
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
                    switch (mPersistence.getRelationship(theClass, collectionClass)) {
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
                            Cursor join = mDb.rawQuery(sql, selectionArgs);
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
                            HasMany belongsTo = mPersistence.belongsTo(collectionClass);
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
                                Cursor join = mDb.rawQuery(sql, null);
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

    private <T> String getTableName(Class<? extends T> clazz) {
        if (TABLE_NAMES.containsKey(clazz)) {
            return TABLE_NAMES.get(clazz);
        }
        String normalize = SQLHelper.normalize(clazz.getSimpleName());
        TABLE_NAMES.put(clazz, normalize);
        return normalize;
    }
}