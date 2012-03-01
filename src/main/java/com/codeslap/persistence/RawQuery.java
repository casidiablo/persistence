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

import android.database.Cursor;

/**
 * Methods in this interface will work like the findAll* methods in the {@link SqlAdapter} interface,
 * but instead of returning a {@link java.util.List} of beans it will return a cursor.
 *
 * @author cristian
 */
public interface RawQuery {
    /**
     * Retrieves all elements from the database of the specified type
     *
     * @param theClass the class of the object that we want to retrieve
     * @return a Cursor pointing to the filter rows
     */
    Cursor findAll(Class<?> theClass);

    /**
     * Retrieves all elements from the database that matches the specified sample
     *
     * @param where sample object
     * @return a Cursor pointing to the filter rows
     */
    Cursor findAll(Object where);

    /**
     * Retrieves all elements from the database that matches the specified sample. And follows the specified constraint.
     *
     * @param where      sample object
     * @param constraint constrains for this query
     * @return a Cursor pointing to the filter rows
     */
    Cursor findAll(Object where, Constraint constraint);

    /**
     * Retrieves all elements from the database that are attached to the specified object
     *
     * @param where      the sample object
     * @param attachedTo the object that is attached to the sample object
     * @return a Cursor pointing to the filter rows
     */
    Cursor findAll(Object where, Object attachedTo);

    /**
     * Retrieves all elements from the database that matches the specified sample
     *
     * @param theClass  the class to find all items
     * @param where     a SQL query. It is recommended to use wildcards like: <code>something = ? AND another = ?</code>
     * @param whereArgs the list of values used in the wildcards
     * @return a Cursor pointing to the filter rows
     */
    Cursor findAll(Class<?> theClass, String where, String[] whereArgs);

    /**
     * @param rawQuery an SQL query to execute
     * @return A Cursor object, which is positioned before the first entry. Note that Cursors are not synchronized,
     *         see the documentation for more details.
     */
    Cursor rawQuery(String rawQuery);

    /**
     * @param table         The table name to compile the query against.
     * @param projection    A list of which columns to return. Passing null will return all columns, which is
     *                      discouraged to prevent reading data from storage that isn't going to be used.
     * @param selection     A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the
     *                      WHERE itself). Passing null will return all rows for the given table.
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values from selectionArgs,
     *                      in order that they appear in the selection. The values will be bound as Strings.
     * @param groupBy       A filter declaring how to group rows, formatted as an SQL GROUP BY clause (excluding the
     *                      GROUP BY itself). Passing null will cause the rows to not be grouped.
     * @param having        A filter declare which row groups to include in the cursor, if row grouping is being used,
     *                      formatted as an SQL HAVING clause (excluding the HAVING itself). Passing null will cause all
     *                      row groups to be included, and is required when row grouping is not being used.
     * @param sortOrder     How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself).
     *                      Passing null will use the default sort order, which may be unordered.
     * @param limit         Limits the number of rows returned by the query, formatted as LIMIT clause. Passing null
     *                      denotes no LIMIT clause.
     * @return A Cursor object, which is positioned before the first entry. Note that Cursors are not synchronized,
     *         see the documentation for more details.
     */
    Cursor findAll(String table, String[] projection, String selection, String[] selectionArgs, String groupBy,
                   String having, String sortOrder, String limit);
}
