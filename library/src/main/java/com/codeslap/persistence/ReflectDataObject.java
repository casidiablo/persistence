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

import android.database.Cursor;
import android.text.TextUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import static com.codeslap.persistence.DataObjectFactory.getDataObject;
import static com.codeslap.persistence.StrUtil.concat;

/**
 * Data object that gets object information using reflection.
 *
 * @author cristian
 */
public class ReflectDataObject implements DataObject<Object> {

  private final Class<?> objectType;
  private final Map<String, ColumnField> fields;
  private final boolean hasAutoincrement;
  private final String tableName;
  private final Collection<HasManySpec> hasManyList;
  private final Collection<ManyToManySpec> manyToManyList;
  private final Class<?> belongsTo;
  private final String primaryKeyName;
  private final Field primaryKeyField;

  public ReflectDataObject(Class<?> type) {
    this(type, new TreeSet<Class<?>>(SqliteAdapterImpl.CLASS_COMPARATOR));
  }

  ReflectDataObject(Class<?> type, Set<Class<?>> graph) {
    objectType = type;
    fields = new HashMap<String, ColumnField>();
    hasManyList = new ArrayList<HasManySpec>();
    manyToManyList = new ArrayList<ManyToManySpec>();

    PrimaryKey primaryKey = null;
    String primaryKeyName = null;
    Field primaryKeyField = null;
    for (Field field : objectType.getDeclaredFields()) {
      if (field.isAnnotationPresent(Ignore.class) ||
          Modifier.isStatic(field.getModifiers()) ||// ignore static fields
          Modifier.isFinal(field.getModifiers())) { // ignore final fields
        continue;
      }
      fields.put(field.getName(), new ReflectColumnField(field));
      field.setAccessible(true);

      PrimaryKey pk = primaryKey == null ? field.getAnnotation(PrimaryKey.class) : null;
      Class<?> fieldType = field.getType();
      if (pk != null) {
        if (pk.autoincrement() && fieldType != long.class && fieldType != Long.class &&
            fieldType != int.class && fieldType != Integer.class) {
          throw new RuntimeException(
              "Only long and int can be used with autoincrement = true: " + objectType.getSimpleName());
        }
        primaryKey = pk;
        primaryKeyName = field.getName();
        primaryKeyField = field;
      }

      if (fieldType == List.class) {
        searchForManyToMany(graph, field);
      }
    }

    if (primaryKey == null) {
      throw new IllegalArgumentException(
          "Primay keys are mandatory: " + objectType.getSimpleName());
    }
    this.primaryKeyName = primaryKeyName;
    this.primaryKeyField = primaryKeyField;

    Belongs annotation = objectType.getAnnotation(Belongs.class);
    belongsTo = annotation != null ? annotation.to() : null;

    hasAutoincrement = primaryKey.autoincrement();
    tableName = getTableName(objectType);
  }

  @Override
  public Object newInstance() {
    try {
      return objectType.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      error(e);
    }
    return null;
  }

  @Override
  public boolean hasAutoincrement() {
    return hasAutoincrement;
  }

  @Override
  public Collection<HasManySpec> hasMany() {
    synchronized (hasManyList) {
      if (hasManyList.isEmpty()) {
        for (ColumnField columnField : fields.values()) {
          searchForHasMany(columnField);
        }
      }
    }
    return hasManyList;
  }

  @Override
  public Collection<ManyToManySpec> manyToMany() {
    return manyToManyList;
  }

  @Override
  public <Child> HasManySpec hasMany(Class<Child> childClass) {
    for (HasManySpec hasManySpec : hasMany()) {
      if (hasManySpec.contained == childClass) {
        return hasManySpec;
      }
    }
    throw new IllegalArgumentException(
        "Cannot find has-many relation between " + objectType + " and " + childClass);
  }

  @Override
  public Class<?> belongsTo() {
    return belongsTo;
  }

  @Override
  public Class getObjectClass() {
    return objectType;
  }

  @Override
  public String getTableName() {
    return tableName;
  }

  @Override
  public String getPrimaryKeyFieldName() {
    return primaryKeyName;
  }

