package com.egoclean.persistence;

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

    private static final String DB_NAME = "persistence.db";
    private static final int DB_VERSION = 1;
    private SQLiteDatabase sqLiteDatabase;
    private final Context mContext;
    private Helper dbHelper;

    private static class Helper extends SQLiteOpenHelper {

        public Helper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            // create all tables for registered daos
            List<Class<?>> objects = Persistence.getSqliteClasses();
            for (Class<?> clazz : objects) {
                sqLiteDatabase.execSQL(SQLHelper.getCreateTableSentence(clazz));
            }
            // create all extra table for many to many relations
            List<ManyToMany> sqliteManyToMany = Persistence.getSqliteManyToMany();
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

    public SqliteDb open() {
        dbHelper = new Helper(mContext);
        sqLiteDatabase = dbHelper.getWritableDatabase();
        // create new tables without increasing db version
        dbHelper.onCreate(sqLiteDatabase);
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public SQLiteDatabase getWritableDatabase() {
        return sqLiteDatabase;
    }
}
