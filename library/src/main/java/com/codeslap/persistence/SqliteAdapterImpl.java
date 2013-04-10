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
import java.util.*;

import static com.codeslap.persistence.DataObjectFactory.getDataObject;
import static com.codeslap.persistence.StrUtil.concat;

/**
 * This is a persistence adapter that uses sqlite database as persistence engine. This is useful to
 * persist collections of beans. To save single objects (objects that don't get repeated,
 * singletons, or any data that don't fit into the tables paradigm), use PreferencesAdapter.
 */
public class SqliteAdapterImpl implements SqlAdapter {

  private static final String TAG = "sqliteImpl";
  private final SqliteDb mDbHelper;

  SqliteAdapterImpl(Context context, String name, String specId) {
    DatabaseSpec dbSpec = PersistenceConfig.getDatabaseSpec(specId);
    mDbHelper = SqliteDb.getInstance(context, name, dbSpec);
  }

  @Override
  public <T> T findFirst(T sample) {
    Class<T> clazz = (Class<T>) sample.getClass();
    ArrayList<String> args = new ArrayList<String>();
    String where = SQLHelper.getWhere(clazz, sample, args, null);
    Cursor query = mDbHelper.getDatabase().query(getDataObject(clazz).getTableName(), null, where,
        args.toArray(new String[args.size()]), null, null, null, "1");
    return findFirstFromCursor(clazz, query);
  }

  @Override
  public <T> T findFirst(Class<T> clazz, String where, String[] whereArgs) {
    DataObject<T> dataObject = getDataObject(clazz);
    Cursor query = mDbHelper.getDatabase()
        .query(dataObject.getTableName(), null, where, whereArgs, null, null, null, "1");
    return findFirstFromCursor(clazz, query);
  }

