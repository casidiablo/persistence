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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cristian
 */
public class SqlPersistence {
    private final List<Class<?>> SQLITE_LIST = new ArrayList<Class<?>>();
    private final List<Class<?>> AUTO_INCREMENT_LIST = new ArrayList<Class<?>>();
    private final List<ManyToMany> MANY_TO_MANY_LIST = new ArrayList<ManyToMany>();
    private final List<HasMany> HAS_MANY_LIST = new ArrayList<HasMany>();

    private final String mName;
    private final int mVersion;

    public SqlPersistence(String name, int version) {
        mName = name;
        mVersion = version;
    }

    public String getName() {
        return mName;
    }

    public int getVersion() {
        return mVersion;
    }

    /**
     * Register one or more classes to be added to the Sqlite model. All classes should have an ID which will be treated
     * as autoincrement if possible. If your class has a field called <code>id</code> then it will be automatically
     * taken as an autoincrement primary key; if your primary key field has another name, use the {@link PrimaryKey}
     * which will also allow you to specify whether the field is autoincrement or not.
     *
     * @param classes a list of classes to register
     */
    public void match(Class<?>... classes) {
        for (Class<?> theClass : classes) {
            if (!SQLITE_LIST.contains(theClass)) {
                SQLITE_LIST.add(theClass);
                boolean isAutoincrement = true;
                for (Field field : theClass.getDeclaredFields()) {
                    PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
                    if (primaryKey != null && !primaryKey.autoincrement()) {
                        isAutoincrement = false;
                        break;
                    }
                }
                if (isAutoincrement && !AUTO_INCREMENT_LIST.contains(theClass)) {
                    AUTO_INCREMENT_LIST.add(theClass);
                }
            }
        }
    }

    /**
     * Use this to register classes that shall no have an autoincrement primary key. Use it only when you have
     * no control over that kind of class. If you do have control over the class, you shall use the normal
     * {@link SqlPersistence#match(Class[])} method and the annotation {@link PrimaryKey} with the autoincrement
     * argument set to <code>false</code>.
     *
     * @param classes the classes to register
     */
    public void matchNotAutoIncrement(Class<?>... classes) {
        for (Class<?> type : classes) {
            if (!SQLITE_LIST.contains(type)) {
                SQLITE_LIST.add(type);
            }
        }
    }

    /**
     * Use this to register many-to-many relationships. You shall no pass repeated relations (including those that
     * has the same classes but in different order). Classes in the relation will be passed to the
     * {@link SqlPersistence#match(Class[])} method, which means that, by default, they will be
     * treated as tables with an autoincrement primary key; if you want to avoid this behavior, use the
     * {@link PrimaryKey} annotation and customize your primary key. If you do not have control over the classes and
     * want to avoid the autoincrement, use the alternative method {@link SqlPersistence#matchNotAutoIncrement(ManyToMany)}.
     *
     * @param manyToMany an instance containing the many-to-many relation
     */
    public void match(ManyToMany manyToMany) {
        // make sure there are no inverted many-to-many relations
        for (ManyToMany mtm : MANY_TO_MANY_LIST) {
            if ((mtm.getFirstRelation() == manyToMany.getSecondRelation() && mtm.getSecondRelation() == manyToMany.getFirstRelation()) ||
                    (mtm.getFirstRelation() == manyToMany.getFirstRelation() && mtm.getSecondRelation() == manyToMany.getSecondRelation())) {
                throw new IllegalStateException(String.format("Error adding '%s': there should not be two many-to-many relations with the same classes.", manyToMany));
            }
        }
        match(manyToMany.getFirstRelation(), manyToMany.getSecondRelation());
        if (!MANY_TO_MANY_LIST.contains(manyToMany)) {
            MANY_TO_MANY_LIST.add(manyToMany);
        }
    }

