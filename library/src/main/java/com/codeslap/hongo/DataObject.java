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
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This class contains all necessary information to save/retrieve an object to/from the database.
 *
 * @author cristian
 */
public interface DataObject<T> {
  /** @return SQLite compatible sentence to create a table representing this object */
  String getCreateTableSentence();

  /**
   * It assumes the object has no constructor or it's both public and without parameters
   *
   * @return new instance of this object
   */
  T newInstance();

  /** @return true if the table has an auto-incrementable primary key */
  boolean hasAutoincrement();

  Collection<HasManySpec> hasMany();

  Collection<ManyToManySpec> manyToMany();

  <Child> HasManySpec hasMany(Class<Child> theClass);

  Class<?> belongsTo();

  Class<T> getObjectClass();

  String getTableName();

  String getPrimaryKeyFieldName();

  boolean hasData(String fieldName, T bean);

  T getBeanFromCursor(Cursor join, Set<Class<?>> tree, SqliteDb dbHelper);

  <Parent> String getWhere(T bean, List<String> args, Parent parent);

  <Parent> void populateColumnsAndValues(T bean, Parent parent, List<String> values,
                                         List<String> columns);

  SqliteType getTypeFrom(String fieldName);

  Collection<ColumnField> getDeclaredFields();

  ColumnField getField(String name);
}
