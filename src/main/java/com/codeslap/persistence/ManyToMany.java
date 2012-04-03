/*
 * Copyright 2012 CodeSlap
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

/**
 * Establishes a many-to-many relation between two models
 *
 * @author cristian
 * @version 1.0
 */
public class ManyToMany {
    private final Class<?> mClassA;
    private final String mClassAPrimaryKey;
    private final Class<?> mClassB;
    private final String mClassBPrimaryKey;

    public ManyToMany(Class<?> classA, String classAPrimaryKey, Class<?> classB, String classBPrimaryKey) {
        mClassA = classA;
        mClassAPrimaryKey = classAPrimaryKey;
        mClassB = classB;
        mClassBPrimaryKey = classBPrimaryKey;
    }

    public ManyToMany(Class<?> classA, Class<?> classB) {
        this(classA, SQLHelper.ID, classB, SQLHelper.ID);
    }

    Class<?> getFirstRelation() {
        return mClassA;
    }

    Class<?> getSecondRelation() {
        return mClassB;
    }

    /**
     * @return the SQL statement for the join table creation
     */
    String getCreateTableStatement() {
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE IF NOT EXISTS ").append(buildTableName(mClassA, mClassB));
        builder.append(" (_id INTEGER PRIMARY KEY AUTOINCREMENT, ");
        builder.append(getMainKey()).append(" TEXT NOT NULL, ");
        builder.append(getSecondaryKey()).append(" TEXT NOT NULL");
        builder.append(");");
        return builder.toString();
    }

    String getMainKey() {
        return new StringBuilder().append(SQLHelper.getTableName(mClassA)).append("_").append(mClassAPrimaryKey).toString();
    }

    String getSecondaryKey() {
        return new StringBuilder().append(SQLHelper.getTableName(mClassB)).append("_").append(mClassBPrimaryKey).toString();
    }

    /**
     * @param classA a model
     * @param classB another model
     * @return name of the joined class
     */
    static String buildTableName(Class<?> classA, Class<?> classB) {
        if (classA.getSimpleName().compareToIgnoreCase(classB.getSimpleName()) <= 0) {
            return new StringBuilder().append(SQLHelper.getTableName(classA)).append("_").append(SQLHelper.getTableName(classB)).toString();
        }
        return new StringBuilder().append(SQLHelper.getTableName(classB)).append("_").append(SQLHelper.getTableName(classA)).toString();
    }

    /**
     * @return name of the joined class
     */
    String getTableName() {
        if (mClassA.getSimpleName().compareToIgnoreCase(mClassB.getSimpleName()) <= 0) {
            return new StringBuilder().append(SQLHelper.getTableName(mClassA)).append("_").append(SQLHelper.getTableName(mClassB)).toString();
        }
        return new StringBuilder().append(SQLHelper.getTableName(mClassB)).append("_").append(SQLHelper.getTableName(mClassA)).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ManyToMany)) return false;

        ManyToMany that = (ManyToMany) o;

        if (mClassA != null ? !mClassA.equals(that.mClassA) : that.mClassA != null) return false;
        if (mClassAPrimaryKey != null ? !mClassAPrimaryKey.equals(that.mClassAPrimaryKey) : that.mClassAPrimaryKey != null)
            return false;
        if (mClassB != null ? !mClassB.equals(that.mClassB) : that.mClassB != null) return false;
        if (mClassBPrimaryKey != null ? !mClassBPrimaryKey.equals(that.mClassBPrimaryKey) : that.mClassBPrimaryKey != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mClassA != null ? mClassA.hashCode() : 0;
        result = 31 * result + (mClassAPrimaryKey != null ? mClassAPrimaryKey.hashCode() : 0);
        result = 31 * result + (mClassB != null ? mClassB.hashCode() : 0);
        result = 31 * result + (mClassBPrimaryKey != null ? mClassBPrimaryKey.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ManyToMany relation between " + mClassA.getSimpleName() +
                " and " + mClassB.getSimpleName() + ", using " + mClassAPrimaryKey +
                " and " + mClassBPrimaryKey + " respectively";
    }
}
