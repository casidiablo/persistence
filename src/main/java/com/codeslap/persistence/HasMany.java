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
import java.lang.reflect.ParameterizedType;
import java.util.List;

/**
 * Establishes a has many relation
 *
 * @author cristian
 * @version 1.0
 */
public class HasMany {

    private final Class<?> mContainerClass;
    private final Class<?> mContainedClass;
    private final String mThrough;
    private final String mThroughColumnName;
    private final Field mThroughField;

    public HasMany(Class<?> classA, Class<?> hasMany, String through, boolean artificial) {
        // do not accept recursive relations
        if (classA == hasMany) {
            throw new IllegalArgumentException("Recursive has-many relations are not accepted");
        }
        // make sure the 'through' exists
        try {
            mThroughField = classA.getDeclaredField(through);
            mThroughColumnName = SQLHelper.getColumnName(mThroughField);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
        if (!artificial) {
            // we must make sure the relation exists
            boolean relationExists = false;
            for (Field field : SQLHelper.getDeclaredFields(classA)) {
                if (field.getType() == List.class) {
                    ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
                    Class<?> collectionClass = (Class<?>) stringListType.getActualTypeArguments()[0];
                    if (collectionClass == hasMany) {
                        relationExists = true;
                        break;
                    }
                }
            }
            // if relation does not exist, do not continue
            if (!relationExists) {
                String msg = String.format("Relation does not exist (%s has many %s)", classA, hasMany);
                throw new IllegalStateException(msg);
            }
        }
        // if it does exist, set the fields
        mContainerClass = classA;
        mContainedClass = hasMany;
        mThrough = through;
    }

    public HasMany(Class<?> classA, Class<?> hasMany, String through) {
        this(classA, hasMany, through, false);
    }

    /**
     * Creates a has-many relation
     *
     * @param classA  the class that has many objects...
     * @param hasMany the class contained in classA
     */
    public HasMany(Class<?> classA, Class<?> hasMany) {
        this(classA, hasMany, SQLHelper.getPrimaryKey(hasMany));
    }

    /**
     * Creates a has-many relation
     *
     * @param classA     the class that has many objects...
     * @param hasMany    the class contained in classA
     * @param artificial must be true if the relation does not actually exists and you will force it
     */
    public HasMany(Class<?> classA, Class<?> hasMany, boolean artificial) {
        this(classA, hasMany, SQLHelper.ID, artificial);
    }

    Class<?> getContainerClass() {
        return mContainerClass;
    }

    Class<?> getContainedClass() {
        return mContainedClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HasMany)) return false;

        HasMany hasMany = (HasMany) o;

        if (mContainerClass != null ? !mContainerClass.equals(hasMany.mContainerClass) : hasMany.mContainerClass != null)
            return false;
        if (mContainedClass != null ? !mContainedClass.equals(hasMany.mContainedClass) : hasMany.mContainedClass != null)
            return false;
        if (mThrough != null ? !mThrough.equals(hasMany.mThrough) : hasMany.mThrough != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mContainerClass != null ? mContainerClass.hashCode() : 0;
        result = 31 * result + (mContainedClass != null ? mContainedClass.hashCode() : 0);
        result = 31 * result + (mThrough != null ? mThrough.hashCode() : 0);
        return result;
    }

    String getThroughColumnName() {
        return mThroughColumnName;
    }

    Field getThroughField() {
        return mThroughField;
    }

    String getForeignKey() {
        return String.format("%s_%s", SQLHelper.normalize(mContainerClass.getSimpleName()), SQLHelper.normalize(mThrough));
    }

    @Override
    public String toString() {
        return mContainerClass + " has many " + mContainedClass + " through " + mThrough;
    }
}
