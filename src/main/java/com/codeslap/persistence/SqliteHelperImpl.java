package com.codeslap.persistence;

import android.content.Context;

import java.util.List;

public class SqliteHelperImpl implements SqliteHelper {
    @Override
    public <T> T findFirst(Context context, T where) {
        return null;
    }

    @Override
    public <T> T findFirst(Context context, String where, String[] whereArgs) {
        return null;
    }

    @Override
    public <T> int delete(Context context, T where) {
        return 0;
    }

    @Override
    public <T> int delete(Context context, String where, String[] whereArgs) {
        return 0;
    }

    @Override
    public <T> Object store(Context context, T object) {
        return null;
    }

    @Override
    public <T> int update(Context context, T object, T where) {
        return 0;
    }

    @Override
    public <T> Object update(Context context, T object, String where, String[] whereArgs) {
        return null;
    }

    @Override
    public <T> void storeCollection(Context context, List<T> collection) {
    }

    @Override
    public <T> void storeUniqueCollection(Context context, List<T> collection) {
    }

    @Override
    public <T> List<T> findAll(Context context, Class<T> clazz) {
        return null;
    }

    @Override
    public <T> List<T> findAllWhere(Context context, T where) {
        return null;
    }

    @Override
    public <T> List<T> findAllWhere(Context context, Class<T> clazz, String where, String[] whereArgs) {
        return null;
    }

    @Override
    public <T, G> List<T> findAllWhere(Context context, T sample, G attachedTo) {
        return null;
    }

    @Override
    public <T> int countAll(Context context, Class<T> clazz) {
        return 0;
    }

    @Override
    public <T> int count(Context context, T where) {
        return 0;
    }

    @Override
    public int count(Context context, String where, String[] whereArgs) {
        return 0;
    }
}