    /**
     * Use this to register many-to-many relationships. You shall no pass repeated relations (including those that
     * has the same classes but in different order). Classes in the relation will be passed to the
     * {@link SqlPersistence#matchNotAutoIncrement(Class[])} method. I recommend to use the
     * {@link SqlPersistence#match(ManyToMany)} and {@link PrimaryKey} annotation if you have control over the
     * classes to be registered.
     *
     * @param manyToMany an instance containing the many-to-many relation
     */
    public void matchNotAutoIncrement(ManyToMany manyToMany) {
        matchNotAutoIncrement(manyToMany.getFirstRelation(), manyToMany.getSecondRelation());
        if (!MANY_TO_MANY_LIST.contains(manyToMany)) {
            MANY_TO_MANY_LIST.add(manyToMany);
        }
    }

    /**
     * Registers a has-many relation. This will register the individual classes using the
     * {@link SqlPersistence#match(Class[])} method which means that those classes will be treated as autoincrement.
     * If you want to avoid this behavior, use the {@link PrimaryKey} annotation; if you do not have control
     * over the registered classes, use the {@link SqlPersistence#matchNotAutoIncrement(ManyToMany)}
     *
     * @param hasMany the has-many relationship to register.
     */
    public void match(HasMany hasMany) {
        Class<?> containerClass = hasMany.getContainerClass();
        Class<?> containedClass = hasMany.getContainedClass();
        // make sure there are no inverted has many relations
        for (HasMany hasManyRelation : HAS_MANY_LIST) {
            Class<?> currentContainerClass = hasManyRelation.getContainerClass();
            Class<?> currentContainedClass = hasManyRelation.getContainedClass();
            if (currentContainerClass == containedClass && currentContainedClass == containerClass) {
                throw new IllegalStateException("There should not be two has-many relations with the same classes. Use Many-To-Many");
            }
        }
        match(containedClass, containerClass);
        // add the has many relation to the list
        if (!HAS_MANY_LIST.contains(hasMany)) {
            HAS_MANY_LIST.add(hasMany);
        }
    }

    /**
     * Registers a has-many relation. This will register the individual classes using the
     * {@link SqlPersistence#matchNotAutoIncrement(Class[])} method which means that those classes do not have an
     * autoincrement primary key. If you have control over the classes to register, I recommend to use the
     * {@link SqlPersistence#match(HasMany)} method and the {@link PrimaryKey} annotation.
     *
     * @param hasMany the has-many relationship to register.
     */
    public void matchNotAutoIncrement(HasMany hasMany) {
        Class<?> containerClass = hasMany.getContainerClass();
        Class<?> containedClass = hasMany.getContainedClass();
        matchNotAutoIncrement(containerClass, containedClass);
        // make sure there are no inverted has many relations
        for (HasMany hasManyRelation : HAS_MANY_LIST) {
            Class<?> currentContainerClass = hasManyRelation.getContainerClass();
            Class<?> currentContainedClass = hasManyRelation.getContainedClass();
            if (currentContainerClass == containedClass &&
                    currentContainedClass == containerClass) {
                throw new IllegalStateException("There should not be two has-many relations with the same classes. Use Many-To-Many");
            }
        }
        // add the has many relation to the list
        if (!HAS_MANY_LIST.contains(hasMany)) {
            HAS_MANY_LIST.add(hasMany);
        }
    }

    /**
     * Returns the relationship of the specified classes
     *
     * @param theClass        a class
     * @param collectionClass another class
     * @return the type of relationship between two classes
     */
    Relationship getRelationship(Class<?> theClass, Class<?> collectionClass) {
        for (HasMany hasMany : HAS_MANY_LIST) {
            Class<?> containerClass = hasMany.getContainerClass();
            Class<?> containedClass = hasMany.getContainedClass();
            if (containerClass == theClass && containedClass == collectionClass) {
                return Relationship.HAS_MANY;
            }
        }

        for (ManyToMany manyToMany : MANY_TO_MANY_LIST) {
            if ((manyToMany.getFirstRelation() == theClass && manyToMany.getSecondRelation() == collectionClass) ||
                    (manyToMany.getSecondRelation() == theClass && manyToMany.getFirstRelation() == collectionClass)) {
                return Relationship.MANY_TO_MANY;
            }
        }
        return Relationship.UNKNOWN;
    }

