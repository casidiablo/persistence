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

package com.codeslap.hongo;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.codeslap.hongo.DataObjectFactory.getDataObject;

/**
 * Data object that gets object information using reflection.
 *
 * @author cristian
 */
public class ReflectDataObject implements DataObject<Object> {

  private final ObjectType objectType;
  private final Set<Class<?>> graph;
  private final Map<String, ColumnField> fields;
  private final boolean hasAutoincrement;
  private final String tableName;
  private final Collection hasManyList;
  private final Collection<ManyToManySpec> manyToManyList;
  private final ObjectType<?> belongsTo;
  private final String primaryKeyName;
  private final ColumnField primaryKeyField;

  public ReflectDataObject(ObjectType type) {
    this(type, new TreeSet<Class<?>>(SqliteAdapterImpl.CLASS_COMPARATOR));
  }

  ReflectDataObject(ObjectType type, Set<Class<?>> graph) {
    objectType = type;
    this.graph = graph;
    fields = new HashMap<String, ColumnField>();
    hasManyList = Collections.synchronizedList(new ArrayList<HasManySpec>());
    manyToManyList = Collections.synchronizedList(new ArrayList<ManyToManySpec>());

    PrimaryKey primaryKey = null;
    String primaryKeyName = null;
    ColumnField primaryKeyField = null;

    ColumnField[] declaredFields = objectType.getDeclaredFields();
    for (ColumnField columnField : declaredFields) {
      if (columnField.isAnnotationPresent(Ignore.class) ||
          columnField.isStatic() ||// ignore static fields
          columnField.isFinal()) { // ignore final fields
        continue;
      }
      fields.put(columnField.getName(), columnField);

      PrimaryKey pk = primaryKey == null ? columnField.getAnnotation(PrimaryKey.class) : null;
      if (pk != null) {
        Class<?> fieldType = columnField.getType();
        if (pk.autoincrement() && fieldType != long.class && fieldType != Long.class &&
            fieldType != int.class && fieldType != Integer.class) {
          throw new RuntimeException("Only long and int can be used with autoincrement = true: "
              + objectType.getSimpleName());
        }
        primaryKey = pk;
        primaryKeyName = columnField.getName();
        primaryKeyField = columnField;
      }
    }

    if (primaryKey == null) {
      throw new IllegalArgumentException(
          "Primay keys are mandatory: " + objectType.getSimpleName());
    }
    this.primaryKeyName = primaryKeyName;
    this.primaryKeyField = primaryKeyField;

    Belongs annotation = (Belongs) objectType.getAnnotation(Belongs.class);
    belongsTo = annotation != null ? new ReflectObjectType(annotation.to()) : null;

    hasAutoincrement = primaryKey.autoincrement();
    tableName = objectType.getTableName();
  }

  @Override
  public Object newInstance() {
    return objectType.newInstance();
  }

  @Override
  public boolean hasAutoincrement() {
    return hasAutoincrement;
  }

  @Override
  public Collection<HasManySpec> hasMany() {
    synchronized (hasManyList) {
      if (hasManyList.isEmpty()) {
        hasManyList.addAll(ClassAnalyzer.getHasManySpecs(objectType, fields.values()));
      }
    }
    return hasManyList;
  }

  @Override
  public Collection<ManyToManySpec> manyToMany() {
    synchronized (manyToManyList) {
      if (manyToManyList.isEmpty()) {
        manyToManyList.addAll(
            ClassAnalyzer.getManyToManySpecs(this, objectType, graph, fields.values()));
      }
    }
    return manyToManyList;
  }

  @Override public HasManySpec hasMany(ObjectType childObjectType) {
    for (HasManySpec hasManySpec : hasMany()) {
      if (hasManySpec.contained.equals(childObjectType)) {
        return hasManySpec;
      }
    }
    throw new IllegalArgumentException(
        "Cannot find has-many relation between " + objectType + " and " + childObjectType);
  }

  @Override
  public ObjectType<?> belongsTo() {
    return belongsTo;
  }

