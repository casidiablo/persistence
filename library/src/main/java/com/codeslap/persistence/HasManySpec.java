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
import java.lang.reflect.ParameterizedType;

import static com.codeslap.persistence.StrUtil.concat;

/**
 * //
 *
 * @author cristian
 */
class HasManySpec {
  final Class<?> container;
  final Class<?> contained;
  final Field throughField;
  final Field listField;
  final String getThroughColumnName;

  HasManySpec(Class<?> container, Field listField) {
    this.container = container;
    this.listField = listField;

    ParameterizedType parameterizedType = (ParameterizedType) listField.getGenericType();
    Class<?> collectionClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];
    contained = collectionClass;
    if (contained == null || contained == Object.class) {
      throw new IllegalStateException("Cannot use Object class. Sorry :P");
    }

    String through = SQLHelper.getPrimaryKey(container);
    try {
      throughField = container.getDeclaredField(through);
      throughField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new IllegalStateException("Cannot find field " + through);
    }

    // if so, add a new field to the table creation statement to create the relation
    String containerClassNormalized = SQLHelper.normalize(container.getSimpleName());
    String throughFieldNormalized = SQLHelper.normalize(SQLHelper.getPrimaryKey(container));
    getThroughColumnName = concat(containerClassNormalized, "_", throughFieldNormalized);
  }

  String getThroughColumnName() {
    return getThroughColumnName;
  }
}
