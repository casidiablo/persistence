/*
 * Copyright 2013 CodeSlap
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
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This is a persistence adapter that uses sqlite database as persistence engine.
 * This is useful to persist collections of beans. To save single objects (objects
 * that don't get repeated, singletons, or any data that don't fit into the tables
 * paradigm), use PreferencesAdapter.
 */
public class SqliteAdapterImpl implements SqlAdapter {

    // this expression is used when inserting rows in the many-to-many relation tables. It will basically
    // prevent a row from being inserted when the values already exist.
    private static final String HACK_INSERT_FORMAT = "CASE WHEN (SELECT COUNT(*) FROM %s WHERE %s = %s AND %s = %s) == 0 THEN %s ELSE NULL END";
    private static final String TAG = "sqliteImpl";

    private final DatabaseSpec mDatabaseSpec;
    private final SqliteDb mDbHelper;

    SqliteAdapterImpl(Context context, String name, String specId) {
        mDatabaseSpec = PersistenceConfig.getDatabaseSpec(specId);
        mDbHelper = SqliteDb.getInstance(context, name, mDatabaseSpec);
    }

    @Override
    public <T> T findFirst(T sample) {
        Class<T> clazz = (Class<T>) sample.getClass();
        ArrayList<String> args = new ArrayList<String>();
        String where = SQLHelper.getWhere(clazz, sample, args, null, mDatabaseSpec);
        Cursor query = mDbHelper.getDatabase().query(SQLHelper.getTableName(clazz), null, where, args.toArray(new String[args.size()]), null, null, null, "1");
        return findFirstFromCursor(clazz, query);
    }

    @Override
    public <T> T findFirst(Class<T> clazz, String where, String[] whereArgs) {
        Cursor query = mDbHelper.getDatabase().query(SQLHelper.getTableName(clazz), null, where, whereArgs, null, null, null, "1");
        return findFirstFromCursor(clazz, query);
    }

