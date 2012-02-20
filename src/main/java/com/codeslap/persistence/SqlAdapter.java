package com.codeslap.persistence;

import java.util.List;

public interface SqlAdapter {
    /**
     * Persist an object in the database
     *
     * @param object the object to insert into the database
     * @param <T>    object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return an object containing the ID of the inserted object
     */
    <T> Object store(T object);

    /**
     * Persist an object in the database attached to another object
     *
     * @param bean       the object to insert into the database
     * @param attachedTo the object that bean is attached to
     * @param <T>        object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @param <G>        attached type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return an object containing the ID of the inserted object
     */
    <T, G> Object store(T bean, G attachedTo);

    /**
     * Persist a collection of objects into the database
     *
     * @param collection a collection with objects to insert
     * @param listener   callback to notify progress. Can be null.
     */
    <T> void storeCollection(List<T> collection, ProgressListener listener);

    /**
     * Persist a collection of objects into the database
     *
     * @param collection a collection with objects to insert
     * @param listener   callback to notify progress. Can be null.
     * @param attachedTo the object that each bean of this collection is attached to
     */
    <T, G> void storeCollection(List<T> collection, G attachedTo, ProgressListener listener);

    /**
     * Persist a collection of objects into the database. It will delete the objects from the
     * database that are not in the collection.
     *
     * @param collection a collection with objects to insert
     * @param listener   callback to notify progress. Can be null.
     */
    <T> void storeUniqueCollection(List<T> collection, ProgressListener listener);

    /**
     * Updates one of more records in the database
     *
     * @param object the object to insert into the database
     * @param where  the sample object
     * @param <T>    object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return how many items were updated
     */
    <T> int update(T object, T where);

    /**
     * Updates one of more records in the database
     *
     * @param object    the object to insert into the database
     * @param where     a SQL query. It is recommended to use wildcards like: <code>something = ? AND another = ?</code>
     * @param whereArgs the list of values used in the wildcards
     * @param <T>       object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return how many items were updated
     */
    <T> int update(T object, String where, String[] whereArgs);

    /**
     * Retrieves an object from the database
     *
     * @param where sample object
     * @param <T>   object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return the first found element using the passed sample
     */
    <T> T findFirst(T where);

    /**
     * Retrieves an object from the database
     *
     * @param clazz     the type of the object to retrieve
     * @param where     a SQL query. It is recommended to use wildcards like: <code>something = ? AND another = ?</code>
     * @param whereArgs the list of values used in the wildcards
     * @param <T>       object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return the first found element using the raw query parameters
     */
    <T> T findFirst(Class<T> clazz, String where, String[] whereArgs);

    /**
     * Retrieves all elements from the database of the specified type
     *
     * @param clazz the class of the object that we want to retrieve
     * @param <T>   object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return a list of {@link T} objects
     */
    <T> List<T> findAll(Class<T> clazz);

    /**
     * Retrieves all elements from the database that matches the specified sample
     *
     * @param where sample object
     * @param <T>   object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return a list of {@link T} objects
     */
    <T> List<T> findAll(T where);

    /**
     * Retrieves all elements from the database that matches the specified sample. And follows the specified constraint.
     *
     * @param where      sample object
     * @param constraint constrains for this query
     * @param <T>        object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return a list of {@link T} objects
     */
    <T> List<T> findAll(T where, Constraint constraint);

    /**
     * Retrieves all elements from the database that are attached to the specified object
     *
     * @param where      the sample object
     * @param attachedTo the object that is attached to the sample object
     * @param <T>        object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return a list of {@link T} objects
     */
    <T, G> List<T> findAll(T where, G attachedTo);

    /**
     * Retrieves all elements from the database that matches the specified sample
     *
     * @param clazz     the class to find all items
     * @param where     a SQL query. It is recommended to use wildcards like: <code>something = ? AND another = ?</code>
     * @param whereArgs the list of values used in the wildcards
     * @param <T>       object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return a list of {@link T} objects
     */
    <T> List<T> findAll(Class<T> clazz, String where, String[] whereArgs);

    /**
     * Deletes one or more elements from the database
     *
     * @param where sample object
     * @param <T>   object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return how many items were deleted
     */
    <T> int delete(T where);

    /**
     * Deletes one or more elements from the database
     *
     * @param clazz     the type of the object to delete
     * @param where     a SQL query. It is recommended to use wildcards like: <code>something = ? AND another = ?</code>
     * @param whereArgs the list of values used in the wildcards
     * @param <T>       object type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return how many items were deleted
     */
    <T> int delete(Class<T> clazz, String where, String[] whereArgs);

    /**
     * Counts how many items there are in the database and match the specified condition
     *
     * @param where the sample object
     * @param <T>   object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return number of elements in the table of the specified object
     */
    <T> int count(T where);

    /**
     * Counts how many items there are in the database and match the specified condition
     *
     * @param clazz     the class of the object that we want to count
     * @param where     a SQL query. It is recommended to use wildcards like: <code>something = ? AND another = ?</code>
     * @param whereArgs the list of values used in the wildcards
     * @param <T>       object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return number of elements in the table of the specified object
     */
    <T> int count(Class<T> clazz, String where, String[] whereArgs);

    /**
     * Counts how many items there are in the database
     *
     * @param clazz the class of the object that we want to count
     * @param <T>   object  type. Must be already registered using {@link SqlPersistence#match(Class[])}
     * @return number of elements in the table of the specified object
     */
    <T> int count(Class<T> clazz);

    /**
     * Closes the database
     */
    void close();

    /**
     * Callback used when storing a collection to notify the progress.
     * Note: when doing a bulk insert, we use the BEGIN TRANSACTION; ...; COMMIT; technique. So, if you are inserting
     * 99 records, each record will consume 1% and the COMMIT phase another 1%.
     */
    interface ProgressListener {
        /**
         * Called each time the progress percentage changes
         *
         * @param percentage the current progress
         */
        void onProgressChange(int percentage);
    }
}