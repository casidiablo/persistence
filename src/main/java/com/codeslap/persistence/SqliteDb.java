package com.codeslap.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

/**
 * Class that will hold and manage the SQLiteDatabase object, needed to
 * persist and retrieve data. This will also create the database if it's not
 * created yet, or update it in case the version number changes.
 */
class SqliteDb {

    public static final String TAG = SqliteDb.class.getSimpleName();
    private SQLiteDatabase mSqLiteDatabase;
    private final Context mContext;
    private Helper mDbHelper;

    private static class Helper extends SQLiteOpenHelper {

        private final String mName;

        public Helper(Context context, String name, int version) {
            super(context, name, null, version);
            mName = name;
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            // create all tables for registered daos
            List<Class<?>> objects = Persistence.getDatabase(mName).getSqliteClasses();
            for (Class<?> clazz : objects) {
                sqLiteDatabase.execSQL(SQLHelper.getCreateTableSentence(mName, clazz));
            }
            // create all extra table for many to many relations
            List<ManyToMany> sqliteManyToMany = Persistence.getDatabase(mName).getSqliteManyToMany();
            for (ManyToMany manyToMany : sqliteManyToMany) {
                sqLiteDatabase.execSQL(manyToMany.getCreateTableStatement());
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            // TODO define a way to migrate data
        }

    }

    public SqliteDb(Context context) {
        mContext = context;
    }

    public SqliteDb open(String name, int version) {
        mDbHelper = new Helper(mContext, name, version);
        mSqLiteDatabase = mDbHelper.getWritableDatabase();
        PersistenceLogManager.d(TAG, String.format("Opening '%s' database... Open: %s", name, mSqLiteDatabase.isOpen()));
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    public SQLiteDatabase getWritableDatabase() {
        return mSqLiteDatabase;
    }
}
