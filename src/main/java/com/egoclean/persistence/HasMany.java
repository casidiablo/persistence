package com.egoclean.persistence;

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

    private final Class<?> mClassA;
    private final Class<?> mClassB;
    private final String mThrough;

    public HasMany(Class<?> classA, Class<?> hasMany, String through) {
        // do not accept recursive relations
        if (classA == hasMany) {
            throw new IllegalStateException("Recursive has-many relations are not accepted");
        }
        // make sure the 'through' exists
        try {
            classA.getDeclaredField(through);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
        // we must make sure the relation exists
        boolean relationExists = false;
        for (Field field : classA.getDeclaredFields()) {
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
            throw new IllegalStateException("Relation does not exist (" + classA + " has many " + hasMany + ")");
        }
        // if it does exist, set the fields
        mClassA = classA;
        mClassB = hasMany;
        mThrough = through;
    }

    /**
     * Creates a has-many relation
     *
     * @param classA  the class that has many objects...
     * @param hasMany the class contained in classA
     */
    public HasMany(Class<?> classA, Class<?> hasMany) {
        this(classA, hasMany, SQLHelper.ID);
    }

    Class<?>[] getClasses() {
        Class<?>[] classes = new Class<?>[2];
        classes[0] = mClassA;
        classes[1] = mClassB;
        return classes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HasMany)) return false;

        HasMany hasMany = (HasMany) o;

        if (mClassA != null ? !mClassA.equals(hasMany.mClassA) : hasMany.mClassA != null) return false;
        if (mClassB != null ? !mClassB.equals(hasMany.mClassB) : hasMany.mClassB != null) return false;
        if (mThrough != null ? !mThrough.equals(hasMany.mThrough) : hasMany.mThrough != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mClassA != null ? mClassA.hashCode() : 0;
        result = 31 * result + (mClassB != null ? mClassB.hashCode() : 0);
        result = 31 * result + (mThrough != null ? mThrough.hashCode() : 0);
        return result;
    }

    String getThrough() {
        return mThrough;
    }

    String getForeignKey() {
        return String.format("%s_%s", SqlUtils.normalize(mClassA.getSimpleName()), SqlUtils.normalize(mThrough));
    }

    @Override
    public String toString() {
        return mClassA + " has many " + mClassB + " through " + mThrough;
    }
}
