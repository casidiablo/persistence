package com.codeslap.persistence;

import android.content.SharedPreferences;

/**
 * This persistence is aimed to persist and retrieve single objects to and from the
 * default shared preferences.
 * Important: daos of this class will work with single beans only, not collections.
 * For collections-able persistence take a look at the sqlite.SqliteDao class
 */
class PrefDao {

    private final SharedPreferences mPreferences;

    PrefDao(SharedPreferences prefs) {
        mPreferences = prefs;
    }

    public <T> void persist(T bean) {
        SharedPreferences.Editor editor = mPreferences.edit();
        fillEditor(bean.getClass(), editor, bean);
        editor.commit();
    }

    public <T> T find(Class<? extends T> clazz) {
        return getBeanFromPreferences(clazz, mPreferences);
    }

    public <T> void delete(Class<? extends T> clazz) {
        SharedPreferences.Editor editor = mPreferences.edit();
        clear(clazz, editor);
        editor.commit();
    }

    protected <T> void fillEditor(Class<? extends T> clazz, SharedPreferences.Editor editor, T bean){}

    protected <T> T getBeanFromPreferences(Class<? extends T> clazz, SharedPreferences preferences){
        return null;
    }

    protected <T> void clear(Class<? extends T> clazz, SharedPreferences.Editor editor){
        
    }
}
