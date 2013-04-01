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
import java.util.*;

import static com.codeslap.persistence.StrUtil.concat;

/**
 * Data object that gets object information using reflection.
 *
 * @author cristian
 */
public class ReflectDataObject implements DataObject<Object> {

  static final String PRIMARY_KEY = " INTEGER PRIMARY KEY";
  private final Class<?> objectType;
  private final List<Field> fields;

  public ReflectDataObject(Class<?> type) {
    objectType = type;
    fields = new ArrayList<Field>();
    for (Field field : objectType.getDeclaredFields()) {
      if (!field.isAnnotationPresent(Ignore.class) &&
          !Modifier.isStatic(field.getModifiers()) &&// ignore static fields
          !Modifier.isFinal(field.getModifiers())) {// ignore final fields
        fields.add(field);
      }
    }
  }

  @Override public Object newInstance() {
    try {
      return objectType.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      error(e);
    }
    return null;
  }

  @Override public String getCreateTableSentence(DatabaseSpec databaseSpec) {
    List<String> fieldSentences = new ArrayList<String>();
    // loop through all the fields and add sql statements
    List<String> columns = new ArrayList<String>();
    for (Field field : fields) {
      String columnName = ReflectHelper.getColumnName(field);
      if (ReflectHelper.isPrimaryKey(field)) {
        String primaryKeySentence = getCreatePrimaryKey(field);
        if (field.getType() == String.class) {// what types are supported
          primaryKeySentence = primaryKeySentence.replace("INTEGER PRIMARY KEY", "TEXT PRIMARY KEY");
        } else if (databaseSpec.isAutoincrement(objectType)) {
          primaryKeySentence += " AUTOINCREMENT";
        }
        if (!columns.contains(columnName)) {
          fieldSentences.add(primaryKeySentence);
          columns.add(columnName);
        }
      } else if (field.getType() != List.class) {
        if (!columns.contains(columnName)) {
          columns.add(columnName);
          boolean notNull = false;
          Column columnAnnotation = field.getAnnotation(Column.class);
          if (columnAnnotation != null) {
            notNull = columnAnnotation.notNull();
          }
          fieldSentences.add(getFieldSentence(columnName, field.getType(), notNull));
        }
      }
    }

    // check whether this class belongs to a has-many relation, in which case we need to create an additional field
    HasMany belongsTo = databaseSpec.belongsTo(objectType);
    if (belongsTo != null) {
      // if so, add a new field to the table creation statement to create the relation
      Class<?> containerClass = belongsTo.getContainerClass();
      Field field = belongsTo.getThroughField();
      String columnName = concat(SQLHelper.normalize(containerClass.getSimpleName()), "_", SQLHelper.normalize(belongsTo.getThroughField().getName()));
      if (!columns.contains(columnName)) {
        fieldSentences.add(getFieldSentence(columnName, field.getType(), true));
        columns.add(ReflectHelper.getColumnName(field));
      }
    }

    // sort sentences
    Collections.sort(fieldSentences, new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        if (s1.contains(PRIMARY_KEY)) {
          return -1;
        }
        if (s2.contains(PRIMARY_KEY)) {
          return 1;
        }
        return 0;
      }
    });

    // build create table sentence
    StringBuilder builder = new StringBuilder();
    builder.append("CREATE TABLE IF NOT EXISTS ").append(SQLHelper.getTableName(objectType)).append(" (");
    boolean first = true;
    for (String fieldSentence : fieldSentences) {
      if (!first) {
        builder.append(", ");
      }
      builder.append(fieldSentence);
      first = false;
    }
    builder.append(");");
    return builder.toString();
  }

  private static String getCreatePrimaryKey(Field field) {
    return concat(ReflectHelper.getIdColumn(field), PRIMARY_KEY);
  }

  /**
   * @param name    the name of the field
   * @param type    the type
   * @param notNull true if the column should be not null
   * @return the sql statement to create that kind of field
   */
  private static String getFieldSentence(String name, Class<?> type, boolean notNull) {
    name = SQLHelper.normalize(name);
    String notNullSentence = "";
    if (notNull) {
      notNullSentence = " NOT NULL";
    }
    if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
      return concat(name, " INTEGER", notNullSentence);
    }
    if (type == boolean.class || type == Boolean.class) {
      return concat(name, " BOOLEAN", notNullSentence);
    }
    if (type == float.class || type == Float.class || type == double.class || type == Double.class) {
      return concat(name, " REAL", notNullSentence);
    }
    if (type == byte[].class || type == Byte[].class) {
      return concat(name, " BLOB", notNullSentence);
    }
    return concat(name, " TEXT", notNullSentence);
  }

  private void error(Exception e) {
    Throwable cause = e.getCause();
    throw cause instanceof RuntimeException ? (RuntimeException) cause : new RuntimeException(cause);
  }
}
