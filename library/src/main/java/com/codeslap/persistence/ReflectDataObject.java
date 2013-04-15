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
  private final Map<String, Field> fields;
  private final boolean hasAutoincrement;
  private final String tableName;
  private final Collection<HasManySpec> hasManyList = new ArrayList<HasManySpec>();
  private final Collection<ManyToManySpec> manyToManyList = new ArrayList<ManyToManySpec>();
  private final Class<?> belongsTo;
  private final String primaryKeyName;
  private final Field primaryKeyField;

  public ReflectDataObject(Class<?> type) {
    this(type, new TreeSet<Class<?>>(SqliteAdapterImpl.CLASS_COMPARATOR));
  }

  ReflectDataObject(Class<?> type, Set<Class<?>> graph) {
    objectType = type;
    fields = new HashMap<String, Field>();

    PrimaryKey primaryKey = null;
    String primaryKeyName = null;
    Field primaryKeyField = null;
    for (Field field : objectType.getDeclaredFields()) {
      if (field.isAnnotationPresent(Ignore.class) ||
          Modifier.isStatic(field.getModifiers()) ||// ignore static fields
          Modifier.isFinal(field.getModifiers())) { // ignore final fields
        continue;
      }
      fields.put(field.getName(), field);
      field.setAccessible(true);

      PrimaryKey pk = primaryKey == null ? field.getAnnotation(PrimaryKey.class) : null;
      Class<?> fieldType = field.getType();
      if (pk != null) {
        if (pk.autoincrement() && fieldType != long.class && fieldType != Long.class &&
            fieldType != int.class && fieldType != Integer.class) {
          throw new RuntimeException(
              "Only long and int can be used with autoincrement = true: " + objectType
                  .getSimpleName());
        }
        primaryKey = pk;
        primaryKeyName = field.getName();
        primaryKeyField = field;
      }

      if (fieldType == List.class) {
        searchForHasMany(field);
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
    return hasManyList;
  }

  @Override
  public Collection<ManyToManySpec> manyToMany() {
    return manyToManyList;
  }

  @Override
  public HasManySpec hasMany(Class<?> theClass) {
    for (HasManySpec hasManySpec : hasManyList) {
      if (hasManySpec.contained == theClass) {
        return hasManySpec;
      }
    }
    throw new IllegalArgumentException(
        "Cannot find has-many relation between " + objectType + "and" + theClass);
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
  public boolean set(String fieldName, Object target, Object value) {
    if (!fields.containsKey(fieldName)) {
      throw new IllegalStateException("Cannot find field " + fieldName + " in " + objectType);
    }
    try {
      fields.get(fieldName).set(target, value);
      return true;
    } catch (IllegalAccessException e) {
      return false;
    }
  }

  @Override
  public Object get(String fieldName, Object target) {
    if (!fields.containsKey(fieldName)) {
      throw new IllegalStateException("Cannot find field " + fieldName + " in " + objectType);
    }
    try {
      Field field = fields.get(fieldName);
      return field.get(target);
    } catch (IllegalAccessException e) {
      return null;
    }
  }

  @Override
  public boolean hasData(String fieldName, Object bean) {
    Object value = get(fieldName, bean);
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
    for (Field field : fields.values()) {
      ReflectColumnField columnField = new ReflectColumnField(field);
      String columnName = ColumnHelper.getColumnName(columnField);
      CreateTableHelper.Type type = getTypeFrom(field);
      if (ColumnHelper.isPrimaryKey(columnField)) {
        String column = ColumnHelper.getIdColumn(columnField);
        createTable.addPk(column, type, hasAutoincrement);
      } else if (field.getType() != List.class) {
        boolean notNull = false;
        Column columnAnnotation = field.getAnnotation(Column.class);
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
      for (HasManySpec hasManySpec : containerDataObject.hasMany()) {
        if (hasManySpec.contained != objectType) {
          continue;
        }
        // add a new field to the table creation statement to create the relation
        // TODO is it really necessary to mark this field as "not null"?
        String columnName = hasManySpec.getThroughColumnName();
        createTable.add(columnName, getTypeFrom(hasManySpec.throughField), false);
        break;
      }
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
    for (Field field : fields.values()) {
      // get the column index
      ReflectColumnField columnField = new ReflectColumnField(field);
      String normalize = ColumnHelper.getColumnName(columnField);
      int columnIndex = query.getColumnIndex(normalize);
      // get an object value depending on the type
      Class type = field.getType();
      Object value = null;
      if (columnIndex == -1 && type == List.class) {
        ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
        Class<?> collectionClass = (Class<?>) stringListType.getActualTypeArguments()[0];
        if (tree.add(collectionClass)) {
          value = processInnerCollection(query, tree, getDataObject(collectionClass), dbHelper);
        }
      } else {// do not process collections here
        value = getValueFromCursor(type, ColumnHelper.getColumnName(columnField),
            query);
      }
      try {
        if (value != null) {
          field.setAccessible(true);
          field.set(bean, value);
        }
      } catch (Exception e) {
        throw new RuntimeException(
            concat("An error occurred setting value to ", field, ", ", value, ": ",
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

  private void searchForHasMany(Field field) {
    HasMany hasMany = field.getAnnotation(HasMany.class);
    if (hasMany != null) {
      ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
      Class<?> collectionClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];

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
                collectionClass.getSimpleName() + " belongs to " + objectType
                .getSimpleName() + " and viceversa");
      }

      HasManySpec hasManySpec = new HasManySpec(objectType, field);
      hasManyList.add(hasManySpec);
    }
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
            .append(collectionDataObject.getTableName()).append(" WHERE ")
            .append(hasManySpec.getThroughColumnName()).append(" = '").append(foreignValue)
            .append(SQLHelper.QUOTE).toString();
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
        .append(collectionDataObject.getTableName()).append(" WHERE ")
        .append(ColumnHelper.getIdColumn(new ReflectColumnField(primaryKeyField)))
        .append(" IN (SELECT ").append(currentManyToMany.getSecondaryKey()).append(" FROM ")
        .append(currentManyToMany.getTableName()).append(" WHERE ")
        .append(currentManyToMany.getMainKey()).append(" = ?)").toString();
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

  private static CreateTableHelper.Type getTypeFrom(Field field) {
    Class<?> type = field.getType();
    if (type == int.class || type == Integer.class || type == long.class || type == Long.class ||
        type == boolean.class || type == Boolean.class) {
      return CreateTableHelper.Type.INTEGER;
    } else if (type == float.class || type == Float.class || type == double.class ||
        type == Double.class) {
      return CreateTableHelper.Type.REAL;
    } else if (type == byte[].class || type == Byte[].class) {
      return CreateTableHelper.Type.BLOB;
    }
    return CreateTableHelper.Type.TEXT;
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
      for (Field field : fields.values()) {
        Class<?> type = field.getType();
        if (type == byte[].class || type == Byte[].class || type == List.class) {
          continue;
        }
        try {
          if (!hasData(field.getName(), bean)) {
            continue;
          }
          Object value = field.get(bean);
          String columnName = ColumnHelper.getColumnName(new ReflectColumnField(field));
          if (args == null) {
            if (field.getType() == String.class) {
              String cleanedValue = String.valueOf(value)
                  .replace(String.valueOf(SQLHelper.QUOTE), SQLHelper.DOUBLE_QUOTE);
              conditions.add(concat(columnName, " LIKE '", cleanedValue, SQLHelper.QUOTE));
            } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
              int intValue = (Boolean) value ? 1 : 0;
              conditions.add(concat(columnName, " = '", intValue, SQLHelper.QUOTE));
            } else {
              conditions.add(concat(columnName, " = '", value, SQLHelper.QUOTE));
            }
          } else {
            if (field.getType() == String.class) {
              conditions.add(concat(columnName, " LIKE ?"));
            } else {
              conditions.add(concat(columnName, " = ?"));
            }
            if (field.getType() == Boolean.class || field.getType() == boolean.class) {
              value = (Boolean) value ? 1 : 0;
            }
            args.add(String.valueOf(value));
          }
        } catch (IllegalAccessException ignored) {
        }
      }
    }

    // if there is an attachment
    if (parent != null) {
      HasManySpec hasManySpec = SQLHelper.getHasManySpec(objectType, parent);
      Object foreignValue = getRelationValueFromParent(parent, hasManySpec);
      if (foreignValue != null) {
        if (args == null) {
          conditions.add(
              concat(hasManySpec.getThroughColumnName(), " = '", foreignValue.toString(), SQLHelper.QUOTE));
        } else {
          conditions.add(concat(hasManySpec.getThroughColumnName(), " = ?"));
          args.add(foreignValue.toString());
        }
      }
    }
    return StrUtil.join(conditions, " AND ");
  }

  private static <Parent> Object getRelationValueFromParent(Parent parent,
                                                            HasManySpec hasManySpec) {
    Object foreignValue = null;
    try {
      foreignValue = hasManySpec.throughField.get(parent);
    } catch (Exception ignored) {
    }
    return foreignValue;
  }
}
