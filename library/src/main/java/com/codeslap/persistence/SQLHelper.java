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
  static final String HEXES = "0123456789ABCDEF";

  private static final Map<Class<?>, String> INSERT_COLUMNS_CACHE = new HashMap<Class<?>, String>();
  private static final Map<Class<?>, Field[]> FIELDS_CACHE = new HashMap<Class<?>, Field[]>();

  static final String STATEMENT_SEPARATOR = "b05f72bb_STATEMENT_SEPARATOR";
  static final char QUOTE = '\'';
  static final String DOUBLE_QUOTE = "''";

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
            ReflectColumnField columnField = new ReflectColumnField(field);
            if (isBoolean) {
              int intValue = (Boolean) value ? 1 : 0;
              sets.add(concat(ColumnHelper.getColumnName(columnField), " = '", intValue, QUOTE));
            } else if (field.getType() == byte[].class || field.getType() == Byte[].class) {
              String hex = getHex((byte[]) value);
              sets.add(concat(ColumnHelper.getColumnName(columnField), " = X'", hex, QUOTE));
            } else {
              String cleanedVal = String.valueOf(value)
                  .replace(String.valueOf(QUOTE), DOUBLE_QUOTE);
              sets.add(concat(ColumnHelper.getColumnName(columnField), " = '", cleanedVal, QUOTE));
            }
          }
        } catch (IllegalAccessException ignored) {
        }
      }
    }
    return StrUtil.join(sets, ", ");
  }

  // TODO remove this :P
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

  static <T> String buildUpdateStatement(T bean, T sample) {
    DataObject<T> dataObject = getDataObject((Class<T>) bean.getClass());
    String where = dataObject.getWhere(sample, null, null);
    String set = getSet(bean);
    return concat("UPDATE ", dataObject.getTableName(), " SET ", set, " WHERE ", where, ";",
        STATEMENT_SEPARATOR);
  }

  public static <T> String buildUpdateStatement(T bean, String where) {
    String set = getSet(bean);
    DataObject<?> dataObject = getDataObject(bean.getClass());
    return concat("UPDATE ", dataObject.getTableName(), " SET ", set, " WHERE ", where, ";",
        STATEMENT_SEPARATOR);
  }

  static <T, Parent> String getInsertStatement(T bean, Parent parent) {
    List<String> values = new ArrayList<String>();
    List<String> columns = null;
    if (!INSERT_COLUMNS_CACHE.containsKey(bean.getClass())) {
      columns = new ArrayList<String>();
    }
    DataObject<T> dataObject = getDataObject((Class<T>) bean.getClass());
    dataObject.populateColumnsAndValues(bean, parent, values, columns);

    String columnsSet;
    if (INSERT_COLUMNS_CACHE.containsKey(bean.getClass())) {
      columnsSet = INSERT_COLUMNS_CACHE.get(bean.getClass());
    } else {
      columnsSet = StrUtil.join(columns, ", ");
      INSERT_COLUMNS_CACHE.put(bean.getClass(), columnsSet);
    }

    // build insert statement for the main object
    String tableName = dataObject.getTableName();
    if (values.size() == 0 && dataObject.hasAutoincrement()) {
      String hack = concat("(SELECT seq FROM sqlite_sequence WHERE name = '", tableName, "')+1");
      String idColumn = ColumnHelper.getIdColumn(
          new ReflectColumnField(getPrimaryKeyField(bean.getClass())));
      return concat("INSERT OR IGNORE INTO ", tableName, " (", idColumn, ") VALUES (", hack, ");",
          STATEMENT_SEPARATOR);
    }
    return concat("INSERT OR IGNORE INTO ", tableName, " (", columnsSet, ") VALUES (",
        StrUtil.join(values, ", "), ");", STATEMENT_SEPARATOR);
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
    DataObject<?> dataObject = DataObjectFactory.getDataObject(theClass);
    for (ColumnField field : dataObject.getDeclaredFields()) {
      if (ColumnHelper.isPrimaryKey(field)) {
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
      if (ColumnHelper.isPrimaryKey(new ReflectColumnField(field))) {
        field.setAccessible(true);
        return field;
      }
    }
    throw new IllegalStateException("Class " + theClass + " does not have a primary key");
  }

  static <T, Parent> Cursor getCursorFindAllWhere(SQLiteDatabase db, Class<? extends T> type,
                                                  T sample, Parent parent, Constraint constraint) {
    DataObject<T> dataObject = getDataObject((Class<T>) type);
    String[] selectionArgs = null;
    String where = null;
    if (sample != null || parent != null) {
      ArrayList<String> args = new ArrayList<String>();
      getDataObject(type);
      where = dataObject.getWhere(sample, args, parent);
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
    return db.query(dataObject.getTableName(), null, where, selectionArgs, groupBy, null, orderBy,
        limit);
  }

  static <T> String getFastInsertSqlHeader(T bean) {
    ArrayList<String> values = new ArrayList<String>();
    ArrayList<String> columns = new ArrayList<String>();

    DataObject<T> dataObject = getDataObject((Class<T>) bean.getClass());
    dataObject.populateColumnsAndValues(bean, null, values, columns);

    StringBuilder result = new StringBuilder();

    result.append("INSERT OR IGNORE INTO ").append(dataObject.getTableName()).append(" ");
    // set insert columns
    result.append("(");
    result.append(StrUtil.join(columns, ", "));
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
    result.append(StrUtil.join(columnsAndValues, ", "));
    return result.toString();
  }

  static <T> String getUnionInsertSql(T bean) {
    ArrayList<String> values = new ArrayList<String>();
    DataObject<T> dataObject = getDataObject((Class<T>) bean.getClass());
    dataObject.populateColumnsAndValues(bean, null, values, null);
    StringBuilder builder = new StringBuilder();
    builder.append(" UNION SELECT ");
    builder.append(StrUtil.join(values, ", "));
    return builder.toString();
  }
}