    /**
     * Returns a relationship of the specified class. If it has two relations, it will return the
     * {@link Relationship#HAS_MANY}
     *
     * @param theClass a class
     * @return the type of relationship between this class and any other
     */
    Relationship getRelationship(Class<?> theClass) {
        for (HasMany hasMany : HAS_MANY_LIST) {
            Class<?> containerClass = hasMany.getContainerClass();
            if (containerClass == theClass) {
                return Relationship.HAS_MANY;
            }
        }

        for (ManyToMany manyToMany : MANY_TO_MANY_LIST) {
            if (manyToMany.getFirstRelation() == theClass || manyToMany.getSecondRelation() == theClass) {
                return Relationship.MANY_TO_MANY;
            }
        }
        return Relationship.UNKNOWN;
    }

    /**
     * @param clazz the class that we are checking whether belongs to another class
     * @return the class that clazz belongs to or null if not such relation
     */
    HasMany belongsTo(Class clazz) {
        for (HasMany hasMany : HAS_MANY_LIST) {
            Class<?> containedClass = hasMany.getContainedClass();
            if (containedClass == clazz) {
                return hasMany;
            }
        }
        return null;
    }

    /**
     * @param theClass the class to check
     * @return true if the class is registered as autoincrement
     */
    boolean isAutoincrement(Class<?> theClass) {
        return AUTO_INCREMENT_LIST.contains(theClass);
    }

    /**
     * @param theClass the table to search for
     * @return a list with the many-to-many relations of this table
     */
    List<ManyToMany> getManyToMany(Class<?> theClass) {
        List<ManyToMany> manyToManyList = new ArrayList<ManyToMany>();
        for (ManyToMany manyToMany : MANY_TO_MANY_LIST) {
            if (manyToMany.getFirstRelation() == theClass || manyToMany.getSecondRelation() == theClass) {
                manyToManyList.add(manyToMany);
            }
        }
        return manyToManyList;
    }

    /**
     * @param clazz the class that we are checking whether has another
     * @return the class that clazz has or null if not such relation
     */
    HasMany has(Class clazz) {
        for (HasMany hasMany : HAS_MANY_LIST) {
            Class<?> containerClass = hasMany.getContainerClass();
            if (containerClass == clazz) {
                return hasMany;
            }
        }
        return null;
    }

    enum PersistenceType {SQLITE, PREFERENCES, UNKNOWN}

    enum Relationship {MANY_TO_MANY, HAS_MANY, UNKNOWN}

    List<Class<?>> getSqliteClasses() {
        return SQLITE_LIST;
    }

    List<ManyToMany> getSqliteManyToMany() {
        return MANY_TO_MANY_LIST;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SqlPersistence that = (SqlPersistence) o;

        if (mVersion != that.mVersion) return false;
        if (AUTO_INCREMENT_LIST != null ? !AUTO_INCREMENT_LIST.equals(that.AUTO_INCREMENT_LIST) : that.AUTO_INCREMENT_LIST != null)
            return false;
        if (HAS_MANY_LIST != null ? !HAS_MANY_LIST.equals(that.HAS_MANY_LIST) : that.HAS_MANY_LIST != null)
            return false;
        if (MANY_TO_MANY_LIST != null ? !MANY_TO_MANY_LIST.equals(that.MANY_TO_MANY_LIST) : that.MANY_TO_MANY_LIST != null)
            return false;
        if (SQLITE_LIST != null ? !SQLITE_LIST.equals(that.SQLITE_LIST) : that.SQLITE_LIST != null) return false;
        if (mName != null ? !mName.equals(that.mName) : that.mName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = SQLITE_LIST != null ? SQLITE_LIST.hashCode() : 0;
        result = 31 * result + (AUTO_INCREMENT_LIST != null ? AUTO_INCREMENT_LIST.hashCode() : 0);
        result = 31 * result + (MANY_TO_MANY_LIST != null ? MANY_TO_MANY_LIST.hashCode() : 0);
        result = 31 * result + (HAS_MANY_LIST != null ? HAS_MANY_LIST.hashCode() : 0);
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        result = 31 * result + mVersion;
        return result;
    }

    @Override
    public String toString() {
        return "SqlPersistence{" +
                "name='" + mName + '\'' +
                ", version=" + mVersion +
                '}';
    }
}
