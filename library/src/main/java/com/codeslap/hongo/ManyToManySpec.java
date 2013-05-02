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
  private final ObjectType<?> mDataObjectA;
  private final String daoAPKFieldName;
  private final ObjectType<?> mDataObjectB;
  private final String daoBPKFieldName;
  private final String dataObjectARelationFieldName;

  ManyToManySpec(ObjectType<?> dataObjectA, String relationFieldName, ObjectType<?> dataObjectB,
      String aPkName, String bPkName) {
    mDataObjectA = dataObjectA;
    dataObjectARelationFieldName = relationFieldName;
    mDataObjectB = dataObjectB;
    daoAPKFieldName = aPkName;
    daoBPKFieldName = bPkName;
  }

  ObjectType<?> getFirstRelation() {
    return mDataObjectA;
  }

  ObjectType<?> getSecondRelation() {
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
    return StrUtil.concat(mDataObjectA.getTableName(), "_", daoAPKFieldName);
  }

  String getSecondaryKey() {
    return StrUtil.concat(mDataObjectB.getTableName(), "_", daoBPKFieldName);
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