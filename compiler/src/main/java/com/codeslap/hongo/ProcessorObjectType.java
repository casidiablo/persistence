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
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * // TODO write description
 *
 * @author cristian
 */
public class ProcessorObjectType implements ObjectType<Object> {

  private final Element element;

  public ProcessorObjectType(Element element) {
    if (element.getKind() != ElementKind.CLASS) {
      throw new IllegalArgumentException("Only fields are accepted");
    }
    this.element = element;
  }

  @Override public ColumnField[] getDeclaredFields() {
    List<Element> fields = new ArrayList<Element>();
    for (Element e : element.getEnclosedElements()) {
      if (e.getKind() == ElementKind.FIELD) {
        fields.add(e);
      }
    }

    ColumnField[] columnFields = new ColumnField[fields.size()];
    for (int i = 0; i < fields.size(); i++) {
      columnFields[i] = new ProcessorColumnField(fields.get(i));
    }
    return columnFields;
  }

  @Override public String getSimpleName() {
    return element.getSimpleName().toString();
  }

  @Override public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
    return element.getAnnotation(annotationClass);
  }

  @Override public String getTableName() {
    return SQLHelper.getTableName(this);
  }

  @Override public Object newInstance() {
    return null;
  }

  @Override public Class getObjectClass() {
    try {
      return Class.forName(element.getSimpleName().toString());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override public String getName() {
    return element.asType().toString();
  }
}