    @Override
    public <T> List<T> findAll(Class<T> theClass) {
        T emptySample = null;
        try {
            emptySample = theClass.newInstance();
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
        List<String> transactions = new ArrayList<String>();
        String sqlStatement = getSqlStatement(bean, new Node(theClass), attachedTo);
        if (sqlStatement != null) {
            String[] statements = sqlStatement.split(SQLHelper.STATEMENT_SEPARATOR);
            for (String statement : statements) {
                if (TextUtils.isEmpty(statement)) {
                    continue;
                }
                transactions.add(statement);
            }
        }
        executeTransactions(transactions);
        Field idField = SQLHelper.getPrimaryKeyField(theClass);
        // if it is autoincrement, we will try to populate the id field with the inserted id
        if (mDatabaseSpec.isAutoincrement(theClass)) {
            Cursor lastId = mDbHelper.getDatabase().query("sqlite_sequence", new String[]{"seq"}, "name = ?",
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
            } else if (lastId != null) {
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
        if (listener != null) {
            listener.onProgressChange(0);
        }
        if (collection.isEmpty()) {
            return;
        }
        List<String> transactions = new ArrayList<String>();
        DatabaseSpec.Relationship relationship = mDatabaseSpec.getRelationship(collection.get(0).getClass());
        // if there is no listener, attached object, collection is too small or objects in the list have inner
        // relationships: insert them in a normal way, in which there will be a sql execution per object
        if (listener != null || attachedTo != null || collection.size() <= 1 || relationship != DatabaseSpec.Relationship.UNKNOWN) {
            int progress;
            int all = collection.size() + 1; // 1 == commit phase
            for (int i = 0, collectionSize = collection.size(); i < collectionSize; i++) {
                T object = collection.get(i);
                String sqlStatement = getSqlStatement(object, new Node(object.getClass()), attachedTo);
                if (sqlStatement == null) {
                    continue;
                }
                String[] statements = sqlStatement.split(SQLHelper.STATEMENT_SEPARATOR);
                Collections.addAll(transactions, statements);
                if (listener != null) {
                    progress = i * 100 / all;
                    listener.onProgressChange(progress);
                }
            }
        } else {
            // get current table size
            int count = count(collection.get(0).getClass());
            boolean tryToUpdate = count > 0;

            // if it reaches here, we can insert collection in a faster way by creating few sql statements
            StringBuilder builder = new StringBuilder();
            for (int i = 0, collectionSize = collection.size(), newItems = 0; i < collectionSize; i++) {
                T bean = collection.get(i);
                if (tryToUpdate) {
                    String updateStatement = getUpdateStatementIfPossible(bean);
                    if (!TextUtils.isEmpty(updateStatement)) {
                        String[] statements = updateStatement.split(SQLHelper.STATEMENT_SEPARATOR);
                        Collections.addAll(transactions, statements);
                        continue;
                    }
                }
                if (newItems % 400 == 0) {
                    if (newItems > 0) {
                        transactions.add(builder.append(";").toString());
                        builder = new StringBuilder();
                    }
                    builder.append(SQLHelper.getFastInsertSqlHeader(bean, mDatabaseSpec));
                } else {
                    builder.append(SQLHelper.getUnionInsertSql(bean, mDatabaseSpec));
                }
                newItems++;
            }
            if (builder.length() > 0) {
                String sql = builder.append(";").toString();
                transactions.add(sql);
            }
        }
        executeTransactions(transactions);
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
        storeCollection(collection, listener);
    }

    @Override
    public <T> int update(T object, T sample) {
        if (object == null) {
            return 0;
        }
        ArrayList<String> args = new ArrayList<String>();
        String where = SQLHelper.getWhere(object.getClass(), sample, args, null, mDatabaseSpec);
        return update(object, where, args.toArray(new String[args.size()]));
    }

    @Override
    public <T> int update(T bean, String where, String[] whereArgs) {
        if (bean == null) {
            return 0;
        }
        int count = count(bean.getClass(), where, whereArgs);
        if (whereArgs != null) {
            for (String arg : whereArgs) {
                where = where.replaceFirst("\\?", String.format("'%s'", arg));
            }
        }
        String sqlStatement = SQLHelper.buildUpdateStatement(bean, where);
        String[] statements = sqlStatement.split(SQLHelper.STATEMENT_SEPARATOR);
        executeTransactions(Arrays.asList(statements));
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
        String where = SQLHelper.getWhere(sample.getClass(), sample, args, null, mDatabaseSpec);
        String[] argsArray = args.toArray(new String[args.size()]);
        return delete(sample.getClass(), where, argsArray);
    }

    @Override
    public <T> int delete(Class<T> theClass, String where, String[] whereArgs) {
        return delete(theClass, where, whereArgs, false);
    }

    @Override
    public <T> int delete(Class<T> theClass, String where, String[] whereArgs, boolean onCascade) {
        DatabaseSpec.Relationship relationship = mDatabaseSpec.getRelationship(theClass);
        if (!relationship.equals(DatabaseSpec.Relationship.UNKNOWN)) {
            Field idField = SQLHelper.getPrimaryKeyField(theClass);
            idField.setAccessible(true);
            switch (relationship) {
                case HAS_MANY:
                    if (onCascade) {
                        HasMany hasMany = mDatabaseSpec.has(theClass);
                        List<T> toDelete = findAll(theClass, where, whereArgs);
                        for (T object : toDelete) {
                            try {
                                Object objectId = idField.get(object);
                                Class<?> containedClass = hasMany.getContainedClass();
                                String whereForeign = String.format("%s = '%s'", hasMany.getForeignKey(), String.valueOf(objectId));
                                delete(containedClass, whereForeign, null);
                            } catch (IllegalAccessException ignored) {
                            }
                        }
                    }
                    break;
                case MANY_TO_MANY:
                    List<ManyToMany> manyToManyList = mDatabaseSpec.getManyToMany(theClass);
                    for (ManyToMany manyToMany : manyToManyList) {
                        String foreignKey;
                        String foreignCurrentKey;
                        Class<?> relationTable;
                        if (manyToMany.getFirstRelation() == theClass) {
                            foreignKey = manyToMany.getMainKey();
                            foreignCurrentKey = manyToMany.getSecondaryKey();
                            relationTable = manyToMany.getSecondRelation();
                        } else {
                            foreignKey = manyToMany.getSecondaryKey();
                            foreignCurrentKey = manyToMany.getMainKey();
                            relationTable = manyToMany.getFirstRelation();
                        }
                        List<T> toRemove = findAll(theClass, where, whereArgs);
                        for (T object : toRemove) {
                            try {
                                Object objectId = idField.get(object);
                                String whereForeign = String.format("%s = '%s'", foreignKey, String.valueOf(objectId));

                                List<String> ids = new ArrayList<String>();
                                if (onCascade) {
                                    Cursor deletionCursor = mDbHelper.getDatabase().query(manyToMany.getTableName(), null, whereForeign, null, null, null, null);
                                    if (deletionCursor.moveToFirst()) {
                                        do {
                                            int index = deletionCursor.getColumnIndex(foreignCurrentKey);
                                            ids.add(deletionCursor.getString(index));
                                        } while (deletionCursor.moveToNext());
                                    }
                                    deletionCursor.close();
                                }

                                mDbHelper.getDatabase().delete(manyToMany.getTableName(), whereForeign, null);

                                for (String id : ids) {
                                    String whereRest = String.format("%s = '%s'", foreignCurrentKey, id);
                                    Cursor cursorRest = mDbHelper.getDatabase().query(manyToMany.getTableName(), null, whereRest, null, null, null, null);
                                    // this means there is no other relation with this object, so we can delete it on cascade :)
                                    if (cursorRest.getCount() == 0) {
                                        mDbHelper.getDatabase().delete(SQLHelper.getTableName(relationTable), SQLHelper._ID + " = ?", new String[]{id});
                                    }
                                }
                            } catch (IllegalAccessException ignored) {
                            }
                        }
                    }
                    break;
            }
        }

        return mDbHelper.getDatabase().delete(SQLHelper.getTableName(theClass), where, whereArgs);
    }

    @Override
    public void truncate(Class<?>... classes) {
        for (Class<?> theClass : classes) {
            String tableName = SQLHelper.getTableName(theClass);
            mDbHelper.getDatabase().delete(tableName, null, null);
            mDbHelper.getDatabase().delete("sqlite_sequence", "name LIKE ?", new String[]{tableName});
        }
    }

    @Override
    public <T> int count(T bean) {
        Cursor query = SQLHelper.getCursorFindAllWhere(mDbHelper.getDatabase(), bean.getClass(), bean, null, null, mDatabaseSpec);
        int count = query.getCount();
        query.close();
        return count;
    }

    @Override
    public <T> int count(Class<T> clazz, String where, String[] whereArgs) {
        Cursor query = mDbHelper.getDatabase().query(SQLHelper.getTableName(clazz), null, where, whereArgs, null, null, null);
        int count = query.getCount();
        query.close();
        return count;
    }

    @Override
    public <T> int count(Class<T> clazz) {
        Cursor query = SQLHelper.getCursorFindAllWhere(mDbHelper.getDatabase(), clazz, null, null, null, mDatabaseSpec);
        int count = query.getCount();
        query.close();
        return count;
    }

    private synchronized void executeTransactions(List<String> transactions) {
        SQLiteDatabase database = mDbHelper.getDatabase();
        boolean activeTransaction = false;
        try {
            database.execSQL("BEGIN TRANSACTION;");
            activeTransaction = true;
        } catch (Exception e) {
            PersistenceLogManager.e(TAG, "Could not initiate transaction", e);
        }

        // try to execute the statements and commit if, and only if,
        // the BEGIN TRANSACTION; was successful
        if (activeTransaction) {
            for (String transaction : transactions) {
                try {
                    database.execSQL(transaction);
                } catch (Exception e) {
                    PersistenceLogManager.e(TAG, "Error executing transaction: " + transaction, e);
                }
            }
            try {
                database.execSQL("COMMIT;");
            } catch (Exception e) {
                // we are doomed; yes, we are. there was an active transaction and yet
                // transaction could not be committed.
                PersistenceLogManager.e(TAG, "Could not commit transaction", e);
            }
        }
    }

    private <T, G> List<T> findAll(Class<T> clazz, T where, G attachedTo, Constraint constraint) {
        Cursor query = SQLHelper.getCursorFindAllWhere(mDbHelper.getDatabase(), clazz, where, attachedTo, constraint, mDatabaseSpec);
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

    private <T> Cursor getCursorFindAllWhere(Class<? extends T> clazz, String where, String[] args) {
        return mDbHelper.getDatabase().query(SQLHelper.getTableName(clazz), null, where, args, null, null, null, null);
    }

    private <T, G> String getSqlStatement(T bean, Node tree, G attachedTo) {
        String updateStatement = getUpdateStatementIfPossible(bean);
        if (!TextUtils.isEmpty(updateStatement)) {
            return updateStatement;
        }
        String mainInsertStatement = SQLHelper.getInsertStatement(bean, attachedTo, mDatabaseSpec);
        try {
            mainInsertStatement += getSqlInsertForChildrenOf(bean, tree);
        } catch (IllegalAccessException ignored) {
        }
        return mainInsertStatement;
    }

    private <T> String getUpdateStatementIfPossible(T bean) {
        // try to find the bean by id and if it exists, do not insert it, update it
        Class<T> theClass = (Class<T>) bean.getClass();
        // get its ID and make sure primary key is not null
        Field theId = SQLHelper.getPrimaryKeyField(theClass);
        theId.setAccessible(true);
        if (theId.getType() == String.class ||
                theId.getType() == Float.class ||
                theId.getType() == Double.class) {
            Object idValue = null;
            try {
                idValue = theId.get(bean);
            } catch (IllegalAccessException ignored) {
            }
            if (idValue == null) {
                throw new IllegalStateException("You cannot insert an object whose primary key is null and it is not int or long");
            }
        }
        String result = null;
        try {
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
                        result = SQLHelper.STATEMENT_SEPARATOR;
                    } else {
                        // update the bean using the just created sample
                        result = SQLHelper.buildUpdateStatement(bean, match, mDatabaseSpec);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private <T> String getSqlInsertForChildrenOf(T bean, Node tree) throws IllegalAccessException {// bodom
        // get a list with the fields that are lists
        Class<?> theClass = bean.getClass();
        Field[] fields = SQLHelper.getDeclaredFields(theClass);
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
            switch (mDatabaseSpec.getRelationship(theClass, collectionClass)) {
                case MANY_TO_MANY: {
                    List list = (List) field.get(bean);
                    if (list != null) {
                        for (Object object : list) {
                            // get the insertion SQL
                            String partialSqlStatement = getSqlStatement(object, tree, null);
                            if (partialSqlStatement != null) {
                                sqlStatement += partialSqlStatement;
                            }
                            // insert items in the joined table
                            // get the table name and columns
                            String relationTableName = ManyToMany.buildTableName(theClass, collectionClass);
                            String mainForeignKey = SQLHelper.getTableName(theClass) + "_id";
                            String secondaryForeignKey = SQLHelper.getTableName(collectionClass) + "_id";

                            // get the value for the main bean ID
                            Object beanId;
                            if (mDatabaseSpec.isAutoincrement(theClass)) {
                                beanId = String.format(SQLHelper.SELECT_AUTOINCREMENT_FORMAT, SQLHelper.getTableName(theClass));
                            } else {
                                Field mainId = SQLHelper.getPrimaryKeyField(theClass);
                                mainId.setAccessible(true);
                                beanId = mainId.get(bean);
                            }

                            // get the value for the secondary bean ID
                            Object secondaryId;
                            if (mDatabaseSpec.isAutoincrement(collectionClass)) {
                                secondaryId = String.format(SQLHelper.SELECT_AUTOINCREMENT_FORMAT, SQLHelper.getTableName(collectionClass));
                            } else {
                                Field secondaryIdField = SQLHelper.getPrimaryKeyField(collectionClass);
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
                        }
                    }
                    break;
                }
                case HAS_MANY:
                    List list = (List) field.get(bean);
                    if (list == null) {
                        break;
                    }
                    for (Object object : list) {
                        // prepare the object by setting the foreign value
                        String partialSqlStatement = getSqlStatement(object, tree, bean);
                        if (partialSqlStatement != null) {
                            sqlStatement += partialSqlStatement;
                        }
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
        Field[] fields = SQLHelper.getDeclaredFields(theClass);
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
                    switch (mDatabaseSpec.getRelationship(theClass, collectionClass)) {
                        case MANY_TO_MANY: {
                            Field collectionId = SQLHelper.getPrimaryKeyField(collectionClass);
                            // build a query that uses the joining table and the joined object
                            String collectionTableName = SQLHelper.getTableName(collectionClass);
                            String sql = new StringBuilder().append("SELECT * FROM ")
                                    .append(SQLHelper.getTableName(collectionClass))
                                    .append(" WHERE ")
                                    .append(SQLHelper.getIdColumn(collectionId))
                                    .append(" IN (SELECT ")
                                    .append(collectionTableName)
                                    .append(SQLHelper._ID)
                                    .append(" FROM ")
                                    .append(ManyToMany.buildTableName(theClass, collectionClass))
                                    .append(" WHERE ")
                                    .append(SQLHelper.getTableName(theClass))
                                    .append(SQLHelper._ID)
                                    .append(" = ?)").toString();
                            // execute the query
                            String[] selectionArgs = new String[1];
                            long id = query.getLong(query.getColumnIndex(SQLHelper._ID));
                            selectionArgs[0] = String.valueOf(id);
                            Cursor join = mDbHelper.getDatabase().rawQuery(sql, selectionArgs);
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
                            HasMany belongsTo = mDatabaseSpec.belongsTo(collectionClass);
                            Field throughField = belongsTo.getThroughField();
                            Object foreignValue = getValueFromCursor(throughField.getType(), belongsTo.getThroughColumnName(), query);
                            if (foreignValue != null) {
                                String sql = new StringBuilder().append("SELECT * FROM ")
                                        .append(SQLHelper.getTableName(collectionClass))
                                        .append(" WHERE ")
                                        .append(belongsTo.getForeignKey())
                                        .append(" = '")
                                        .append(foreignValue)
                                        .append("'").toString();
                                // execute the query and set the result to the current field
                                Cursor join = mDbHelper.getDatabase().rawQuery(sql, null);
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
        try {
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
            } else if (type == byte[].class || type == Byte[].class) {
                value = query.getBlob(columnIndex);
            }
            return value;
        } catch (Exception e) {
            throw new IllegalStateException("Error getting column " + name, e);
        }
    }

}