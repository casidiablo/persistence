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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class ReflectObjectType<T> implements ObjectType {
  private final Class<T> type;
  private ColumnField[] columnFields;

  public ReflectObjectType(Class<T> type) {
    this.type = type;
  }

  @Override public ColumnField[] getDeclaredFields() {
    if (columnFields == null) {
      Field[] declaredFields = type.getDeclaredFields();
      columnFields = new ColumnField[declaredFields.length];
      for (int i = 0; i < declaredFields.length; i++) {
        columnFields[i] = new ReflectColumnField(declaredFields[i]);
      }
    }
    return columnFields;
  }

  @Override public String getSimpleName() {
    return type.getSimpleName();
  }

  @Override public Annotation getAnnotation(Class annotationClass) {
    return type.getAnnotation(annotationClass);
  }

  @Override public String getTableName() {
    return SQLHelper.getTableName(this);
  }

  @Override public Object newInstance() {
    try {
      return type.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      error(e);
      return null;
    }
  }

  @Override public Class getObjectClass() {
    return type;
  }

  @Override public String getName() {
    return type.getName();
  }

  private void error(Exception e) {
    Throwable cause = e.getCause();
    throw cause instanceof RuntimeException ? (RuntimeException) cause : new RuntimeException(
        cause);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ObjectType) {
      ObjectType type = (ObjectType) o;
      return this.type.equals(type.getObjectClass());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return type != null ? type.hashCode() : 0;
  }
}