  @Override
  public boolean hasData(String fieldName, Object bean) {
    Object value = getField(fieldName).get(bean);
    Class<?> type = fields.get(fieldName).getType();
    if (type == long.class || type == Long.class) {
      return value != null && ((Long) value) != 0L;
    }
    if (type == int.class || type == Integer.class) {
      return value != null && ((Integer) value) != 0;
    }
    if (type == float.class || type == Float.class) {
      return value != null && ((Float) value) != 0.0;
    }
    if (type == double.class || type == Double.class) {
      return value != null && ((Double) value) != 0.0;
    }
    if (type == boolean.class || type == Boolean.class) {
      if (value instanceof Boolean) {
        return (Boolean) value;
      }
      if (value instanceof Integer) {
        return ((Integer) value) != 0;
      }
      return false;
    }
    if (type == byte[].class || type == Byte[].class) {
      return value != null && ((byte[]) value).length > 0;
    }
    return value != null;
  }

  @Override
  public String getCreateTableSentence() {
    CreateTableHelper createTable = CreateTableHelper.init(tableName);
    for (ColumnField columnField : fields.values()) {
      String columnName = ColumnHelper.getColumnName(columnField);
      SqliteType type = getTypeFrom(columnField);
      if (ColumnHelper.isPrimaryKey(columnField)) {
        String column = ColumnHelper.getIdColumn(columnField);
        createTable.addPk(column, type, hasAutoincrement);
      } else if (columnField.getType() != List.class) {
        boolean notNull = false;
        Column columnAnnotation = columnField.getAnnotation(Column.class);
        if (columnAnnotation != null) {
          notNull = columnAnnotation.notNull();
        }
        createTable.add(columnName, type, notNull);
      }
    }

    // check whether this class belongs to a has-many relation,
    // in which case we need to create an additional field
    Class<?> containerClass = belongsTo();
    if (containerClass != null) {
      DataObject<?> containerDataObject = DataObjectFactory.getDataObject(containerClass);
      HasManySpec hasManySpec = containerDataObject.hasMany(objectType);
      // add a new field to the table creation statement to create the relation
      // TODO is it really necessary to mark this field as "not null"?
      String columnName = hasManySpec.getThroughColumnName();
      SqliteType sqlType = containerDataObject.getTypeFrom(
          containerDataObject.getPrimaryKeyFieldName());
      createTable.add(columnName, sqlType, false);
    }
    return createTable.build();
  }

  @Override
  public Object getBeanFromCursor(Cursor query, Set<Class<?>> tree, SqliteDb dbHelper) {
    Object bean;
    try {
      bean = newInstance();
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not initialize object of type " + objectType + ", " + e.getMessage());
    }

    // get each field and put its value in a content values object
    for (ColumnField columnField : fields.values()) {
      // get the column index
      String normalize = ColumnHelper.getColumnName(columnField);
      int columnIndex = query.getColumnIndex(normalize);
      // get an object value depending on the type
      Class type = columnField.getType();
      Object value = null;
      if (columnIndex == -1 && type == List.class) {
        Class<?> collectionClass = columnField.getGenericType();
        if (tree.add(collectionClass)) {
          value = processInnerCollection(query, tree, getDataObject(collectionClass), dbHelper);
        }
      } else {// do not process collections here
        value = getValueFromCursor(type, ColumnHelper.getColumnName(columnField), query);
      }
      try {
        if (value != null) {
          columnField.set(bean, value);
        }
      } catch (Exception e) {
        throw new RuntimeException(
            concat("An error occurred setting value to ", columnField, ", ", value, ": ",
                e.getMessage()));
      }
    }
    return bean;
  }

