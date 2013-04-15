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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.SimpleTypeVisitor6;
import java.util.List;

/** Some utilities were shamelessly stolen from dagger project */
class CodeGenHelper {
  static PackageElement getPackage(Element element) {
    while (element.getKind() != ElementKind.PACKAGE) {
      element = element.getEnclosingElement();
    }
    return (PackageElement) element;
  }

  /** Returns a string for {@code type}. Primitive types are always boxed. */
  public static String typeToString(TypeMirror type) {
    StringBuilder result = new StringBuilder();
    typeToString(type, result, '.');
    return result.toString();
  }

  /**
   * Appends a string for {@code type} to {@code result}. Primitive types are always boxed.
   *
   * @param innerClassSeparator either '.' or '$', which will appear in a class name like
   *                            "java.lang.Map.Entry" or "java.lang.Map$Entry". Use '.' for
   *                            references to existing types in code. Use '$' to define new class
   *                            names and for strings that will be used by runtime reflection.
   */
  public static void typeToString(final TypeMirror type, final StringBuilder result, final char innerClassSeparator) {
    type.accept(new SimpleTypeVisitor6<Void, Void>() {
      @Override public Void visitDeclared(DeclaredType declaredType, Void v) {
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        rawTypeToString(result, typeElement, innerClassSeparator);
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (!typeArguments.isEmpty()) {
          result.append("<");
          for (int i = 0; i < typeArguments.size(); i++) {
            if (i != 0) {
              result.append(", ");
            }
            typeToString(typeArguments.get(i), result, innerClassSeparator);
          }
          result.append(">");
        }
        return null;
      }

      @Override public Void visitPrimitive(PrimitiveType primitiveType, Void v) {
        result.append(box((PrimitiveType) type).getName());
        return null;
      }

      @Override public Void visitArray(ArrayType arrayType, Void v) {
        typeToString(arrayType.getComponentType(), result, innerClassSeparator);
        result.append("[]");
        return null;
      }

      @Override public Void visitTypeVariable(TypeVariable typeVariable, Void v) {
        result.append(typeVariable.asElement().getSimpleName());
        return null;
      }

      @Override protected Void defaultAction(TypeMirror typeMirror, Void v) {
        throw new UnsupportedOperationException("Unexpected TypeKind " + typeMirror.getKind() + " for " + typeMirror);
      }
    }, null);
  }

  private static Class<?> box(PrimitiveType primitiveType) {
    switch (primitiveType.getKind()) {
      case BYTE:
        return Byte.class;
      case SHORT:
        return Short.class;
      case INT:
        return Integer.class;
      case LONG:
        return Long.class;
      case FLOAT:
        return Float.class;
      case DOUBLE:
        return Double.class;
      case BOOLEAN:
        return Boolean.class;
      case CHAR:
        return Character.class;
      case VOID:
        return Void.class;
      default:
        throw new AssertionError();
    }
  }

  private static void rawTypeToString(StringBuilder result, TypeElement type, char innerClassSeparator) {
    String packageName = getPackage(type).getQualifiedName().toString();
    String qualifiedName = type.getQualifiedName().toString();
    if (packageName.isEmpty()) {
      result.append(qualifiedName.replace('.', innerClassSeparator));
    } else {
      result.append(packageName);
      result.append('.');
      result.append(qualifiedName.substring(packageName.length() + 1).replace('.', innerClassSeparator));
    }
  }

  public static String getColumnName(Element element) {
    if (isPrimaryKey(element) && !forcedName(element)) {
      return getIdColumn(element);
    }
    Column column = element.getAnnotation(Column.class);
    if (column != null) {
      return column.value();
    }
    String name = element.getSimpleName().toString();
    StringBuilder newName = new StringBuilder();
    newName.append(name.charAt(0));
    for (int i = 1; i < name.length(); i++) {
      if (Character.isUpperCase(name.charAt(i))) {
        newName.append("_");
      }
      newName.append(name.charAt(i));
    }
    return newName.toString().toLowerCase();
  }

  public static String getIdColumn(Element element) {
    if (forcedName(element)) {
      return getColumnName(element);
    }
    return SQLHelper._ID;
  }

  private static boolean forcedName(Element element) {
    Column column = element.getAnnotation(Column.class);
    if (column == null) {
      return false;
    }
    return column.forceName();
  }

  static boolean isPrimaryKey(Element element) {
    PrimaryKey primaryKey = element.getAnnotation(PrimaryKey.class);
    if (primaryKey != null) {
      return true;
    }
    String elementName = element.getSimpleName().toString();
    return elementName.equals(SQLHelper.ID) || elementName
        .equals(getIdColumn(element));
  }
}
