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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static com.codeslap.persistence.StrUtil.concat;

/**
 * Data object that gets object information using reflection.
 *
 * @author cristian
 */
public class ReflectDataObject implements DataObject<Object> {

  private final Class<?> objectType;
  private final List<Field> fields;
  private final boolean hasAutoincrement;
  private final String tableName;

  public ReflectDataObject(Class<?> type, DatabaseSpec spec) {
    objectType = type;
    fields = new ArrayList<Field>();

    boolean autoincrement = true;
    for (Field field : objectType.getDeclaredFields()) {
      if (!field.isAnnotationPresent(Ignore.class) &&
          !Modifier.isStatic(field.getModifiers()) &&// ignore static fields
          !Modifier.isFinal(field.getModifiers())) {// ignore final fields
        fields.add(field);
      }

      PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
      if (primaryKey != null) {
        if (field.getType() == String.class ||
            field.getType() == Boolean.class || field.getType() == boolean.class ||
            field.getType() == Float.class || field.getType() == float.class ||
            field.getType() == Double.class || field.getType() == double.class) {
          autoincrement = false;
        } else {
          autoincrement = primaryKey.autoincrement();
        }
      }
    }
    hasAutoincrement = spec != null ? autoincrement && !spec.isNotAutoincrement(type) : autoincrement;
    tableName = SQLHelper.getTableName(objectType);
  }

  @Override public Object newInstance() {
    try {
      return objectType.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      error(e);
    }
    return null;
  }

  @Override public boolean hasAutoincrement() {
    return hasAutoincrement;
  }

  @Override public String getCreateTableSentence(DatabaseSpec databaseSpec) {
    CreateTableHelper createTable = CreateTableHelper.init(tableName);
    for (Field field : fields) {
      String columnName = ReflectHelper.getColumnName(field);
      CreateTableHelper.Type type = getTypeFrom(field);
      if (ReflectHelper.isPrimaryKey(field)) {
        String column = ReflectHelper.getIdColumn(field);
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
    HasMany belongsTo = databaseSpec.belongsTo(objectType);
    if (belongsTo != null) {
      // if so, add a new field to the table creation statement to create the relation
      Class<?> containerClass = belongsTo.getContainerClass();
      Field field = belongsTo.getThroughField();

      String containerClassNormalized = SQLHelper.normalize(containerClass.getSimpleName());
      String throughFieldNormalized = SQLHelper.normalize(belongsTo.getThroughField().getName());
      String columnName = concat(containerClassNormalized, "_", throughFieldNormalized);

      createTable.add(columnName, getTypeFrom(field), false);
    }
    return createTable.build();
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
    throw cause instanceof RuntimeException ? (RuntimeException) cause : new RuntimeException(cause);
  }
}
