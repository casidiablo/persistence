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

public class ManyToManySpec {
  private final DataObject<?> mDataObjectA;
  private final DataObject<?> mDataObjectB;
  private final String dataObjectARelationFieldName;

  ManyToManySpec(DataObject<?> dataObjectA, String relationFieldName, DataObject<?> dataObjectB) {
    mDataObjectA = dataObjectA;
    dataObjectARelationFieldName = relationFieldName;
    mDataObjectB = dataObjectB;
  }

  DataObject<?> getFirstRelation() {
    return mDataObjectA;
  }

  DataObject<?> getSecondRelation() {
    return mDataObjectB;
  }

  String getFirstRelationFieldName() {
    return dataObjectARelationFieldName;
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
    return StrUtil.concat(mDataObjectA.getTableName(), "_", mDataObjectA.getPrimaryKeyFieldName());
  }

  String getSecondaryKey() {
    return StrUtil.concat(mDataObjectB.getTableName(), "_", mDataObjectB.getPrimaryKeyFieldName());
  }

  /** @return name of the joined class */
  String getTableName() {
    String classATableName = mDataObjectA.getTableName();
    String classBTableName = mDataObjectB.getTableName();
    if (classATableName.compareToIgnoreCase(classBTableName) <= 0) {
      return StrUtil.concat(classATableName, "_", classBTableName);
    }
    return StrUtil.concat(classBTableName, "_", classATableName);
  }
}