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

import java.util.ArrayList;
import java.util.List;

/**
 * @author cristian
 */
@SuppressWarnings("UnusedDeclaration")
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

    public void match(Class<?>... types) {
        for (Class<?> type : types) {
            if (!SQLITE_LIST.contains(type)) {
                SQLITE_LIST.add(type);
                AUTO_INCREMENT_LIST.add(type);
            }
        }
    }

    public void matchNotAutoIncrement(Class<?>... types) {
        for (Class<?> type : types) {
            if (!SQLITE_LIST.contains(type)) {
                SQLITE_LIST.add(type);
            }
        }
    }

    public void match(ManyToMany manyToMany) {
        match(manyToMany.getClasses());
        if (!MANY_TO_MANY_LIST.contains(manyToMany)) {
            MANY_TO_MANY_LIST.add(manyToMany);
        }
    }

    public void matchNotAutoIncrement(ManyToMany manyToMany) {
        matchNotAutoIncrement(manyToMany.getClasses());
        if (!MANY_TO_MANY_LIST.contains(manyToMany)) {
            MANY_TO_MANY_LIST.add(manyToMany);
        }
    }

    public void match(HasMany hasMany) {
        Class<?>[] classes = hasMany.getClasses();
        match(classes);
        // make sure there are no inverted has many relations
        for (HasMany hasManyRelation : HAS_MANY_LIST) {
            Class<?>[] relationClasses = hasManyRelation.getClasses();
            if (relationClasses[0] == classes[1] && relationClasses[1] == classes[0] && hasMany.getThrough().equals(hasManyRelation.getThrough())) {
                throw new IllegalStateException("There should not be two has-many relations with the same classes. Use Many-To-Many");
            }
        }
        // add the has many relation to the list
        if (!HAS_MANY_LIST.contains(hasMany)) {
            HAS_MANY_LIST.add(hasMany);
        }
    }

    public void matchNotAutoIncrement(HasMany hasMany) {
        Class<?>[] classes = hasMany.getClasses();
        matchNotAutoIncrement(classes);
        // make sure there are no inverted has many relations
        for (HasMany hasManyRelation : HAS_MANY_LIST) {
            Class<?>[] relationClasses = hasManyRelation.getClasses();
            if (relationClasses[0] == classes[1] && relationClasses[1] == classes[0] && hasMany.getThrough().equals(hasManyRelation.getThrough())) {
                throw new IllegalStateException("There should not be two has-many relations with the same classes. Use Many-To-Many");
            }
        }
        // add the has many relation to the list
        if (!HAS_MANY_LIST.contains(hasMany)) {
            HAS_MANY_LIST.add(hasMany);
        }
    }

    /**
     * @param theClass        a class
     * @param collectionClass another class
     * @return the type of relationship between two classes
     */
    Relationship getRelationship(Class<?> theClass, Class<?> collectionClass) {
        for (HasMany hasMany : HAS_MANY_LIST) {
            Class<?>[] classes = hasMany.getClasses();
            if (classes[0] == theClass && classes[1] == collectionClass) {
                return Relationship.HAS_MANY;
            }
        }

        for (ManyToMany manyToMany : MANY_TO_MANY_LIST) {
            Class<?>[] classes = manyToMany.getClasses();
            if ((classes[0] == theClass && classes[1] == collectionClass) ||
                    (classes[1] == theClass && classes[0] == collectionClass)) {
                return Relationship.MANY_TO_MANY;
            }
        }
        return Relationship.UNKNOWN;
    }

    /**
     * @param theClass a class
     * @return the type of relationship between this class and any other
     */
    Relationship getRelationship(Class<?> theClass) {
        for (HasMany hasMany : HAS_MANY_LIST) {
            Class<?>[] classes = hasMany.getClasses();
            if (classes[0] == theClass) {
                return Relationship.HAS_MANY;
            }
        }

        for (ManyToMany manyToMany : MANY_TO_MANY_LIST) {
            Class<?>[] classes = manyToMany.getClasses();
            if (classes[0] == theClass || classes[1] == theClass) {
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
            Class<?>[] classes = hasMany.getClasses();
            if (classes[1] == clazz) {
                return hasMany;
            }
        }
        return null;
    }

    /**
     * @param theClass the table to search for
     * @return a list with the many-to-many relations of this table
     */
    List<ManyToMany> getManyToMany(Class<?> theClass) {
        List<ManyToMany> manyToManyList = new ArrayList<ManyToMany>();
        for (ManyToMany manyToMany : MANY_TO_MANY_LIST) {
            Class<?>[] classes = manyToMany.getClasses();
            if (classes[0] == theClass || classes[1] == theClass) {
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
            Class<?>[] classes = hasMany.getClasses();
            if (classes[0] == clazz) {
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

    List<Class<?>> getAutoIncrementList() {
        return AUTO_INCREMENT_LIST;
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
}
