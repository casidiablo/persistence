/*
 * Copyright 2012 CodeSlap
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeslap.persistence;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a persistence adapter that uses sqlite database as persistence engine.
 * This is useful to persist collections of beans. To save single objects (objects
 * that don't get repeated, singletons, or any data that don't fit into the tables
 * paradigm), use PreferencesAdapter.
 */
class SqliteAdapterImpl implements SqlAdapter {
    private static final String TAG = SqliteAdapterImpl.class.getSimpleName();

    // this expression is used when inserting rows in the many-to-many relation tables. It will basically
    // prevent a row from being inserted when the values already exist.
    private static final String HACK_INSERT_FORMAT = "CASE WHEN (SELECT COUNT(*) FROM %s WHERE %s = %s AND %s = %s) == 0 THEN %s ELSE NULL END";

    private final SQLiteDatabase mDb;
    private final SqlPersistence mPersistence;
    private final Map<String, DatabaseUtils.InsertHelper> mInsertHelperMap;

    SqliteAdapterImpl(Context context, String name) {
        mPersistence = PersistenceConfig.getDatabase(name);
        SqliteDb helper = SqliteDb.getInstance(context, mPersistence.getName(), mPersistence.getVersion());
        mDb = helper.getDatabase();
        mInsertHelperMap = new HashMap<String, DatabaseUtils.InsertHelper>();
    }

    @Override
    public <T> T findFirst(T sample) {
        Class<T> clazz = (Class<T>) sample.getClass();
        ArrayList<String> args = new ArrayList<String>();
        String where = SQLHelper.getWhere(mPersistence.getName(), clazz, sample, args, null);
        Cursor query = mDb.query(SQLHelper.getTableName(clazz), null, where, args.toArray(new String[args.size()]), null, null, null, "1");
        return findFirstFromCursor(clazz, query);
    }