  @Override
  public ObjectType getObjectType() {
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
    ObjectType<?> containerClass = belongsTo();
    if (containerClass != null) {
      DataObject<?> containerDataObject =
          DataObjectFactory.getDataObject(containerClass.getObjectClass());
      HasManySpec hasManySpec = containerDataObject.hasMany(objectType);
      // add a new field to the table creation statement to create the relation
      // TODO is it really necessary to mark this field as "not null"?
      String columnName = hasManySpec.getThroughColumnName();
      SqliteType sqlType =
          containerDataObject.getTypeFrom(containerDataObject.getPrimaryKeyFieldName());
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
        ObjectType<?> collectionType = columnField.getGenericType();
        if (tree.add(collectionType.getObjectClass())) {
          value =
              processInnerCollection(query, tree, getDataObject(collectionType.getObjectClass()),
                  dbHelper);
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
            StrUtil.concat("An error occurred setting value to ", columnField, ", ", value, ": ",
                e.getMessage()));
      }
    }
    return bean;
  }

  private <Child> List<Child> processInnerCollection(Cursor query, Set<Class<?>> tree,
      DataObject<Child> collectionDataObject, SqliteDb dbHelper) {
    List<Child> value = getListFromHasMany(query, tree, collectionDataObject, dbHelper);
    if (value != null) {
      return value;
    }
    return getListFromManyToMany(query, tree, collectionDataObject, dbHelper);
  }

  private <Child> List<Child> getListFromHasMany(Cursor query, Set<Class<?>> tree,
      DataObject<Child> collectionDataObject, SqliteDb dbHelper) {
    ObjectType<?> belongsTo = collectionDataObject.belongsTo();
    if (belongsTo == null || !belongsTo.equals(objectType)) {
      return null;
    }
    ObjectType<Child> collectionClass = collectionDataObject.getObjectType();
    for (HasManySpec hasManySpec : hasMany()) {
      if (hasManySpec.contained.getObjectClass().equals(collectionClass)) {
        continue;
      }
      // build a query that uses the joining table and the joined object
      ColumnField pkField = fields.get(getPrimaryKeyFieldName());
      Object foreignValue =
          getValueFromCursor(long.class /* TODO test this*/, ColumnHelper.getIdColumn(pkField) /* this is not like this all the time*/,
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
      DataObject<Child> collectionDataObject, SqliteDb dbHelper) {
    ObjectType<?> collectionType = collectionDataObject.getObjectType();
    boolean manyToMany = false;
    ManyToManySpec currentManyToMany = null;
    for (ManyToManySpec manyToManySpec : manyToMany()) {
      currentManyToMany = manyToManySpec;
      if (currentManyToMany.getFirstRelation().equals(collectionType)
          || currentManyToMany.getSecondRelation().equals(collectionType)) {
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
        .append(ColumnHelper.getIdColumn(primaryKeyField))
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
      } else if (type == float.class
          || type == Float.class
          || type == double.class
          || type == Double.class) {
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

  @Override public <Parent> String getWhere(Object bean, List<String> args, Parent parent) {
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
            conditions.add(StrUtil.concat(columnName, " LIKE '", cleanedValue, SQLHelper.QUOTE));
          } else if (columnField.getType() == Boolean.class
              || columnField.getType() == boolean.class) {
            int intValue = (Boolean) value ? 1 : 0;
            conditions.add(StrUtil.concat(columnName, " = '", intValue, SQLHelper.QUOTE));
          } else {
            conditions.add(StrUtil.concat(columnName, " = '", value, SQLHelper.QUOTE));
          }
        } else {
          if (columnField.getType() == String.class) {
            conditions.add(StrUtil.concat(columnName, " LIKE ?"));
          } else {
            conditions.add(StrUtil.concat(columnName, " = ?"));
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
      Object foreignValue = getPrimaryKeyValue(parent);
      if (foreignValue != null) {
        if (args == null) {
          conditions.add(
              StrUtil.concat(hasManySpec.getThroughColumnName(), " = '", foreignValue.toString(),
                  SQLHelper.QUOTE));
        } else {
          conditions.add(StrUtil.concat(hasManySpec.getThroughColumnName(), " = ?"));
          args.add(foreignValue.toString());
        }
      }
    }
    return StrUtil.join(conditions, " AND ");
  }

  @Override
  public <Parent> void populateColumnsAndValues(Object bean, Parent parent, List<String> values,
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
          values.add(StrUtil.concat("X'", hex, SQLHelper.QUOTE));
        }
      } else if (value == null) {
        Column columnAnnotation = columnField.getAnnotation(Column.class);
        boolean hasDefault = false;
        if (columnAnnotation != null) {
          hasDefault = !columnAnnotation.defaultValue().equals(Column.NULL);
        }
        if (columnAnnotation != null && columnAnnotation.notNull() && !hasDefault) {
          String msg = StrUtil.concat("Field ", columnField.getName(), " from class ",
              objectType.getSimpleName(),
              " cannot be null. It was marked with the @Column not null annotation and it has not a default value");
          throw new IllegalStateException(msg);
        }
        if (hasDefault) {
          values.add(StrUtil.concat(SQLHelper.QUOTE, columnAnnotation.defaultValue()
              .replace(String.valueOf(SQLHelper.QUOTE), SQLHelper.DOUBLE_QUOTE), SQLHelper.QUOTE));
        } else {
          values.add("NULL");
        }
      } else {
        values.add(StrUtil.concat(SQLHelper.QUOTE,
            String.valueOf(value).replace(String.valueOf(SQLHelper.QUOTE), SQLHelper.DOUBLE_QUOTE),
            SQLHelper.QUOTE));
      }
    }
    if (parent != null) {
      HasManySpec hasManySpec = getHasManySpec(parent);
      Object foreignValue = getPrimaryKeyValue(parent);

      if (columns != null) {
        columns.add(hasManySpec.getThroughColumnName());
      }
      if (values != null) {
        Class<Parent> parentClass = (Class<Parent>) parent.getClass();
        DataObject<Parent> parentDataObject = getDataObject(parentClass);
        String pkFieldName = parentDataObject.getPrimaryKeyFieldName();
        if (foreignValue != null && parentDataObject.hasData(pkFieldName, parent)) {
          values.add(String.valueOf(foreignValue));
        } else {
          String tableName = parentDataObject.getTableName();
          values.add(
              StrUtil.concat("(SELECT seq FROM sqlite_sequence WHERE name = '", tableName, "')"));
        }
      }
    }
  }

  private static <T> Object getPrimaryKeyValue(T parent) {
    Object foreignValue = null;
    try {
      DataObject<T> dataObjectParent = (DataObject<T>) getDataObject(parent.getClass());

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
    ObjectType<Parent> containerClass = (ObjectType<Parent>) belongsTo();
    Class<Parent> parentClass = (Class<Parent>) parent.getClass();
    if (containerClass.getObjectClass() != parentClass) {
      throw new IllegalArgumentException(
          "Cannot find has-many relation between " + containerClass + "and" + objectType);
    }
    DataObject<Parent> containerDataObject = getDataObject(parentClass);
    return containerDataObject.hasMany(objectType);
  }
}
