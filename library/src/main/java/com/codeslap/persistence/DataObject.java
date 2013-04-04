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
import java.util.Collection;

import static com.codeslap.persistence.StrUtil.concat;

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

  HasManySpec hasMany(Class<?> theClass);

  Class<?> belongsTo();

  static class HasManySpec {
    public final Class<?> container;
    public final Class<?> contained;
    public final String through;
    public final Field fieldThrough;
    public final Field listField;

    public HasManySpec(Class<?> container, Class<?> contained, String through, Field listField) {
      this.container = container;
      this.contained = contained;
      this.through = through;
      this.listField = listField;
      try {
        Field throughField = container.getDeclaredField(through);
        throughField.setAccessible(true);
        this.fieldThrough = throughField;
      } catch (NoSuchFieldException e) {
        throw new IllegalStateException("Cannot find field " + through);
      }
    }

    public String getThroughColumnName() {
      // if so, add a new field to the table creation statement to create the relation
      String containerClassNormalized = SQLHelper.normalize(container.getSimpleName());
      String throughFieldNormalized = SQLHelper.normalize(through);
      return concat(containerClassNormalized, "_", throughFieldNormalized);
    }
  }
}