    @Override
    public <T> T findFirst(Class<T> clazz, String where, String[] whereArgs) {
        Cursor query = mDb.query(SQLHelper.getTableName(clazz), null, where, whereArgs, null, null, null, "1");
        return findFirstFromCursor(clazz, query);
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        T emptySample = null;
        try {
            emptySample = clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return findAll(emptySample, null);
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
        if (bean == null) {
            return null;
        }
        Class<?> theClass = bean.getClass();
        synchronized (mDb) {
            mDb.execSQL("BEGIN TRANSACTION;");
            String sqlStatement = getSqlStatement(bean, new Node(theClass), attachedTo);
            String[] statements = sqlStatement.split(SQLHelper.STATEMENT_SEPARATOR);
            for (String statement : statements) {
                if (statement.isEmpty()) {
                    continue;
                }
                mDb.execSQL(statement);
            }
            mDb.execSQL("COMMIT;");
        }
        Field idField = null;
        try {
            idField = theClass.getDeclaredField(SQLHelper.ID);
        } catch (NoSuchFieldException e) {
            PersistenceLogManager.e(TAG, "Could not find a normal ID... let's try to find by annotation...");
            for (Field field : theClass.getDeclaredFields()) {
                PrimaryKey current = field.getAnnotation(PrimaryKey.class);
                if (current != null && idField != null) {
                    throw new IllegalStateException("Cannot have two primary keys");
                } else if (current != null) {
                    idField = field;
                }
            }
        }
        // if it is autoincrement, we will try to populate the id field with the inserted id
        if (mPersistence.isAutoincrement(theClass)) {
            Cursor lastId = mDb.query("sqlite_sequence", new String[]{"seq"}, "name = ?",
                    new String[]{SQLHelper.getTableName(theClass)}, null, null, null);
            if (lastId != null && lastId.moveToFirst()) {
                long id = lastId.getLong(0);
                lastId.close();
                if (idField != null) {
                    idField.setAccessible(true);
                    try {
                        idField.set(bean, id);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                return id;
            } else {
                lastId.close();
            }
        } else {
            try {
                idField.setAccessible(true);
                return idField.get(bean);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public <T> void storeCollection(List<T> collection, ProgressListener listener) {
        storeCollection(collection, null, listener);
    }

    @Override
    public <T, G> void storeCollection(List<T> collection, G attachedTo, ProgressListener listener) {
        synchronized (mDb) {
            mDb.execSQL("BEGIN TRANSACTION;");
            if (listener != null) {
                listener.onProgressChange(0);
            }
            int progress;
            int all = collection.size() + 1; // 1 == commit phase
            for (int i = 0, collectionSize = collection.size(); i < collectionSize; i++) {
                T object = collection.get(i);
                String sqlStatement = getSqlStatement(object, new Node(object.getClass()), attachedTo);
                if (sqlStatement == null) {
                    continue;
                }
                String[] statements = sqlStatement.split(SQLHelper.STATEMENT_SEPARATOR);
                for (String statement : statements) {
                    mDb.execSQL(statement);
                }
                if (listener != null) {
                    progress = i * 100 / all;
                    listener.onProgressChange(progress);
                }
            }
            mDb.execSQL("COMMIT;");
        }
        if (listener != null) {
            listener.onProgressChange(100);
        }
    }

    @Override
    public <T> void storeUniqueCollection(List<T> collection, ProgressListener listener) {
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
                            break;
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
        storeCollection(collection, null);
    }

    @Override
    public <T> int update(T object, T sample) {
        if (object == null) {
            return 0;
        }
        ArrayList<String> args = new ArrayList<String>();
        String where = SQLHelper.getWhere(mPersistence.getName(), object.getClass(), sample, args, null);
        return update(object, where, args.toArray(new String[args.size()]));
    }

    @Override
    public <T> int update(T bean, String where, String[] whereArgs) {
        if (bean == null) {
            return 0;
        }
        int count = count(bean.getClass(), where, whereArgs);
        synchronized (mDb) {
            mDb.execSQL("BEGIN TRANSACTION;");
            if (whereArgs != null) {
                for (String arg : whereArgs) {
                    where = where.replaceFirst("\\?", String.format("'%s'", arg));
                }
            }
            String sqlStatement = SQLHelper.getUpdateStatement(bean, where);
            String[] statements = sqlStatement.split(SQLHelper.STATEMENT_SEPARATOR);
            for (String statement : statements) {
                mDb.execSQL(statement);
            }
            mDb.execSQL("COMMIT;");
        }
        return count;
    }

    @Override
    public <T> int delete(T sample) {
        return delete(sample, false);
    }

    @Override
    public <T> int delete(T sample, boolean onCascade) {
        if (sample == null) {
            return -1;
        }
        ArrayList<String> args = new ArrayList<String>();
        String where = SQLHelper.getWhere(mPersistence.getName(), sample.getClass(), sample, args, null);
        String[] argsArray = args.toArray(new String[args.size()]);
        return delete(sample.getClass(), where, argsArray);
    }

    @Override
    public <T> int delete(Class<T> theClass, String where, String[] whereArgs) {
        return delete(theClass, where, whereArgs, false);
    }

    @Override
    public <T> int delete(Class<T> theClass, String where, String[] whereArgs, boolean onCascade) {
        SqlPersistence.Relationship relationship = mPersistence.getRelationship(theClass);
        if (!relationship.equals(SqlPersistence.Relationship.UNKNOWN)) {
            Field idField = null;
            try {
                idField = theClass.getDeclaredField(SQLHelper.ID);
                idField.setAccessible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (idField != null) {
                switch (relationship) {
                    case HAS_MANY:
                        if (onCascade) {
                            HasMany hasMany = mPersistence.has(theClass);
                            List<T> toDelete = findAll(theClass, where, whereArgs);
                            for (T object : toDelete) {
                                try {
                                    Object objectId = idField.get(object);
                                    Class<?>[] classes = hasMany.getClasses();
                                    Class<?> containedClass = classes[1];
                                    String whereForeign = String.format("%s = '%s'", hasMany.getForeignKey(), String.valueOf(objectId));
                                    delete(containedClass, whereForeign, null);
                                } catch (IllegalAccessException ignored) {
                                }
                            }
                        }
                        break;
                    case MANY_TO_MANY:
                        List<ManyToMany> manyToManyList = mPersistence.getManyToMany(theClass);
                        for (ManyToMany manyToMany : manyToManyList) {
                            Class<?>[] classes = manyToMany.getClasses();
                            String foreignKey;
                            String foreignCurrentKey;
                            Class<?> relationTable;
                            if (classes[0] == theClass) {
                                foreignKey = manyToMany.getMainKey();
                                foreignCurrentKey = manyToMany.getSecondaryKey();
                                relationTable = classes[1];
                            } else {
                                foreignKey = manyToMany.getSecondaryKey();
                                foreignCurrentKey = manyToMany.getMainKey();
                                relationTable = classes[0];
                            }
                            List<T> toRemove = findAll(theClass, where, whereArgs);
                            for (T object : toRemove) {
                                try {
                                    Object objectId = idField.get(object);
                                    String whereForeign = String.format("%s = '%s'", foreignKey, String.valueOf(objectId));

                                    List<String> ids = new ArrayList<String>();
                                    if (onCascade) {
                                        Cursor deletionCursor = mDb.query(manyToMany.getTableName(), null, whereForeign, null, null, null, null);
                                        if (deletionCursor.moveToFirst()) {
                                            do {
                                                int index = deletionCursor.getColumnIndex(foreignCurrentKey);
                                                ids.add(deletionCursor.getString(index));
                                            } while (deletionCursor.moveToNext());
                                        }
                                        deletionCursor.close();
                                    }

                                    mDb.delete(manyToMany.getTableName(), whereForeign, null);

                                    for (String id : ids) {
                                        String whereRest = String.format("%s = '%s'", foreignCurrentKey, id);
                                        Cursor cursorRest = mDb.query(manyToMany.getTableName(), null, whereRest, null, null, null, null);
                                        // this means there is no other relation with this object, so we can delete it on cascade :)
                                        if (cursorRest.getCount() == 0) {
                                            mDb.delete(SQLHelper.getTableName(relationTable), SQLHelper.ID + " = ?", new String[]{id});
                                        }
                                    }
                                } catch (IllegalAccessException ignored) {
                                }
                            }
                        }
                        break;
                }
            }
        }

        return mDb.delete(SQLHelper.getTableName(theClass), where, whereArgs);
    }

    @Override
    public void truncate(Class<?>... classes) {
        for (Class<?> theClass : classes) {
            String tableName = SQLHelper.getTableName(theClass);
            mDb.delete(tableName, null, null);
            mDb.delete("sqlite_sequence", "name LIKE ?", new String[]{tableName});
        }
    }

    @Override
    public <T> int count(T bean) {
        Cursor query = getCursorFindAllWhere(bean.getClass(), bean, null, null);
        int count = query.getCount();
        query.close();
        return count;
    }

    @Override
    public <T> int count(Class<T> clazz, String where, String[] whereArgs) {
        Cursor query = mDb.query(SQLHelper.getTableName(clazz), null, where, whereArgs, null, null, null);
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
            if (where == null || where.trim().isEmpty()) {
                where = null;
            } else {
                selectionArgs = args.toArray(new String[args.size()]);
            }
        }
        String orderBy = null;
        String limit = null;
        String groupBy = null;
        if (constraint != null) {
            orderBy = constraint.getOrderBy();
            limit = String.valueOf(constraint.getLimit());
            groupBy = constraint.getGroupBy();
        }
        return mDb.query(SQLHelper.getTableName(clazz), null, where, selectionArgs, groupBy, null, orderBy, limit);
    }

    private <T> Cursor getCursorFindAllWhere(Class<? extends T> clazz, String where, String[] args) {
        return mDb.query(SQLHelper.getTableName(clazz), null, where, args, null, null, null, null);
    }

    private <T, G> String getSqlStatement(T bean, Node tree, G attachedTo) {
        // first try to find the bean by id (if its id is not autoincrement)
        // and if it exists, do not insert it, update it
        Class<T> theClass = (Class<T>) bean.getClass();
        try {
            // get its ID
            Field theId = theClass.getDeclaredField(SQLHelper.ID);
            theId.setAccessible(true);
            Object beanId = theId.get(bean);
            if (SQLHelper.hasData(theId.getType(), beanId)) {
                // create an object of the same type of the bean with the same id to search of it
                Constructor<?> constructor = theClass.getConstructor();
                Object sample = constructor.newInstance();
                theId.set(sample, beanId);

                Object match = findFirst((T) sample);
                if (match != null) {
                    // if they are the same, do nothing...
                    if (bean.equals(match)) {
                        return null;
                    }
                    // update the bean using the just create sample
                    return SQLHelper.getUpdateStatement(bean, sample);// TODO update children
                }
            }

        } catch (Exception ignored) {
        }

        String mainInsertStatement = SQLHelper.getInsertStatement(bean, attachedTo, mPersistence);
        try {
            mainInsertStatement += getSqlInsertForChildrenOf(bean, tree);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return mainInsertStatement;
    }

    private <T> String getSqlInsertForChildrenOf(T bean, Node tree) throws IllegalAccessException {// bodom
        // get a list with the fields that are lists
        Class<?> theClass = bean.getClass();
        Field[] fields = theClass.getDeclaredFields();
        List<Field> collectionFields = new ArrayList<Field>();
        for (Field field : fields) {
            if (field.getType() == List.class) {
                collectionFields.add(field);
                field.setAccessible(true);
            }
        }

        String sqlStatement = "";
        for (Field field : collectionFields) {
            // get the generic type for this list field
            ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
            Class<?> collectionClass = (Class<?>) stringListType.getActualTypeArguments()[0];
            Node child = new Node(collectionClass);
            if (!tree.addChild(child)) {
                continue;
            }
            switch (mPersistence.getRelationship(theClass, collectionClass)) {
                case MANY_TO_MANY: {
                    List list = (List) field.get(bean);
                    if (list != null) {
                        for (Object object : list) {
                            // get the insertion SQL
                            sqlStatement += getSqlStatement(object, tree, null);
                            // insert items in the joined table
                            try {
                                // get the table name and columns
                                String relationTableName = ManyToMany.buildTableName(theClass, collectionClass);
                                String mainForeignKey = SQLHelper.getTableName(theClass) + "_id";
                                String secondaryForeignKey = SQLHelper.getTableName(collectionClass) + "_id";

                                // get the value for the main bean ID
                                Object beanId;
                                if (mPersistence.isAutoincrement(theClass)) {
                                    beanId = String.format(SQLHelper.SELECT_AUTOINCREMENT_FORMAT, SQLHelper.getTableName(theClass));
                                } else {
                                    Field mainId = theClass.getDeclaredField(SQLHelper.ID);
                                    mainId.setAccessible(true);
                                    beanId = mainId.get(bean);
                                }

                                // get the value for the secondary bean ID
                                Object secondaryId;
                                if (mPersistence.isAutoincrement(collectionClass)) {
                                    secondaryId = String.format(SQLHelper.SELECT_AUTOINCREMENT_FORMAT, SQLHelper.getTableName(collectionClass));
                                } else {
                                    Field secondaryIdField = collectionClass.getDeclaredField(SQLHelper.ID);
                                    secondaryIdField.setAccessible(true);
                                    secondaryId = secondaryIdField.get(object);
                                }

                                // build the sql statement for the insertion of the many-to-many relation
                                String hack = String.format(HACK_INSERT_FORMAT, relationTableName, mainForeignKey,
                                        String.valueOf(beanId), secondaryForeignKey,
                                        String.valueOf(secondaryId), String.valueOf(beanId));
                                sqlStatement += String.format("INSERT OR IGNORE INTO %s (%s, %s) VALUES (%s, %s);%s",
                                        relationTableName, mainForeignKey, secondaryForeignKey,
                                        hack, String.valueOf(secondaryId), SQLHelper.STATEMENT_SEPARATOR);
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
                        // prepare the object by setting the foreign value
                        sqlStatement += getSqlStatement(object, tree, bean);
                    }
                    break;
            }
            tree.removeChild(child);
        }
        return sqlStatement;
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
            throw new RuntimeException("Could not initialize object of type " + theClass + ", " + e.getMessage());
        }

        // get each field and put its value in a content values object
        Field[] fields = theClass.getDeclaredFields();
        for (Field field : fields) {
            // get the column index
            String normalize = SQLHelper.getColumnName(field);
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
                            String collectionTableName = SQLHelper.getTableName(collectionClass);
                            String sql = "SELECT * FROM " + SQLHelper.getTableName(collectionClass) +
                                    " WHERE " + SQLHelper.ID + " IN (SELECT " + collectionTableName + "_id FROM " +
                                    ManyToMany.buildTableName(theClass, collectionClass) +
                                    " WHERE " + SQLHelper.getTableName(theClass) + "_id = ?)";
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
                            join.close();
                            value = listValue;
                        }
                        break;
                        case HAS_MANY:
                            // build a query that uses the joining table and the joined object
                            HasMany belongsTo = mPersistence.belongsTo(collectionClass);
                            Class<?> containerClass = belongsTo.getClasses()[0];
                            Field throughField;
                            String through = SQLHelper.normalize(belongsTo.getThrough());
                            try {
                                throughField = containerClass.getDeclaredField(through);
                            } catch (NoSuchFieldException e) {
                                break;
                            }
                            Object foreignValue = getValueFromCursor(throughField.getType(), through, query);
                            if (foreignValue != null) {
                                String sql = "SELECT * FROM " + SQLHelper.getTableName(collectionClass) +
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
                                join.close();
                                value = listValue;
                            }
                            break;
                    }
                    tree.removeChild(node);
                }
            } else {// do not process collections here
                value = getValueFromCursor(type, SQLHelper.getColumnName(field), query);
            }
            try {
                if (value != null) {
                    field.setAccessible(true);
                    field.set(bean, value);
                }
            } catch (Exception e) {
                throw new RuntimeException(String.format("An error occurred setting value to \"%s\", (%s): %s%n", field, value, e.getMessage()));
            }
        }
        return bean;
    }

    private Object getValueFromCursor(Class<?> type, String name, Cursor query) {
        // get the column index
        int columnIndex = query.getColumnIndex(name);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SqliteAdapterImpl that = (SqliteAdapterImpl) o;

        if (mDb != null ? !mDb.equals(that.mDb) : that.mDb != null) return false;
        if (mInsertHelperMap != null ? !mInsertHelperMap.equals(that.mInsertHelperMap) : that.mInsertHelperMap != null)
            return false;
        if (mPersistence != null ? !mPersistence.equals(that.mPersistence) : that.mPersistence != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mDb != null ? mDb.hashCode() : 0;
        result = 31 * result + (mPersistence != null ? mPersistence.hashCode() : 0);
        result = 31 * result + (mInsertHelperMap != null ? mInsertHelperMap.hashCode() : 0);
        return result;
    }
}