  private void searchForManyToMany(Set<Class<?>> graph, Field field) {
    ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
    if (manyToMany != null && !graph.contains(objectType)) {
      ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
      Class<?> collectionClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];

      boolean relationExists = false;
      ManyToMany manyToManyColl;
      for (Field collField : collectionClass.getDeclaredFields()) {
        manyToManyColl = collField.getAnnotation(ManyToMany.class);
        if (manyToManyColl != null && collField.getType() == List.class) {
          ParameterizedType parameterizedTypeColl = (ParameterizedType) collField.getGenericType();
          Class<?> selfClass = (Class<?>) parameterizedTypeColl.getActualTypeArguments()[0];
          if (selfClass == objectType) {
            relationExists = true;
            break;
          }
        }
      }

      if (!relationExists) {
        throw new IllegalStateException(
            "When defining a ManyToMany relation both classes must use the ManyToMany annotation");
      }

      if (graph.add(objectType)) {
        ReflectDataObject collDataObject = new ReflectDataObject(collectionClass, graph);
        ManyToManySpec manyToManySpec = new ManyToManySpec(this, field.getName(), collDataObject);
        manyToManyList.add(manyToManySpec);
      }
    }
  }

  private void searchForHasMany(ColumnField columnField) {
    HasMany hasMany = columnField.getAnnotation(HasMany.class);
    if (hasMany == null) {
      return;
    }
    Class<?> collectionClass = columnField.getGenericType();

    if (!collectionClass.isAnnotationPresent(Belongs.class)) {
      throw new IllegalStateException(
          "When defining a HasMany relation you must specify a Belongs annotation in the child class");
    }
    Belongs belongs = collectionClass.getAnnotation(Belongs.class);
    if (belongs.to() != objectType) {
      throw new IllegalStateException(
          "Belongs class points to " + belongs.to() + " but should point to " + objectType);
    }

    Belongs thisBelongsTo = objectType.getAnnotation(Belongs.class);
    if (thisBelongsTo != null && thisBelongsTo.to() == collectionClass) {
      throw new IllegalStateException(
          "Cyclic has-many relations not supported. Use many-to-many instead: " +
              collectionClass.getSimpleName() + " belongs to " + objectType.getSimpleName() + " and viceversa");
    }

    HasManySpec hasManySpec = new HasManySpec(objectType, columnField.getName(), collectionClass);
    hasManyList.add(hasManySpec);
  }

  private <Child> List<Child> processInnerCollection(Cursor query, Set<Class<?>> tree,
                                                     DataObject<Child> collectionDataObject,
                                                     SqliteDb dbHelper) {
    List<Child> value = getListFromHasMany(query, tree, collectionDataObject, dbHelper);
    if (value != null) {
      return value;
    }
    return getListFromManyToMany(query, tree, collectionDataObject, dbHelper);
  }

  private <Child> List<Child> getListFromHasMany(Cursor query, Set<Class<?>> tree,
                                                 DataObject<Child> collectionDataObject,
                                                 SqliteDb dbHelper) {
    Class<?> belongsTo = collectionDataObject.belongsTo();
    if (belongsTo != objectType) {
      return null;
    }
    Class<Child> collectionClass = collectionDataObject.getObjectClass();
    for (HasManySpec hasManySpec : hasMany()) {
      if (hasManySpec.contained != collectionClass) {
        continue;
      }
      // build a query that uses the joining table and the joined object
      Object foreignValue = getValueFromCursor(long.class /* TODO test this*/,
          ColumnHelper.getIdColumn(new ReflectColumnField(SQLHelper.getPrimaryKeyField(objectType))) /* this is not like this all the time*/,
          query);
      if (foreignValue != null) {
        String sql = new StringBuilder().append("SELECT * FROM ")
            .append(collectionDataObject.getTableName())
            .append(" WHERE ")
            .append(hasManySpec.getThroughColumnName())
            .append(" = '")
            .append(foreignValue)
            .append(SQLHelper.QUOTE)
            .toString();
        // execute the query and set the result to the current field
        Cursor join = dbHelper.getDatabase().rawQuery(sql, null);
        List<Child> listValue = new ArrayList<Child>();
        if (join.moveToFirst()) {
          do {
            Child beanFromCursor = collectionDataObject.getBeanFromCursor(join, tree, dbHelper);
            listValue.add(beanFromCursor);
          } while (join.moveToNext());
        }
        join.close();
        return listValue;
      }
    }
    return null;
  }

  private <Child> List<Child> getListFromManyToMany(Cursor query, Set tree,
                                                    DataObject<Child> collectionDataObject,
                                                    SqliteDb dbHelper) {
    Class<?> collectionClass = collectionDataObject.getObjectClass();
    boolean manyToMany = false;
    ManyToManySpec currentManyToMany = null;
    for (ManyToManySpec manyToManySpec : manyToMany()) {
      currentManyToMany = manyToManySpec;
      if (currentManyToMany.getFirstRelation()
          .getObjectClass() == collectionClass || currentManyToMany.getSecondRelation()
          .getObjectClass() == collectionClass) {
        manyToMany = true;
        break;
      }
    }

    if (!manyToMany) {
      return null;
    }

    // build a query that uses the joining table and the joined object
    String sql = new StringBuilder().append("SELECT * FROM ")
        .append(collectionDataObject.getTableName())
        .append(" WHERE ")
        .append(ColumnHelper.getIdColumn(new ReflectColumnField(primaryKeyField)))
        .append(" IN (SELECT ")
        .append(currentManyToMany.getSecondaryKey())
        .append(" FROM ")
        .append(currentManyToMany.getTableName())
        .append(" WHERE ")
        .append(currentManyToMany.getMainKey())
        .append(" = ?)")
        .toString();
    // execute the query
    String[] selectionArgs = new String[1];
    long id = query.getLong(query.getColumnIndex(SQLHelper._ID));
    selectionArgs[0] = String.valueOf(id);
    Cursor join = dbHelper.getDatabase().rawQuery(sql, selectionArgs);
    // build a list based on the cursor result
    List<Child> listValue = new ArrayList<Child>();
    if (join.moveToFirst()) {
      do {
        Child beanFromCursor = collectionDataObject.getBeanFromCursor(join, tree, dbHelper);
        listValue.add(beanFromCursor);
      } while (join.moveToNext());
    }
    join.close();
    return listValue;
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

  @Override public SqliteType getTypeFrom(String fieldName) {
    return getTypeFrom(fields.get(fieldName));
  }

  @Override public Collection<ColumnField> getDeclaredFields() {
    return fields.values();
  }

  @Override public ColumnField getField(String name) {
    if (!fields.containsKey(name)) {
      throw new IllegalStateException("Cannot find field " + name + " in " + objectType);
    }
    return fields.get(name);
  }

  private static SqliteType getTypeFrom(ColumnField field) {
    Class<?> type = field.getType();
    if (type == int.class || type == Integer.class || type == long.class || type == Long.class ||
        type == boolean.class || type == Boolean.class) {
      return SqliteType.INTEGER;
    } else if (type == float.class || type == Float.class || type == double.class ||
        type == Double.class) {
      return SqliteType.REAL;
    } else if (type == byte[].class || type == Byte[].class) {
      return SqliteType.BLOB;
    }
    return SqliteType.TEXT;
  }

  private void error(Exception e) {
    Throwable cause = e.getCause();
    throw cause instanceof RuntimeException ? (RuntimeException) cause : new RuntimeException(
        cause);
  }

  private static String getTableName(Class<?> theClass) {
    Table table = theClass.getAnnotation(Table.class);
    String tableName;
    if (table != null) {
      tableName = table.value();
      if (TextUtils.isEmpty(tableName)) {
        String msg = concat("You cannot leave a table name empty: class ",
            theClass.getSimpleName());
        throw new IllegalArgumentException(msg);
      }
      if (tableName.contains(" ")) {
        String msg = concat("Table name cannot have spaces: '", tableName, "'; found in class ",
            theClass.getSimpleName());
        throw new IllegalArgumentException(msg);
      }
    } else {
      String name = theClass.getSimpleName();
      if (name.endsWith("y")) {
        name = name.substring(0, name.length() - 1) + "ies";
      } else if (!name.endsWith("s")) {
        name += "s";
      }
      tableName = SQLHelper.normalize(name);
    }
    return tableName;
  }

  @Override
  public <Parent> String getWhere(Object bean, List<String> args, Parent parent) {
    List<String> conditions = new ArrayList<String>();
    if (bean != null) {
      for (ColumnField columnField : fields.values()) {
        Class<?> type = columnField.getType();
        if (type == byte[].class || type == Byte[].class || type == List.class) {
          continue;
        }
        if (!hasData(columnField.getName(), bean)) {
          continue;
        }
        Object value = columnField.get(bean);
        String columnName = ColumnHelper.getColumnName(columnField);
        if (args == null) {
          if (columnField.getType() == String.class) {
            String cleanedValue = String.valueOf(value)
                .replace(String.valueOf(SQLHelper.QUOTE), SQLHelper.DOUBLE_QUOTE);
            conditions.add(concat(columnName, " LIKE '", cleanedValue, SQLHelper.QUOTE));
          } else if (columnField.getType() == Boolean.class || columnField.getType() == boolean.class) {
            int intValue = (Boolean) value ? 1 : 0;
            conditions.add(concat(columnName, " = '", intValue, SQLHelper.QUOTE));
          } else {
            conditions.add(concat(columnName, " = '", value, SQLHelper.QUOTE));
          }
        } else {
          if (columnField.getType() == String.class) {
            conditions.add(concat(columnName, " LIKE ?"));
          } else {
            conditions.add(concat(columnName, " = ?"));
          }
          if (columnField.getType() == Boolean.class || columnField.getType() == boolean.class) {
            value = (Boolean) value ? 1 : 0;
          }
          args.add(String.valueOf(value));
        }
      }
    }

    // if there is an attachment
    if (parent != null) {
      HasManySpec hasManySpec = getHasManySpec(parent);
      Object foreignValue = getRelationValueFromParent(parent);
      if (foreignValue != null) {
        if (args == null) {
          conditions.add(concat(hasManySpec.getThroughColumnName(), " = '", foreignValue.toString(),
              SQLHelper.QUOTE));
        } else {
          conditions.add(concat(hasManySpec.getThroughColumnName(), " = ?"));
          args.add(foreignValue.toString());
        }
      }
    }
    return StrUtil.join(conditions, " AND ");
  }

  @Override public <Parent> void populateColumnsAndValues(Object bean, Parent parent,
                                                          List<String> values,
                                                          List<String> columns) {
    if (bean == null) {
      return;
    }
    for (ColumnField columnField : fields.values()) {
      // if the class has an autoincrement, ignore the ID
      if (ColumnHelper.isPrimaryKey(columnField) && hasAutoincrement()) {
        continue;
      }
      Class<?> type = columnField.getType();
      if (type == List.class) {
        continue;
      }
      Object value = columnField.get(bean);
      if (columns != null) {
        columns.add(ColumnHelper.getColumnName(columnField));
      }
      if (values == null) {
        continue;
      }
      if (columnField.getType() == Boolean.class || columnField.getType() == boolean.class) {
        int intValue = (Boolean) value ? 1 : 0;
        values.add(String.valueOf(intValue));
      } else if (columnField.getType() == Byte[].class || columnField.getType() == byte[].class) {
        if (value == null) {
          values.add("NULL");
        } else {
          String hex = getHex((byte[]) value);
          values.add(concat("X'", hex, SQLHelper.QUOTE));
        }
      } else if (value == null) {
        Column columnAnnotation = columnField.getAnnotation(Column.class);
        boolean hasDefault = false;
        if (columnAnnotation != null) {
          hasDefault = !columnAnnotation.defaultValue().equals(Column.NULL);
        }
        if (columnAnnotation != null && columnAnnotation.notNull() && !hasDefault) {
          String msg = concat("Field ", columnField.getName(), " from class ",
              objectType.getSimpleName(),
              " cannot be null. It was marked with the @Column not null annotation and it has not a default value");
          throw new IllegalStateException(msg);
        }
        if (hasDefault) {
          values.add(concat(SQLHelper.QUOTE, columnAnnotation.defaultValue()
              .replace(String.valueOf(SQLHelper.QUOTE), SQLHelper.DOUBLE_QUOTE), SQLHelper.QUOTE));
        } else {
          values.add("NULL");
        }
      } else {
        values.add(concat(SQLHelper.QUOTE,
            String.valueOf(value).replace(String.valueOf(SQLHelper.QUOTE), SQLHelper.DOUBLE_QUOTE),
            SQLHelper.QUOTE));
      }
    }
    if (parent != null) {
      HasManySpec hasManySpec = getHasManySpec(parent);
      Object foreignValue = getRelationValueFromParent(parent);

      if (columns != null) {
        columns.add(hasManySpec.getThroughColumnName());
      }
      if (values != null) {
        if (foreignValue != null && SQLHelper.hasData(foreignValue.getClass(), foreignValue)) {
          values.add(String.valueOf(foreignValue));
        } else {
          DataObject<?> parentDataObject = getDataObject(parent.getClass());
          String tableName = parentDataObject.getTableName();
          values.add(concat("(SELECT seq FROM sqlite_sequence WHERE name = '", tableName, "')"));
        }
      }
    }
  }

  // TODO rename this method
  private static <Parent> Object getRelationValueFromParent(Parent parent) {
    Object foreignValue = null;
    try {
      DataObject<Parent> dataObjectParent = getDataObject((Class<Parent>) parent.getClass());

      String parentPKeyName = dataObjectParent.getPrimaryKeyFieldName();
      ColumnField field = dataObjectParent.getField(parentPKeyName);
      foreignValue = field.get(parent);
    } catch (Exception ignored) {
    }
    return foreignValue;
  }

  private static String getHex(byte[] raw) {
    if (raw == null) {
      return null;
    }
    final StringBuilder hex = new StringBuilder(2 * raw.length);
    for (final byte b : raw) {
      hex.append(SQLHelper.HEXES.charAt((b & 0xF0) >> 4))
          .append(SQLHelper.HEXES.charAt((b & 0x0F)));
    }
    return hex.toString();
  }

  <Parent> HasManySpec getHasManySpec(Parent parent) {
    Class<Parent> containerClass = (Class<Parent>) belongsTo();
    if (containerClass != parent.getClass()) {
      throw new IllegalArgumentException(
          "Cannot find has-many relation between " + containerClass + "and" + objectType);
    }
    DataObject<Parent> containerDataObject = getDataObject(containerClass);
    return containerDataObject.hasMany(objectType);
  }
}
