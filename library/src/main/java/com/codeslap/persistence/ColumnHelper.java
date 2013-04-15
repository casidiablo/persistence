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

import java.util.HashMap;
import java.util.Map;

/**
 * // TODO write description
 *
 * @author cristian
 */
class ColumnHelper {
  private static final Map<ColumnField, String> COLUMN_NAMES_CACHE = new HashMap<ColumnField, String>();

  /**
   * @param field field to get the column name from
   * @return gets the column name version of the specified field
   */
  static String getColumnName(ColumnField field) {
    if (COLUMN_NAMES_CACHE.containsKey(field)) {
      return COLUMN_NAMES_CACHE.get(field);
    }
    if (isPrimaryKey(field) && !forcedName(field)) {
      return getIdColumn(field);
    }
    Column column = field.getAnnotation(Column.class);
    if (column != null) {
      return column.value();
    }
    String name = field.getName();
    StringBuilder newName = new StringBuilder();
    newName.append(name.charAt(0));
    for (int i = 1; i < name.length(); i++) {
      if (Character.isUpperCase(name.charAt(i))) {
        newName.append("_");
      }
      newName.append(name.charAt(i));
    }
    String columnName = newName.toString().toLowerCase();
    COLUMN_NAMES_CACHE.put(field, columnName);
    return columnName;
  }

  static boolean isPrimaryKey(ColumnField field) {
    if (field.isAnnotationPresent(PrimaryKey.class)) {
      return true;
    }
    return field.getName().equals(SQLHelper.ID) || field.getName().equals(getIdColumn(field));
  }

  static String getIdColumn(ColumnField field) {
    if (forcedName(field)) {
      return getColumnName(field);
    }
    return SQLHelper._ID;
  }

  static boolean forcedName(ColumnField field) {
    return field.isAnnotationPresent(Column.class) && field.getAnnotation(Column.class).forceName();
  }
}
