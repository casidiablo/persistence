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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.lang.annotation.Annotation;

public class ProcessorColumnField implements ColumnField {

  private final Element element;

  public ProcessorColumnField(Element element) {
    if (element.getKind() != ElementKind.FIELD) {
      throw new IllegalArgumentException("Only fields are accepted");
    }
    this.element = element;
  }

  @Override public <T extends Annotation> T getAnnotation(Class<T> annotation) {
    return element.getAnnotation(annotation);
  }

  @Override public String getName() {
    return element.getSimpleName().toString();
  }

  @Override public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotation) {
    return element.getAnnotation(annotation) != null;
  }

  @Override public Class<?> getType() {
    return null;
  }

  @Override public boolean set(Object target, Object value) {
    return false;
  }

  @Override public Object get(Object target) {
    return null;
  }

  @Override public ObjectType<?> getGenericType() {
    return null;
  }

  @Override public boolean isStatic() {
    return false;
  }

  @Override public boolean isFinal() {
    return false;
  }
}
