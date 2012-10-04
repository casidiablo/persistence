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

import android.content.*;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This basic implementation of a {@link ContentProvider} will allow you to easily expose your database
 * or use it from a {@link android.content.ContentResolver}. You will just need to provide the
 * database name and the authority to use. Since all methods of a content provider need work with
 * a {@link Uri} you will need to provide one with the following format:
 * <p/>
 * <pre>content://{authority}/{table_name}</pre>
 * <p/>
 * Since you may not know what the table name is, you can use the helper method
 * {@link BaseContentProvider#buildBaseUri(String, Class)} which will return the uri for the specified class.
 *
 * @author cristian
 */
public abstract class BaseContentProvider extends ContentProvider {

    private static UriMatcher sUriMatcher;
    private static final Map<Integer, String> TABLE_NAME_IDS = new HashMap<Integer, String>();

    private DatabaseSpec mDatabaseSpec;

    @Override
    public boolean onCreate() {
        mDatabaseSpec = PersistenceConfig.getDatabaseSpec(getDatabaseSpecId());
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // get the list of registered classes and add them to the matcher
        List<Class<?>> objects = mDatabaseSpec.getSqliteClasses();
        int id = 1;
        for (Class<?> theClass : objects) {
            String tableName = SQLHelper.getTableName(theClass);
            TABLE_NAME_IDS.put(id, tableName);
            sUriMatcher.addURI(getAuthority(), tableName, id);
            id++;
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int id = sUriMatcher.match(uri);
        if (!TABLE_NAME_IDS.containsKey(id)) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        String tableName = TABLE_NAME_IDS.get(id);
        Cursor cursor = getDatabase().query(tableName, projection, selection, selectionArgs, null, null, sortOrder, null);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        int id = sUriMatcher.match(uri);
        String tableName = TABLE_NAME_IDS.get(id);
        return "vnd.android.cursor.dir/vnd." + tableName.replace("_", ".");
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        int id = sUriMatcher.match(uri);
        if (!TABLE_NAME_IDS.containsKey(id)) {
            throw new IllegalArgumentException("Unknown URI " + uri + "; id " + id+"; "+TABLE_NAME_IDS);
        }

        if (initialValues == null) {
            initialValues = new ContentValues();
        }

        String tableName = TABLE_NAME_IDS.get(id);
        long rowId = getDatabase().insert(tableName, null, initialValues);
        if (rowId > 0) {
            Uri CONTENT_URI = Uri.parse(String.format("content://%s/%s", getAuthority(), tableName));
            Uri beanUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(beanUri, null);
            return beanUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        int id = sUriMatcher.match(uri);
        if (!TABLE_NAME_IDS.containsKey(id)) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        String tableName = TABLE_NAME_IDS.get(id);
        int count = getDatabase().delete(tableName, where, whereArgs);

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int id = sUriMatcher.match(uri);
        if (!TABLE_NAME_IDS.containsKey(id)) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        String tableName = TABLE_NAME_IDS.get(id);
        int count = getDatabase().update(tableName, values, where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private SQLiteDatabase getDatabase() {
        SqliteDb helper = SqliteDb.getInstance(getContext(), getDatabaseName(), mDatabaseSpec);
        return helper.getDatabase();
    }

    public static Uri buildBaseUri(String authority, Class<?> theClass) {
        return Uri.parse(String.format("content://%s/%s", authority, SQLHelper.getTableName(theClass)));
    }

    /**
     * @return the name of the database where the tables of this content provider are
     */
    public abstract String getDatabaseName();

    /**
     * @return the id of the database spec
     */
    public abstract String getDatabaseSpecId();

    /**
     * @return the authority that will be used in the Uri's
     */
    protected abstract String getAuthority();
}
