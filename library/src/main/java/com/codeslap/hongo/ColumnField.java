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

/**
 * Representation of a column field
 *
 * @author cristian
 */
public interface ColumnField {
  <T extends Annotation> T getAnnotation(Class<T> annotation);

  String getName();

  <T extends Annotation> boolean isAnnotationPresent(Class<T> annotation);

  Class<?> getType();

  boolean set(Object target, Object value);

  Object get(Object target);

  Class<?> getGenericType();

  boolean isStatic();

  boolean isFinal();
}
