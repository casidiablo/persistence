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
            List<Class> objects = DaoFactory.getRegisteredObjects();
            for (Class clazz : objects) {
                sqLiteDatabase.execSQL(SQLHelper.getCreateTableSentence(clazz));
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            sqLiteDatabase.execSQL("DROP TABLE " + DbSchema.Media.TABLE_NAME);
            sqLiteDatabase.execSQL("DROP TABLE " + DbSchema.Show.TABLE_NAME);
            sqLiteDatabase.execSQL("DROP TABLE " + DbSchema.Likes.TABLE_NAME);
            onCreate(sqLiteDatabase);
        }

    }

    public SqliteDb(Context context) {
        mContext = context;
    }

    public SqliteDb open() {
        dbHelper = new Helper(mContext);
        sqLiteDatabase = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public SQLiteDatabase getWritableDatabase() {
        return sqLiteDatabase;
    }
}
