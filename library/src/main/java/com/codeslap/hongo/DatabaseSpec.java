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

package com.codeslap.hongo;

import android.content.Context;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that defines a database (what tables it has, what are they relationships and how it is
 * created/upgraded)
 *
 * @author cristian
 */
public class DatabaseSpec {
  private final int mVersion;
  private final List<DataObject<?>> mDataObjects = new ArrayList<DataObject<?>>();
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

  public int getVersion() {
    return mVersion;
  }

  /**
   * Register one or more classes to be added to the Sqlite model. All classes should have an ID
   * which will be treated as autoincrement if possible. If your class has a field called
   * <code>id</code> then it will be automatically taken as an autoincrement primary key; if your
   * primary key field has another name, use the {@link PrimaryKey} which will also allow you to
   * specify whether the field is autoincrement or not.
   *
   * @param classes a list of classes to register
   */
  public void match(Class<?>... classes) {
    for (Class<?> theClass : classes) {
      DataObject<?> dataObject = DataObjectFactory.getDataObject(theClass);
      if (!mDataObjects.contains(dataObject)) {
        mDataObjects.add(dataObject);
      }
    }
  }

  /**
   * Adds one or more importers from the file paths specified. This is executed before tables are
   * created.
   *
   * @param context used to get the content of the assets
   * @param paths one or more file paths relative to the Assets folder
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
   * @param inputStream the input stream must not be null and must point to sqlite statements to
   * execute
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
   * Adds one or more importers from the file paths specified. This is executed before tables are
   * created. Executes the specified sql statements after tables are created.
   *
   * @param context used to get the content of the assets
   * @param paths one or more file paths relative to the Assets folder
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
   * @param inputStream the input stream must not be null and must point to sqlite statements to
   * execute
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

  List<DataObject<?>> getDataObjects() {
    return mDataObjects;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DatabaseSpec that = (DatabaseSpec) o;

    if (mVersion != that.mVersion) return false;
    if (!mDataObjects.equals(that.mDataObjects)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = mDataObjects.hashCode();
    result = 31 * result + mVersion;
    return result;
  }

  @Override
  public String toString() {
    return "DatabaseSpec{" +
        "mSqliteList=" + mDataObjects +
        ", mVersion=" + mVersion +
        ", mBeforeImporters=" + mBeforeImporters +
        ", mAfterImporters=" + mAfterImporters +
        '}';
  }
}
