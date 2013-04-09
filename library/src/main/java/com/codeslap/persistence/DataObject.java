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

import java.util.Collection;

/**
 * //
 *
 * @author cristian
 */
public interface DataObject<T> {
  String getCreateTableSentence();

  T newInstance();

  boolean hasAutoincrement();

  Collection<HasManySpec> hasMany();

  Collection<ManyToManySpec> manyToMany();

  HasManySpec hasMany(Class<?> theClass);

  Class<?> belongsTo();

  Class<T> getObjectClass();

  String getTableName();

  String getPrimaryKeyFieldName();

  boolean set(String fieldName, T target, Object value);

  Object get(String fieldName, T target);

  boolean hasData(String fieldName, Object bean);
}
