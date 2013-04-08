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
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.codeslap.persistence.DataObjectFactory.getDataObject;
import static com.codeslap.persistence.StrUtil.concat;

public class SQLHelper {

  static final String ID = "id";
  static final String _ID = "_id";
  private static final String HEXES = "0123456789ABCDEF";

  private static final Map<Class<?>, String> INSERT_COLUMNS_CACHE = new HashMap<Class<?>, String>();
  private static final Map<Class<?>, String> TABLE_NAMES_CACHE = new HashMap<Class<?>, String>();
  private static final Map<Class<?>, Field[]> FIELDS_CACHE = new HashMap<Class<?>, Field[]>();

  static final String STATEMENT_SEPARATOR = "b05f72bb_STATEMENT_SEPARATOR";
  private static final char QUOTE = '\'';
  private static final String DOUBLE_QUOTE = "''";

  static Field[] getDeclaredFields(Class theClass) {
    if (!FIELDS_CACHE.containsKey(theClass)) {
      List<Field> list = new ArrayList<Field>();
      for (Field field : theClass.getDeclaredFields()) {
        // - If it has the ignore annotation, ignore it.
        // - Oh, really? What a brilliant idea.
        if (!field.isAnnotationPresent(Ignore.class) &&
            !Modifier.isStatic(field.getModifiers()) &&// ignore static fields
            !Modifier.isFinal(field.getModifiers())) {// ignore final fields
          list.add(field);
        }
      }
      FIELDS_CACHE.put(theClass, list.toArray(new Field[list.size()]));
    }
    return FIELDS_CACHE.get(theClass);
  }

