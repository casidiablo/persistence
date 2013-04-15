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
import java.util.HashMap;
import java.util.Map;

/**
 * Helper methods used with reflection
 *
 * @author cristian
 */
class ReflectHelper {
  private static final Map<Field, String> COLUMN_NAMES_CACHE = new HashMap<Field, String>();

  /**
   * @param field field to get the column name from
   * @return gets the column name version of the specified field
   */
  static String getColumnName(Field field) {
    if (COLUMN_NAMES_CACHE.containsKey(field)) {
      return COLUMN_NAMES_CACHE.get(field);
    }
    String columnName = ColumnHelper.getColumnName(new ReflectColumnField(field));
    COLUMN_NAMES_CACHE.put(field, columnName);
    return columnName;
  }

  static boolean isPrimaryKey(Field field) {
    return ColumnHelper.isPrimaryKey(new ReflectColumnField(field));
  }

  static String getIdColumn(Field field) {
    return ColumnHelper.getIdColumn(new ReflectColumnField(field));
  }
}
