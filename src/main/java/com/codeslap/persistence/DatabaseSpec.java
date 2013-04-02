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

import android.content.Context;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that defines a database (what tables it has, what are they relationships and how it is created/upgraded)
 *
 * @author cristian
 */
public class DatabaseSpec {
    private final List<Class<?>> mSqliteList = new ArrayList<Class<?>>();
    private final List<Class<?>> mAutoIncrementList = new ArrayList<Class<?>>();
    private final List<ManyToMany> mManyToManyList = new ArrayList<ManyToMany>();
    private final List<HasMany> mHasManyList = new ArrayList<HasMany>();

    private final int mVersion;
    private final List<Importer> mBeforeImporters = new ArrayList<Importer>();
    private final List<Importer> mAfterImporters = new ArrayList<Importer>();
    DbOpenHelperBuilder mDbOpenHelperBuilder;

    DatabaseSpec(int version) {
        mVersion = version;
    }

    /**
     * Sets a {@link DbOpenHelper} builder. Use this if you want to provide a custom way of
     * creating/upgrading the database
     *
     * @param dbOpenHelperBuilder the {@link DbOpenHelperBuilder} implementation
     * @return instance of current {@link DatabaseSpec} object
     */
    public DatabaseSpec setDbOpenHelperBuilder(DbOpenHelperBuilder dbOpenHelperBuilder) {
        mDbOpenHelperBuilder = dbOpenHelperBuilder;
        return this;
    }

