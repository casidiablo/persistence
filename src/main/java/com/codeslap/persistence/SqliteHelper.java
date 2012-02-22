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

import android.content.Context;

import java.util.List;

/**
 * This is a persitence helper. You must know that each time that you call a method from this class,
 * the database will be open and closed. So, if you need to do bulk tasks, consider using {@link SqlAdapter} instead
 */
public interface SqliteHelper {
    /**
     * Retrieves an object from the database
     *
     * @param context used to open the database
     * @param where   sample object
     * @param <T>     object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return the first found element using the passed sample
     */
    <T> T findFirst(Context context, T where);

    /**
     * Retrieves an object from the database
     *
     * @param context   used to open the database
     * @param clazz     the type of the object to retrieve
     * @param where     a SQL query. It is recommended to use wildcards like: <code>something = ? AND another = ?</code>
     * @param whereArgs the list of values used in the wildcards
     * @param <T>       object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return the first found element using the raw query parameters
     */
    <T> T findFirst(Context context, Class<T> clazz, String where, String[] whereArgs);

    /**
     * Deletes one or more elements from the database
     *
     * @param context used to open the database
     * @param where   sample object
     * @param <T>     object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return how many items were deleted
     */
    <T> int delete(Context context, T where);

    /**
     * Deletes one or more elements from the database
     *
     * @param context   used to open the database
     * @param where     a SQL query. It is recommended to use wildcards like: <code>something = ? AND another = ?</code>
     * @param whereArgs the list of values used in the wildcards
     * @param <T>       object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return how many items were deleted
     */
    <T> int delete(Context context, String where, String[] whereArgs);

    /**
     * Persist an object in the database
     *
     * @param context used to open the database
     * @param object  the object to insert into the database
     * @param <T>     object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return an object containing the ID of the inserted object
     */
    <T> Object store(Context context, T object);

    /**
     * Updates one of more records in the database
     *
     * @param context used to open the database
     * @param object  the object to insert into the database
     * @param where   the sample object
     * @param <T>     object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return how many items were updated
     */
    <T> int update(Context context, T object, T where);

    /**
     * Updates one of more records in the database
     *
     * @param context   used to open the database
     * @param object    the object to insert into the database
     * @param where     a SQL query. It is recommended to use wildcards like: <code>something = ? AND another = ?</code>
     * @param whereArgs the list of values used in the wildcards
     * @param <T>       object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return how many items were updated
     */
    <T> Object update(Context context, T object, String where, String[] whereArgs);

    /**
     * Persist a collection of objects into the database
     *
     * @param context    used to open the database
     * @param collection a collection with objects to insert
     * @param <T>        object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     */
    <T> void storeCollection(Context context, List<T> collection);

    /**
     * Persist a collection of objects into the database. It will delete the objects from the
     * database that are not in the collection.
     *
     * @param context    used to open the database
     * @param collection a collection with objects to insert
     * @param <T>        object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     */
    <T> void storeUniqueCollection(Context context, List<T> collection);

    /**
     * Retrieves all elements from the database of the specified type
     *
     * @param context used to open the database
     * @param clazz   the class of the object that we want to retrieve
     * @param <T>     object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return a list of {@link T} objects
     */
    <T> List<T> findAll(Context context, Class<T> clazz);

    /**
     * Retrieves all elements from the database that matches the specified sample
     *
     * @param context used to open the database
     * @param where   sample object
     * @param <T>     object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return a list of {@link T} objects
     */
    <T> List<T> findAllWhere(Context context, T where);

    /**
     * Retrieves all elements from the database that matches the specified sample
     *
     * @param context   used to open the database
     * @param where     a SQL query. It is recommended to use wildcards like: <code>something = ? AND another = ?</code>
     * @param whereArgs the list of values used in the wildcards
     * @param <T>       object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return a list of {@link T} objects
     */
    <T> List<T> findAllWhere(Context context, Class<T> clazz, String where, String[] whereArgs);

    /**
     * Retrieves all elements from the database that are attached to the specified object
     *
     * @param context    used to open the database
     * @param sample     the sample object
     * @param attachedTo the object that is attached to the sample object
     * @param <T>        object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return a list of {@link T} objects
     */
    <T, G> List<T> findAllWhere(Context context, T sample, G attachedTo);

    /**
     * Counts how many items there are in the database
     *
     * @param context used to open the database
     * @param clazz   the class of the object that we want to count
     * @param <T>     object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return number of elements in the table of the specified object
     */
    <T> int countAll(Context context, Class<T> clazz);

    /**
     * Counts how many items there are in the database and match the specified condition
     *
     * @param context used to open the database
     * @param where   the sample object
     * @param <T>     object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return number of elements in the table of the specified object
     */
    <T> int count(Context context, T where);

    /**
     * Counts how many items there are in the database and match the specified condition
     *
     * @param context   used to open the database
     * @param where     a SQL query. It is recommended to use wildcards like: <code>something = ? AND another = ?</code>
     * @param whereArgs the list of values used in the wildcards
     * @return number of elements in the table of the specified object
     */
    int count(Context context, String where, String[] whereArgs);
}
