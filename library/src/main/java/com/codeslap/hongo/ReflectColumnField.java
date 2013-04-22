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
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.List;

public class ReflectColumnField implements ColumnField {

  private final Field field;

  public ReflectColumnField(Field field) {
    this.field = field;
    this.field.setAccessible(true);
  }

  @Override public <T extends Annotation> T getAnnotation(Class<T> annotation) {
    return field.getAnnotation(annotation);
  }

  @Override public String getName() {
    return field.getName();
  }

  @Override public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotation) {
    return field.isAnnotationPresent(annotation);
  }

  @Override public Class<?> getType() {
    return field.getType();
  }

  @Override public boolean set(Object target, Object value) {
    try {
      field.set(target, value);
      return true;
    } catch (IllegalAccessException e) {
      return false;
    }
  }

  @Override public Object get(Object target) {
    try {
      return field.get(target);
    } catch (IllegalAccessException e) {
      return null;
    }
  }

  @Override public Class<?> getGenericType() {
    if (getType() != List.class) {
      throw new IllegalStateException(
          "Cannot use get generic type with a non list field: " + field);
    }
    ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
    Class<?> collectionClass = (Class<?>) stringListType.getActualTypeArguments()[0];
    return collectionClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReflectColumnField that = (ReflectColumnField) o;
    return !(field != null ? !field.equals(that.field) : that.field != null);
  }

  @Override
  public int hashCode() {
    return field != null ? field.hashCode() : 0;
  }

  public boolean isStatic() {
    return Modifier.isStatic(field.getModifiers());
  }

  public boolean isFinal() {
    return Modifier.isFinal(field.getModifiers());
  }
}
