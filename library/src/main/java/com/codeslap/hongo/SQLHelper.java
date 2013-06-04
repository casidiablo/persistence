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
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import java.util.*;

import static com.codeslap.hongo.DataObjectFactory.getDataObject;

public class SQLHelper {

  static final String ID = "id";
  static final String _ID = "_id";
  static final String HEXES = "0123456789ABCDEF";

  private static final Map<Class<?>, String> INSERT_COLUMNS_CACHE = new HashMap<Class<?>, String>();

  static final String STATEMENT_SEPARATOR = "b05f72bb_STATEMENT_SEPARATOR";
  static final char QUOTE = '\'';
  static final String DOUBLE_QUOTE = "''";

  private static <T> String getSet(T bean) {
    List<String> sets = new ArrayList<String>();
    if (bean == null) {
      return StrUtil.join(sets, ", ");
    }
    DataObject<T> dataObject = getDataObject((Class<T>) bean.getClass());
    for (ColumnField field : dataObject.getDeclaredFields()) {
      Class<?> type = field.getType();
      if (type == List.class) {
        continue;
      }
      Object value = field.get(bean);
      boolean isBoolean = field.getType() == Boolean.class || field.getType() == boolean.class;
      if (isBoolean || dataObject.hasData(field.getName(), bean)) {
        if (isBoolean) {
          int intValue = (Boolean) value ? 1 : 0;
          sets.add(StrUtil.concat(ColumnHelper.getColumnName(field), " = '", intValue, QUOTE));
        } else if (field.getType() == byte[].class || field.getType() == Byte[].class) {
          String hex = getHex((byte[]) value);
          sets.add(StrUtil.concat(ColumnHelper.getColumnName(field), " = X'", hex, QUOTE));
        } else {
          String cleanedVal = String.valueOf(value).replace(String.valueOf(QUOTE), DOUBLE_QUOTE);
          sets.add(StrUtil.concat(ColumnHelper.getColumnName(field), " = '", cleanedVal, QUOTE));
        }
      }
    }
    return StrUtil.join(sets, ", ");
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
    Class<T> beanClass = (Class<T>) bean.getClass();
    DataObject<T> dataObject = getDataObject(beanClass);
    String where = dataObject.getWhere(sample, null, null);
    String set = getSet(bean);
    return StrUtil.concat("UPDATE ", dataObject.getTableName(), " SET ", set, " WHERE ", where, ";",
        STATEMENT_SEPARATOR);
  }

  public static <T> String buildUpdateStatement(T bean, String where) {
    String set = getSet(bean);
    Class<T> beanClass = (Class<T>) bean.getClass();
    DataObject<?> dataObject = getDataObject(beanClass);
    return StrUtil.concat("UPDATE ", dataObject.getTableName(), " SET ", set, " WHERE ", where, ";",
        STATEMENT_SEPARATOR);
  }

  static <T, Parent> String getInsertStatement(T bean, Parent parent) {
    List<String> values = new ArrayList<String>();
    List<String> columns = null;
    if (!INSERT_COLUMNS_CACHE.containsKey(bean.getClass())) {
      columns = new ArrayList<String>();
    }
    Class<T> beanClass = (Class<T>) bean.getClass();
    DataObject<T> dataObject = getDataObject(beanClass);
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
      String hack = StrUtil.concat("(SELECT seq FROM sqlite_sequence WHERE name = '", tableName,
          "')+1");
      ColumnField pkField = dataObject.getField(dataObject.getPrimaryKeyFieldName());
      String idColumn = ColumnHelper.getIdColumn(pkField);
      return StrUtil.concat("INSERT OR IGNORE INTO ", tableName, " (", idColumn, ") VALUES (", hack,
          ");", STATEMENT_SEPARATOR);
    }
    return StrUtil.concat("INSERT OR IGNORE INTO ", tableName, " (", columnsSet, ") VALUES (",
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
   * @param type the class to the get primary key from
   * @return the primary key from a class
   */
  // TODO should this get removed?
  static String getPrimaryKey(ObjectType type) {
    DataObject<?> dataObject = DataObjectFactory.getDataObject(type.getObjectClass());
    for (ColumnField field : dataObject.getDeclaredFields()) {
      if (ColumnHelper.isPrimaryKey(field)) {
        return field.getName();
      }
    }
    throw new IllegalStateException("Class " + type + " does not have a primary key");
  }

  static <T, Parent> Cursor getCursorFindAllWhere(SQLiteDatabase db, ObjectType<T> type, T sample,
                                                  Parent parent, Constraint constraint) {
    DataObject<T> dataObject = getDataObject(type.getObjectClass());
    String[] selectionArgs = null;
    String where = null;
    if (sample != null || parent != null) {
      ArrayList<String> args = new ArrayList<String>();
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

    Class<T> beanClass = (Class<T>) bean.getClass();
    DataObject<T> dataObject = getDataObject(beanClass);
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
    Class<T> beanClass = (Class<T>) bean.getClass();
    DataObject<T> dataObject = getDataObject(beanClass);
    dataObject.populateColumnsAndValues(bean, null, values, null);
    StringBuilder builder = new StringBuilder();
    builder.append(" UNION SELECT ");
    builder.append(StrUtil.join(values, ", "));
    return builder.toString();
  }

  static String getTableName(ObjectType<?> theClass) {
    Table table = theClass.getAnnotation(Table.class);
    String tableName;
    if (table != null) {
      tableName = table.value();
      if (TextUtils.isEmpty(tableName)) {
        String msg = StrUtil.concat("You cannot leave a table name empty: class ",
            theClass.getSimpleName());
        throw new IllegalArgumentException(msg);
      }
      if (tableName.contains(" ")) {
        String msg = StrUtil.concat("Table name cannot have spaces: '", tableName,
            "'; found in class ", theClass.getSimpleName());
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
    return tableName;
  }
}