  @Override
  public <T> List<T> findAll(Class<T> theClass) {
    T emptySample = null;
    try {
      DataObject<T> dataObject = getDataObject(theClass);
      emptySample = dataObject.newInstance();
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
  public <T, Parent> List<T> findAll(T where, Parent parent) {
    return findAll(getDataObject((Class<T>) where.getClass()), where, parent, null);
  }

  @Override
  public <T> List<T> findAll(Class<T> clazz, String where, String[] whereArgs) {
    Cursor query = getCursorFindAllWhere(clazz, where, whereArgs);
    return findAllFromCursor(getDataObject(clazz), query);
  }

  @Override
  public <T> List<T> findAll(T where, Constraint constraint) {
    Class<T> clazz = (Class<T>) where.getClass();
    return findAll(getDataObject(clazz), where, null, constraint);
  }

  @Override
  public <T> Object store(T bean) {
    if (bean == null) {
      return null;
    }
    Class<T> theClass = (Class<T>) bean.getClass();
    List<String> transactions = new ArrayList<String>();
    String sqlStatement = getSqlStatement(bean, classesTree(theClass), null);
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

    // if it is autoincrement, we will try to populate the id field with the inserted id
    DataObject<T> dataObject = getDataObject(theClass);
    String primaryKeyFieldName = dataObject.getPrimaryKeyFieldName();
    if (dataObject.hasAutoincrement()) {
      Cursor lastId = mDbHelper.getDatabase()
          .query("sqlite_sequence", new String[]{"seq"}, "name = ?",
              new String[]{dataObject.getTableName()}, null, null, null);
      if (lastId != null && lastId.moveToFirst()) {
        long id = lastId.getLong(0);
        lastId.close();
        try {
          dataObject.set(primaryKeyFieldName, bean, id);
        } catch (Exception pokemon) {
          pokemon.printStackTrace();
        }
        return id;
      } else if (lastId != null) {
        lastId.close();
      }
    } else {
      try {
        return dataObject.get(primaryKeyFieldName, bean);
      } catch (Exception pokemon) {
        pokemon.printStackTrace();
      }
    }
    return null;
  }

  @Override
  public <T> void storeCollection(List<T> collection, ProgressListener listener) {
    storeCollection(collection, null, listener);
  }

  @Override
  public <T, Parent> void storeCollection(List<T> collection, Parent parent,
                                          ProgressListener listener) {
    if (listener != null) {
      listener.onProgressChange(0);
    }
    if (collection.isEmpty()) {
      return;
    }
    List<String> transactions = new ArrayList<String>();
    DataObject<?> collectionDataObject = getDataObject(collection.get(0).getClass());
    // TODO revisit this. it should use the optimized insert statement
    boolean hasRelations = !collectionDataObject.manyToMany().isEmpty() || !collectionDataObject
        .hasMany().isEmpty();
    // if there is no listener, parent object, collection is too small or objects in the list have inner
    // relationships: insert them in a normal way, in which there will be a sql execution per object
    if (listener != null || parent != null || collection.size() <= 1 || hasRelations) {
      int progress;
      int all = collection.size() + 1; // 1 == commit phase
      for (int i = 0, collectionSize = collection.size(); i < collectionSize; i++) {
        T object = collection.get(i);
        Set tree = classesTree(object.getClass());
        String sqlStatement = getSqlStatement(object, tree, parent);
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
          builder.append(SQLHelper.getFastInsertSqlHeader(bean));
        } else {
          builder.append(SQLHelper.getUnionInsertSql(bean));
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
        DataObject<T> dataObject = getDataObject(theClass);

        List<T> allStored = findAll(theClass);
        for (T stored : allStored) {
          boolean contained = false;
          for (T object : collection) {
            Object storedId = dataObject.get(dataObject.getPrimaryKeyFieldName(), stored);
            Object storedObject = dataObject.get(dataObject.getPrimaryKeyFieldName(), object);
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
    String where = SQLHelper.getWhere(object.getClass(), sample, args, null);
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
        where = where.replaceFirst("\\?", concat(SQLHelper.QUOTE, arg, SQLHelper.QUOTE));
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
    String where = SQLHelper.getWhere(sample.getClass(), sample, args, null);
    String[] argsArray = args.toArray(new String[args.size()]);
    return delete(sample.getClass(), where, argsArray);
  }

  @Override
  public <T> int delete(Class<T> theClass, String where, String[] whereArgs) {
    return delete(theClass, where, whereArgs, false);
  }

  @Override
  public <T> int delete(Class<T> theClass, String where, String[] whereArgs, boolean onCascade) {
    DataObject<T> dataObject = getDataObject(theClass);
    Collection<HasManySpec> hasManySpecs = dataObject.hasMany();
    String pkFieldName = dataObject.getPrimaryKeyFieldName();

    if (onCascade) {
      List<T> toDelete = findAll(theClass, where, whereArgs);
      for (T object : toDelete) {
        for (HasManySpec hasManySpec : hasManySpecs) {
          Object objectId = dataObject.get(pkFieldName, object);
          String whereForeign = concat(hasManySpec.getThroughColumnName(), " = '",
              String.valueOf(objectId), SQLHelper.QUOTE);
          delete(hasManySpec.contained, whereForeign, null);
        }
      }
    }

    for (ManyToManySpec manyToMany : dataObject.manyToMany()) {
      String foreignKey;
      String foreignCurrentKey;
      Class<?> relationTable;
      if (manyToMany.getFirstRelation().getObjectClass() == theClass) {
        foreignKey = manyToMany.getMainKey();
        foreignCurrentKey = manyToMany.getSecondaryKey();
        relationTable = manyToMany.getSecondRelation().getObjectClass();
      } else {
        foreignKey = manyToMany.getSecondaryKey();
        foreignCurrentKey = manyToMany.getMainKey();
        relationTable = manyToMany.getFirstRelation().getObjectClass();
      }
      List<T> toRemove = findAll(theClass, where, whereArgs);
      for (T object : toRemove) {
        Object objectId = dataObject.get(pkFieldName, object);
        String whereForeign = concat(foreignKey, " = '", String.valueOf(objectId), SQLHelper.QUOTE);

        List<String> ids = new ArrayList<String>();
        if (onCascade) {
          Cursor deletionCursor = mDbHelper.getDatabase()
              .query(manyToMany.getTableName(), null, whereForeign, null, null, null, null);
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
          String whereRest = concat(foreignCurrentKey, " = '", id, SQLHelper.QUOTE);
          Cursor cursorRest = mDbHelper.getDatabase()
              .query(manyToMany.getTableName(), null, whereRest, null, null, null, null);
          // this means there is no other relation with this object, so we can delete it on cascade :)
          if (cursorRest.getCount() == 0) {
            DataObject<?> relationTableDataObject = getDataObject(relationTable);
            mDbHelper.getDatabase()
                .delete(relationTableDataObject.getTableName(), SQLHelper._ID + " = ?",
                    new String[]{id});
          }
        }
      }
    }
    return mDbHelper.getDatabase().delete(dataObject.getTableName(), where, whereArgs);
  }

  @Override
  public void truncate(Class<?>... classes) {
    for (Class<?> theClass : classes) {
      String tableName = getDataObject(theClass).getTableName();
      mDbHelper.getDatabase().delete(tableName, null, null);
      mDbHelper.getDatabase().delete("sqlite_sequence", "name LIKE ?", new String[]{tableName});
    }
  }

  @Override
  public <T> int count(T bean) {
    Cursor query = SQLHelper
        .getCursorFindAllWhere(mDbHelper.getDatabase(), bean.getClass(), bean, null, null);
    int count = query.getCount();
    query.close();
    return count;
  }

  @Override
  public <T> int count(Class<T> type, String where, String[] whereArgs) {
    DataObject dataObject = getDataObject(type);
    Cursor query = mDbHelper.getDatabase()
        .query(dataObject.getTableName(), null, where, whereArgs, null, null, null);
    int count = query.getCount();
    query.close();
    return count;
  }

  @Override
  public <T> int count(Class<T> clazz) {
    Cursor query = SQLHelper
        .getCursorFindAllWhere(mDbHelper.getDatabase(), clazz, null, null, null);
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

  private <T, Parent> List<T> findAll(DataObject<T> dataObject, T where, Parent parent,
                                      Constraint constraint) {
    Cursor query = SQLHelper
        .getCursorFindAllWhere(mDbHelper.getDatabase(), dataObject.getObjectClass(), where, parent,
            constraint);
    return findAllFromCursor(dataObject, query);
  }

  private <T> List<T> findAllFromCursor(DataObject<T> dataObject, Cursor query) {
    List<T> beans = new ArrayList<T>();
    if (query.moveToFirst()) {
      do {
        T bean = dataObject
            .getBeanFromCursor(query, classesTree(dataObject.getObjectClass()), mDbHelper);
        beans.add(bean);
      } while (query.moveToNext());
    }
    query.close();
    return beans;
  }

  private <T> Cursor getCursorFindAllWhere(Class<? extends T> type, String where, String[] args) {
    DataObject<? extends T> dataObject = getDataObject(type);
    return mDbHelper.getDatabase()
        .query(dataObject.getTableName(), null, where, args, null, null, null, null);
  }

  private <T, Parent> String getSqlStatement(T bean, Set tree, Parent parent) {
    String updateStatement = getUpdateStatementIfPossible(bean);
    if (!TextUtils.isEmpty(updateStatement)) {
      return updateStatement;
    }
    String mainInsertStatement = SQLHelper.getInsertStatement(bean, parent);
    try {
      mainInsertStatement += getSqlInsertForChildrenOf(bean, tree);
    } catch (IllegalAccessException ignored) {
    }
    return mainInsertStatement;
  }

  private <T> String getUpdateStatementIfPossible(T bean) {
    // try to find the bean by id and if it exists, do not insert it, update it
    Class<T> theClass = (Class<T>) bean.getClass();
    DataObject<T> dataObject = getDataObject(theClass);
    // get its ID and make sure primary key is not null
    String pkFieldName = dataObject.getPrimaryKeyFieldName();
    if (!dataObject.hasAutoincrement() && !dataObject.hasData(pkFieldName, bean)) {
      throw new IllegalStateException(
          "You cannot insert an object whose primary key is null and it is not autoincrementable");
    }

    String result = null;
    try {
      if (dataObject.hasData(pkFieldName, bean)) {
        // create an object of the same type of the bean with the same id to search of it
        T sample = dataObject.newInstance();
        Object beanId = dataObject.get(pkFieldName, bean);
        dataObject.set(pkFieldName, sample, beanId);

        Object match = findFirst(sample);
        if (match != null) {
          // if they are the same, do nothing...
          if (bean.equals(match)) {
            result = SQLHelper.STATEMENT_SEPARATOR;
          } else {
            // update the bean using the just created sample
            result = SQLHelper.buildUpdateStatement(bean, match);
          }
        }
      }
    } catch (Exception ignored) {
    }
    return result;
  }

  private <T> String getSqlInsertForChildrenOf(T bean, Set tree) throws IllegalAccessException {
    String sqlStatement = getSqlInsertForHasManyRelations(bean, tree);

    // get a list with the fields that are lists
    Class<T> theClass = (Class<T>) bean.getClass();
    DataObject<T> dataObject = getDataObject(theClass);

    Collection<ManyToManySpec> manyToManySpecs = dataObject.manyToMany();
    for (ManyToManySpec manyToManySpec : manyToManySpecs) {
      if (!tree.add(manyToManySpec.getSecondRelation().getObjectClass())) {
        continue;
      }

      DataObject<?> firstRelation = manyToManySpec.getFirstRelation();
      if (firstRelation.getObjectClass() != theClass) {
        continue;
      }

      String firstRelationField = manyToManySpec.getFirstRelationFieldName();
      Object o = dataObject.get(firstRelationField, bean);
      List list = (List) o;
      if (list == null) {
        break;
      }
      for (Object object : list) {
        if (object == null) {
          continue;
        }

        // get the insertion SQL
        String partialSqlStatement = getSqlStatement(object, tree, null);
        if (partialSqlStatement != null) {
          sqlStatement += partialSqlStatement;
        }

        String insertStatement = getManyToManyInsertStatement(bean, object);
        sqlStatement += insertStatement;
      }
    }
    return sqlStatement;
  }

  private <Foo, Bar> String getManyToManyInsertStatement(Foo foo,
                                                         Bar bar) throws IllegalAccessException {
    Class<Foo> theClass = (Class<Foo>) foo.getClass();
    Class<Bar> collectionClass = (Class<Bar>) bar.getClass();
    DataObject<Foo> dataObject = getDataObject(theClass);
    DataObject<Bar> collectionDataObject = getDataObject(collectionClass);

    ManyToManySpec manyToMany = null;
    String mainKey = null, secondaryKey = null;
    for (Iterator<ManyToManySpec> iterator = dataObject.manyToMany().iterator(); iterator
        .hasNext(); ) {
      manyToMany = iterator.next();
      if (manyToMany.getFirstRelation().getObjectClass() == theClass && manyToMany
          .getSecondRelation().getObjectClass() == collectionClass) {
        mainKey = manyToMany.getMainKey();
        secondaryKey = manyToMany.getSecondaryKey();
        break;
      } else if (manyToMany.getSecondRelation().getObjectClass() == theClass && manyToMany
          .getFirstRelation().getObjectClass() == collectionClass) {
        secondaryKey = manyToMany.getMainKey();
        mainKey = manyToMany.getSecondaryKey();
        break;
      }
    }

    if (manyToMany == null || mainKey == null || secondaryKey == null) {
      throw new IllegalStateException(
          "Could not find many-to-many relation keys for " + foo + " and " + bar);
    }

    String mainTableName = dataObject.getTableName();
    String secondaryTableName = collectionDataObject.getTableName();

    // get the value for the main foo ID
    Object beanId;
    if (dataObject.hasAutoincrement()) {
      beanId = getSelectSeqSqlite(mainTableName);
    } else {
      beanId = dataObject.get(dataObject.getPrimaryKeyFieldName(), foo);
    }

    // get the value for the secondary foo ID
    Object secondaryId;
    if (collectionDataObject.hasAutoincrement()) {
      secondaryId = getSelectSeqSqlite(secondaryTableName);
    } else {
      secondaryId = collectionDataObject.get(collectionDataObject.getPrimaryKeyFieldName(), bar);
    }

    // build the sql statement for the insertion of the many-to-many relation
    String relationTableName = manyToMany.getTableName();
    String hack = concat("CASE WHEN (SELECT COUNT(*) FROM ", relationTableName, " WHERE ", mainKey,
        " = ", String.valueOf(beanId), " AND ", secondaryKey, " = ", String.valueOf(secondaryId),
        ") == 0 THEN ", String.valueOf(beanId), " ELSE NULL END");
    return concat("INSERT OR IGNORE INTO ", relationTableName, " (", mainKey, ", ", secondaryKey,
        ") VALUES (", hack, ", ", String.valueOf(secondaryId), ");", SQLHelper.STATEMENT_SEPARATOR);
  }

  private static String getSelectSeqSqlite(String tableName) {
    return concat("(SELECT seq FROM sqlite_sequence WHERE name = '", tableName, "')");
  }

  private <T> String getSqlInsertForHasManyRelations(T bean,
                                                     Set tree) throws IllegalAccessException {
    Class<T> type = (Class<T>) bean.getClass();
    DataObject<T> dataObject1 = getDataObject(type);

    StringBuilder sqlStatement = new StringBuilder();
    for (HasManySpec hasManySpec : dataObject1.hasMany()) {
      List list = (List) hasManySpec.listField.get(bean);
      if (list == null) {
        break;
      }
      for (Object object : list) {
        // prepare the object by setting the foreign value
        String partialSqlStatement = getSqlStatement(object, tree, bean);
        if (partialSqlStatement != null) {
          sqlStatement.append(partialSqlStatement);
        }
      }
    }
    return sqlStatement.toString();
  }

  private <T> T findFirstFromCursor(Class<T> clazz, Cursor query) {
    if (query.moveToFirst()) {
      T bean = getDataObject(clazz).getBeanFromCursor(query, classesTree(clazz), mDbHelper);
      query.close();
      return bean;
    }
    query.close();
    return null;
  }

  static Comparator<Class<?>> CLASS_COMPARATOR = new Comparator<Class<?>>() {
    @Override public int compare(Class<?> foo, Class<?> bar) {
      return foo.getName().compareToIgnoreCase(bar.getName());
    }
  };

  private static Set<Class<?>> classesTree(Class<?> type) {
    TreeSet<Class<?>> tree = new TreeSet<Class<?>>(CLASS_COMPARATOR);
    tree.add(type);
    return tree;
  }
}