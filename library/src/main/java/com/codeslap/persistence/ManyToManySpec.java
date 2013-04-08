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

import static com.codeslap.persistence.StrUtil.concat;

class ManyToManySpec {
  private final DataObject<?> mDataObjectA;
  private final DataObject<?> mDataObjectB;
  private final Field firstRelationField;

  ManyToManySpec(DataObject<?> dataObjectA, Field firstRelationField, DataObject<?> dataObjectB) {

    mDataObjectA = dataObjectA;
    this.firstRelationField = firstRelationField;
    mDataObjectB = dataObjectB;
  }

  DataObject<?> getFirstRelation() {
    return mDataObjectA;
  }

  DataObject<?> getSecondRelation() {
    return mDataObjectB;
  }

  Field getFirstRelationField() {
    return firstRelationField;
  }

  /** @return the SQL statement for the join table creation */
  String getCreateTableStatement() {
    StringBuilder builder = new StringBuilder();
    builder.append("CREATE TABLE IF NOT EXISTS ").append(getTableName());
    builder.append(" (_id INTEGER PRIMARY KEY AUTOINCREMENT, ");
    builder.append(getMainKey()).append(" TEXT NOT NULL, ");
    builder.append(getSecondaryKey()).append(" TEXT NOT NULL");
    builder.append(");");
    return builder.toString();
  }

  String getMainKey() {
    return concat(mDataObjectA.getTableName(), "_", mDataObjectA.getPrimaryKeyFieldName());
  }

  String getSecondaryKey() {
    return concat(mDataObjectB.getTableName(), "_", mDataObjectB.getPrimaryKeyFieldName());
  }

  /** @return name of the joined class */
  String getTableName() {
    String classATableName = mDataObjectA.getTableName();
    String classBTableName = mDataObjectB.getTableName();
    if (classATableName.compareToIgnoreCase(classBTableName) <= 0) {
      return concat(classATableName, "_", classBTableName);
    }
    return concat(classBTableName, "_", classATableName);
  }
}