    /**
     * Use this to create a new {@link DbOpenHelper} implementation
     */
    public static interface DbOpenHelperBuilder {
        /**
         * This method must return a new {@link DbOpenHelper} implementation always.
         *
         * @param context the context used to create the open helper
         * @param name    the name to provide to the open helper (database name)
         * @param version the version to provide to the open helper (database name)
         * @return new {@link DbOpenHelper} implementation
         */
        DbOpenHelper buildOpenHelper(Context context, String name, int version);
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
            if (!mSqliteList.contains(theClass)) {
                mSqliteList.add(theClass);
                boolean isAutoincrement = true;
                Field pk = SQLHelper.getPrimaryKeyField(theClass);
                if (pk.getType() == String.class ||
                        pk.getType() == Boolean.class || pk.getType() == boolean.class ||
                        pk.getType() == Float.class || pk.getType() == float.class ||
                        pk.getType() == Double.class || pk.getType() == double.class) {
                    isAutoincrement = false;
                } else {
                    for (Field field : SQLHelper.getDeclaredFields(theClass)) {
                        PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
                        if (primaryKey != null && !primaryKey.autoincrement()) {
                            isAutoincrement = false;
                            break;
                        }
                    }
                }
                if (isAutoincrement && !mAutoIncrementList.contains(theClass)) {
                    mAutoIncrementList.add(theClass);
                }
            }
        }
    }

    /**
     * Use this to register classes that shall no have an autoincrement primary key. Use it only when you have
     * no control over that kind of class. If you do have control over the class, you shall use the normal
     * {@link DatabaseSpec#match(Class[])} method and the annotation {@link PrimaryKey} with the autoincrement
     * argument set to <code>false</code>.
     *
     * @param classes the classes to register
     */
    public void matchNotAutoIncrement(Class<?>... classes) {
        for (Class<?> type : classes) {
            if (!mSqliteList.contains(type)) {
                mSqliteList.add(type);
            }
        }
    }

    /**
     * Use this to register many-to-many relationships. You shall no pass repeated relations (including those that
     * has the same classes but in different order). Classes in the relation will be passed to the
     * {@link DatabaseSpec#match(Class[])} method, which means that, by default, they will be
     * treated as tables with an autoincrement primary key; if you want to avoid this behavior, use the
     * {@link PrimaryKey} annotation and customize your primary key. If you do not have control over the classes and
     * want to avoid the autoincrement, use the alternative method {@link DatabaseSpec#matchNotAutoIncrement(ManyToMany)}.
     *
     * @param manyToMany an instance containing the many-to-many relation
     */
    public void match(ManyToMany manyToMany) {
        // make sure there are no inverted many-to-many relations
        for (ManyToMany mtm : mManyToManyList) {
            if ((mtm.getFirstRelation() == manyToMany.getSecondRelation() && mtm.getSecondRelation() == manyToMany.getFirstRelation()) ||
                    (mtm.getFirstRelation() == manyToMany.getFirstRelation() && mtm.getSecondRelation() == manyToMany.getSecondRelation())) {
                throw new IllegalStateException(String.format("Error adding '%s': there should not be two many-to-many relations with the same classes.", manyToMany));
            }
        }
        match(manyToMany.getFirstRelation(), manyToMany.getSecondRelation());
        if (!mManyToManyList.contains(manyToMany)) {
            mManyToManyList.add(manyToMany);
        }
    }

    /**
     * Use this to register many-to-many relationships. You shall no pass repeated relations (including those that
     * has the same classes but in different order). Classes in the relation will be passed to the
     * {@link DatabaseSpec#matchNotAutoIncrement(Class[])} method. I recommend to use the
     * {@link DatabaseSpec#match(ManyToMany)} and {@link PrimaryKey} annotation if you have control over the
     * classes to be registered.
     *
     * @param manyToMany an instance containing the many-to-many relation
     */
    public void matchNotAutoIncrement(ManyToMany manyToMany) {
        matchNotAutoIncrement(manyToMany.getFirstRelation(), manyToMany.getSecondRelation());
        if (!mManyToManyList.contains(manyToMany)) {
            mManyToManyList.add(manyToMany);
        }
    }

    /**
     * Registers a has-many relation. This will register the individual classes using the
     * {@link DatabaseSpec#match(Class[])} method which means that those classes will be treated as autoincrement.
     * If you want to avoid this behavior, use the {@link PrimaryKey} annotation; if you do not have control
     * over the registered classes, use the {@link DatabaseSpec#matchNotAutoIncrement(ManyToMany)}
     *
     * @param hasMany the has-many relationship to register.
     */
    public void match(HasMany hasMany) {
        Class<?> containerClass = hasMany.getContainerClass();
        Class<?> containedClass = hasMany.getContainedClass();
        // make sure there are no inverted has many relations
        for (HasMany hasManyRelation : mHasManyList) {
            Class<?> currentContainerClass = hasManyRelation.getContainerClass();
            Class<?> currentContainedClass = hasManyRelation.getContainedClass();
            if (currentContainerClass == containedClass && currentContainedClass == containerClass) {
                throw new IllegalStateException("There should not be two has-many relations with the same classes. Use Many-To-Many");
            }
        }
        match(containedClass, containerClass);
        // add the has many relation to the list
        if (!mHasManyList.contains(hasMany)) {
            mHasManyList.add(hasMany);
        }
    }

    /**
     * Registers a has-many relation. This will register the individual classes using the
     * {@link DatabaseSpec#matchNotAutoIncrement(Class[])} method which means that those classes do not have an
     * autoincrement primary key. If you have control over the classes to register, I recommend to use the
     * {@link DatabaseSpec#match(HasMany)} method and the {@link PrimaryKey} annotation.
     *
     * @param hasMany the has-many relationship to register.
     */
    public void matchNotAutoIncrement(HasMany hasMany) {
        Class<?> containerClass = hasMany.getContainerClass();
        Class<?> containedClass = hasMany.getContainedClass();
        matchNotAutoIncrement(containerClass, containedClass);
        // make sure there are no inverted has many relations
        for (HasMany hasManyRelation : mHasManyList) {
            Class<?> currentContainerClass = hasManyRelation.getContainerClass();
            Class<?> currentContainedClass = hasManyRelation.getContainedClass();
            if (currentContainerClass == containedClass &&
                    currentContainedClass == containerClass) {
                throw new IllegalStateException("There should not be two has-many relations with the same classes. Use Many-To-Many");
            }
        }
        // add the has many relation to the list
        if (!mHasManyList.contains(hasMany)) {
            mHasManyList.add(hasMany);
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
        for (HasMany hasMany : mHasManyList) {
            Class<?> containerClass = hasMany.getContainerClass();
            Class<?> containedClass = hasMany.getContainedClass();
            if (containerClass == theClass && containedClass == collectionClass) {
                return Relationship.HAS_MANY;
            }
        }

        for (ManyToMany manyToMany : mManyToManyList) {
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
        for (HasMany hasMany : mHasManyList) {
            Class<?> containerClass = hasMany.getContainerClass();
            if (containerClass == theClass) {
                return Relationship.HAS_MANY;
            }
        }

        for (ManyToMany manyToMany : mManyToManyList) {
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
        for (HasMany hasMany : mHasManyList) {
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
        return mAutoIncrementList.contains(theClass);
    }

    /**
     * @param theClass the table to search for
     * @return a list with the many-to-many relations of this table
     */
    List<ManyToMany> getManyToMany(Class<?> theClass) {
        List<ManyToMany> manyToManyList = new ArrayList<ManyToMany>();
        for (ManyToMany manyToMany : mManyToManyList) {
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
        for (HasMany hasMany : mHasManyList) {
            Class<?> containerClass = hasMany.getContainerClass();
            if (containerClass == clazz) {
                return hasMany;
            }
        }
        return null;
    }

    /**
     * Adds one or more importers from the file paths specified. This is executed before tables are created.
     *
     * @param context used to get the content of the assets
     * @param paths   one or more file paths relative to the Assets folder
     */
    public void beforeCreateImportFromAssets(Context context, String... paths) {
        if (paths.length == 0) {
            throw new IllegalStateException("You should specify at lease one path");
        }
        for (String path : paths) {
            mBeforeImporters.add(new AssetsImporter(context, path));
        }
    }

    /**
     * Adds an importer from a stream. This is executed before tables are created.
     *
     * @param inputStream the input stream must not be null and must point to sqlite statements to execute
     */
    public void beforeCreateImportFromStream(InputStream inputStream) {
        mBeforeImporters.add(new StreamImporter(inputStream));
    }

    /**
     * Execute sqlite statements before tables are created.
     *
     * @param sqlStatements the statements to execute
     */
    public void beforeCreateImportFromString(String sqlStatements) {
        mBeforeImporters.add(new RawImporter(sqlStatements));
    }

    /**
     * Adds one or more importers from the file paths specified. This is executed before tables are created.
     * Executes the specified sql statements after tables are created.
     *
     * @param context used to get the content of the assets
     * @param paths   one or more file paths relative to the Assets folder
     */
    public void afterCreateImportFromAssets(Context context, String... paths) {
        if (paths.length == 0) {
            throw new IllegalStateException("You should specify at lease one path");
        }
        for (String path : paths) {
            mAfterImporters.add(new AssetsImporter(context, path));
        }
    }

    /**
     * Adds an importer from a stream. This is executed after tables are created.
     *
     * @param inputStream the input stream must not be null and must point to sqlite statements to execute
     */
    public void afterCreateImportFromStream(InputStream inputStream) {
        mAfterImporters.add(new StreamImporter(inputStream));
    }

    /**
     * Execute sqlite statements after tables are created.
     *
     * @param sqlStatements the statements to execute
     */
    public void afterCreateImportFromString(String sqlStatements) {
        mAfterImporters.add(new RawImporter(sqlStatements));
    }

    List<Importer> getAfterImporters() {
        return mAfterImporters;
    }

    List<Importer> getBeforeImporters() {
        return mBeforeImporters;
    }

    enum Relationship {MANY_TO_MANY, HAS_MANY, UNKNOWN}

    List<Class<?>> getSqliteClasses() {
        return mSqliteList;
    }

    List<ManyToMany> getSqliteManyToMany() {
        return mManyToManyList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DatabaseSpec that = (DatabaseSpec) o;

        if (mVersion != that.mVersion) return false;
        if (!mAutoIncrementList.equals(that.mAutoIncrementList))
            return false;
        if (!mHasManyList.equals(that.mHasManyList))
            return false;
        if (!mManyToManyList.equals(that.mManyToManyList))
            return false;
        if (!mSqliteList.equals(that.mSqliteList)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mSqliteList.hashCode();
        result = 31 * result + (mAutoIncrementList.hashCode());
        result = 31 * result + (mManyToManyList.hashCode());
        result = 31 * result + (mHasManyList.hashCode());
        result = 31 * result + mVersion;
        return result;
    }

    @Override
    public String toString() {
        return "DatabaseSpec{" +
                "mSqliteList=" + mSqliteList +
                ", mAutoIncrementList=" + mAutoIncrementList +
                ", mManyToManyList=" + mManyToManyList +
                ", mHasManyList=" + mHasManyList +
                ", mVersion=" + mVersion +
                ", mBeforeImporters=" + mBeforeImporters +
                ", mAfterImporters=" + mAfterImporters +
                '}';
    }

    /**
     * Returns the List of classes that have been added to the DatabaseSpec, useful for
     * operations like Database truncate.
     * @return List<Class?>
     */
    public List<Class<?>> getMatches(){
        return mSqliteList;
    }
}