  static <T, Parent> String getWhere(Class<?> theClass, T bean, List<String> args, Parent parent) {
    List<String> conditions = new ArrayList<String>();
    if (bean != null) {
      Class<?> clazz = bean.getClass();
      Field[] fields = getDeclaredFields(clazz);
      for (Field field : fields) {
        Class<?> type = field.getType();
        if (type == byte[].class || type == Byte[].class || type == List.class) {
          continue;
        }
        try {
          field.setAccessible(true);
          Object value = field.get(bean);
          if (!hasData(type, value)) {
            continue;
          }
          String columnName = ReflectHelper.getColumnName(field);
          if (args == null) {
            if (field.getType() == String.class) {
              String cleanedValue = String.valueOf(value)
                  .replace(String.valueOf(QUOTE), DOUBLE_QUOTE);
              conditions.add(concat(columnName, " LIKE '", cleanedValue, QUOTE));
            } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
              int intValue = (Boolean) value ? 1 : 0;
              conditions.add(concat(columnName, " = '", intValue, QUOTE));
            } else {
              conditions.add(concat(columnName, " = '", value, QUOTE));
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
      HasManySpec hasManySpec = getHasManySpec(theClass, parent);
      Object foreignValue = getRelationValueFromParent(parent, hasManySpec);
      if (foreignValue != null) {
        if (args == null) {
          conditions.add(
              concat(hasManySpec.getThroughColumnName(), " = '", foreignValue.toString(), QUOTE));
        } else {
          conditions.add(concat(hasManySpec.getThroughColumnName(), " = ?"));
          args.add(foreignValue.toString());
        }
      }
    }
    return join(conditions, " AND ");
  }

  private static <Parent> HasManySpec getHasManySpec(Class<?> theClass, Parent parent) {
    Class<Parent> containerClass = (Class<Parent>) getDataObject(theClass).belongsTo();
    if (containerClass != parent.getClass()) {
      throw new IllegalArgumentException(
          "Cannot find has-many relation between " + containerClass + "and" + theClass);
    }
    DataObject<Parent> containerDataObject = getDataObject(containerClass);
    return containerDataObject.hasMany(theClass);
  }

  private static <T> String getSet(T bean) {
    List<String> sets = new ArrayList<String>();
    if (bean != null) {
      Field[] fields = getDeclaredFields(bean.getClass());
      for (Field field : fields) {
        try {
          Class<?> type = field.getType();
          if (type == List.class) {
            continue;
          }
          field.setAccessible(true);
          Object value = field.get(bean);
          boolean isBoolean = field.getType() == Boolean.class || field.getType() == boolean.class;
          if (isBoolean || hasData(type, value)) {
            if (isBoolean) {
              int intValue = (Boolean) value ? 1 : 0;
              sets.add(concat(ReflectHelper.getColumnName(field), " = '", intValue, QUOTE));
            } else if (field.getType() == byte[].class || field.getType() == Byte[].class) {
              String hex = getHex((byte[]) value);
              sets.add(concat(ReflectHelper.getColumnName(field), " = X'", hex, QUOTE));
            } else {
              String cleanedVal = String.valueOf(value)
                  .replace(String.valueOf(QUOTE), DOUBLE_QUOTE);
              sets.add(concat(ReflectHelper.getColumnName(field), " = '", cleanedVal, QUOTE));
            }
          }
        } catch (IllegalAccessException ignored) {
        }
      }
    }

    return join(sets, ", ");
  }

  private static String join(List<String> sets, String glue) {
    StringBuilder builder = new StringBuilder();
    boolean glued = false;
    for (String condition : sets) {
      if (glued) {
        builder.append(glue);
      }
      builder.append(condition);
      glued = true;
    }
    return builder.toString();
  }

  static boolean hasData(Class<?> type, Object value) {
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

  /**
   * @param name string to normalize
   * @return converts a camel-case string into a lowercase, _ separated string
   */
  static String normalize(String name) {
    StringBuilder newName = new StringBuilder();
    newName.append(name.charAt(0));
    for (int i = 1; i < name.length(); i++) {
      if (Character.isUpperCase(name.charAt(i))) {
        newName.append("_");
      }
      newName.append(name.charAt(i));
    }
    return newName.toString().toLowerCase();
  }

  static <T> String buildUpdateStatement(T bean, Object sample) {
    String where = getWhere(bean.getClass(), sample, null, null);
    String set = getSet(bean);
    return concat("UPDATE ", getTableName(bean), " SET ", set, " WHERE ", where, ";",
        STATEMENT_SEPARATOR);
  }

  public static <T> String buildUpdateStatement(T bean, String where) {
    String set = getSet(bean);
    return concat("UPDATE ", getTableName(bean), " SET ", set, " WHERE ", where, ";",
        STATEMENT_SEPARATOR);
  }

  static <T, Parent> String getInsertStatement(T bean, Parent parent) {
    List<String> values = new ArrayList<String>();
    List<String> columns = null;
    if (!INSERT_COLUMNS_CACHE.containsKey(bean.getClass())) {
      columns = new ArrayList<String>();
    }
    populateColumnsAndValues(bean, parent, values, columns);

    String columnsSet;
    if (INSERT_COLUMNS_CACHE.containsKey(bean.getClass())) {
      columnsSet = INSERT_COLUMNS_CACHE.get(bean.getClass());
    } else {
      columnsSet = join(columns, ", ");
      INSERT_COLUMNS_CACHE.put(bean.getClass(), columnsSet);
    }

    // build insert statement for the main object
    String tableName = getTableName(bean);
    DataObject<?> dataObject = getDataObject(bean.getClass());
    if (values.size() == 0 && dataObject.hasAutoincrement()) {
      String hack = concat("(SELECT seq FROM sqlite_sequence WHERE name = '", tableName, "')+1");
      String idColumn = ReflectHelper.getIdColumn(getPrimaryKeyField(bean.getClass()));
      return concat("INSERT OR IGNORE INTO ", tableName, " (", idColumn, ") VALUES (", hack, ");",
          STATEMENT_SEPARATOR);
    }
    return concat("INSERT OR IGNORE INTO ", tableName, " (", columnsSet, ") VALUES (",
        join(values, ", "), ");", STATEMENT_SEPARATOR);
  }

  private static <T, Parent> void populateColumnsAndValues(T bean, Parent parent,
                                                           List<String> values,
                                                           List<String> columns) {
    if (bean == null) {
      return;
    }
    Class<?> theClass = bean.getClass();
    Field[] fields = getDeclaredFields(theClass);
    DataObject<?> dataObject = getDataObject(theClass);
    for (Field field : fields) {
      // if the class has an autoincrement, ignore the ID
      if (ReflectHelper.isPrimaryKey(field) && dataObject.hasAutoincrement()) {
        continue;
      }
      try {
        Class<?> type = field.getType();
        if (type == List.class) {
          continue;
        }
        field.setAccessible(true);
        Object value = field.get(bean);
        if (columns != null) {
          columns.add(ReflectHelper.getColumnName(field));
        }
        if (values == null) {
          continue;
        }
        if (field.getType() == Boolean.class || field.getType() == boolean.class) {
          int intValue = (Boolean) value ? 1 : 0;
          values.add(String.valueOf(intValue));
        } else if (field.getType() == Byte[].class || field.getType() == byte[].class) {
          if (value == null) {
            values.add("NULL");
          } else {
            String hex = getHex((byte[]) value);
            values.add(concat("X'", hex, QUOTE));
          }
        } else if (value == null) {
          Column columnAnnotation = field.getAnnotation(Column.class);
          boolean hasDefault = false;
          if (columnAnnotation != null) {
            hasDefault = !columnAnnotation.defaultValue().equals(Column.NULL);
          }
          if (columnAnnotation != null && columnAnnotation.notNull() && !hasDefault) {
            String msg = concat("Field ", field.getName(), " from class ", theClass.getSimpleName(),
                " cannot be null. It was marked with the @Column not null annotation and it has not a default value");
            throw new IllegalStateException(msg);
          }
          if (hasDefault) {
            values.add(concat(QUOTE,
                columnAnnotation.defaultValue().replace(String.valueOf(QUOTE), DOUBLE_QUOTE),
                QUOTE));
          } else {
            values.add("NULL");
          }
        } else {
          values.add(
              concat(QUOTE, String.valueOf(value).replace(String.valueOf(QUOTE), DOUBLE_QUOTE),
                  QUOTE));
        }
      } catch (IllegalAccessException ignored) {
      }
    }
    if (parent != null) {
      HasManySpec hasManySpec = getHasManySpec(theClass, parent);
      Object foreignValue = getRelationValueFromParent(parent, hasManySpec);
      if (columns != null) {
        columns.add(hasManySpec.getThroughColumnName());
      }
      if (values != null) {
        if (foreignValue != null && hasData(foreignValue.getClass(), foreignValue)) {
          values.add(String.valueOf(foreignValue));
        } else {
          String tableName = getTableName(parent.getClass());
          values.add(concat("(SELECT seq FROM sqlite_sequence WHERE name = '", tableName, "')"));
        }
      }
    }
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

  // TODO reduce use of this method
  public static String getTableName(Class<?> theClass) {
    if (TABLE_NAMES_CACHE.containsKey(theClass)) {
      return TABLE_NAMES_CACHE.get(theClass);
    }
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
      tableName = normalize(name);
    }
    TABLE_NAMES_CACHE.put(theClass, tableName);
    return tableName;
  }

  private static <T> String getTableName(T bean) {
    return getTableName(bean.getClass());
  }

  private static String getHex(byte[] raw) {
    if (raw == null) {
      return null;
    }
    final StringBuilder hex = new StringBuilder(2 * raw.length);
    for (final byte b : raw) {
      hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
    }
    return hex.toString();
  }

  /**
   * @param theClass the class to the get primary key from
   * @return the primary key from a class
   */
  static String getPrimaryKey(Class<?> theClass) {
    for (Field field : getDeclaredFields(theClass)) {
      if (ReflectHelper.isPrimaryKey(field)) {
        return field.getName();
      }
    }
    throw new IllegalStateException("Class " + theClass + " does not have a primary key");
  }

  /**
   * @param theClass the class to the get primary key from
   * @return the primary key field from a class
   */
  static Field getPrimaryKeyField(Class<?> theClass) {
    for (Field field : getDeclaredFields(theClass)) {
      if (ReflectHelper.isPrimaryKey(field)) {
        field.setAccessible(true);
        return field;
      }
    }
    throw new IllegalStateException("Class " + theClass + " does not have a primary key");
  }

  static <T, Parent> Cursor getCursorFindAllWhere(SQLiteDatabase db, Class<? extends T> clazz,
                                                  T sample, Parent parent, Constraint constraint) {
    String[] selectionArgs = null;
    String where = null;
    if (sample != null || parent != null) {
      ArrayList<String> args = new ArrayList<String>();
      where = getWhere(clazz, sample, args, parent);
      if (TextUtils.isEmpty(where)) {
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
      if (constraint.getLimit() != null) {
        limit = constraint.getLimit().toString();
      }
      groupBy = constraint.getGroupBy();
    }
    return db.query(getTableName(clazz), null, where, selectionArgs, groupBy, null, orderBy, limit);
  }

  static <T> String getFastInsertSqlHeader(T bean) {
    ArrayList<String> values = new ArrayList<String>();
    ArrayList<String> columns = new ArrayList<String>();
    populateColumnsAndValues(bean, null, values, columns);

    StringBuilder result = new StringBuilder();

    result.append("INSERT OR IGNORE INTO ").append(getTableName(bean.getClass())).append(" ");
    // set insert columns
    result.append("(");
    result.append(join(columns, ", "));
    result.append(")");
    // add first insertion body
    result.append(" SELECT ");

    ArrayList<String> columnsAndValues = new ArrayList<String>();
    for (int i = 0, valuesSize = values.size(); i < valuesSize; i++) {
      String column = columns.get(i);
      String value = values.get(i);
      StringBuilder columnAndValue = new StringBuilder();
      columnAndValue.append(value).append(" AS ").append(column);
      columnsAndValues.add(columnAndValue.toString());
    }
    result.append(join(columnsAndValues, ", "));
    return result.toString();
  }

  static <T> String getUnionInsertSql(T bean) {
    ArrayList<String> values = new ArrayList<String>();
    populateColumnsAndValues(bean, null, values, null);
    StringBuilder builder = new StringBuilder();
    builder.append(" UNION SELECT ");
    builder.append(join(values, ", "));
    return builder.toString();
  }
}
