package com.codeslap.persistence;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Field;

/**
 * This adapter is used to persist and retrieve single beans. This is an alternative
 * to the SqliteAdapter which is more useful when we want to save collection of
 * beans that can be organized in tables.
 */
class PrefsAdapterImpl implements PreferencesAdapter {

    private final SharedPreferences mPreferences;
    private final String mName;

    public PrefsAdapterImpl(Context context, String name) {
        mName = name;
        mPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    @Override
    public <T> void store(T bean) {
        if (!PersistenceConfig.getPreference(mName).belongsToPreferences(bean.getClass())) {
            throw new IllegalStateException("This object is not associated with a preference persister");
        }
        SharedPreferences.Editor editor = mPreferences.edit();
        fillEditor(editor, bean);
        editor.commit();
    }

    @Override
    public <T> T retrieve(Class<T> clazz) {
        try {
            T bean = clazz.newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                String value = mPreferences.getString(field.getName(), null);
                if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    field.set(bean, Boolean.parseBoolean(value == null ? "false" : value));
                } else if (field.getType() == float.class || field.getType() == Float.class
                        || field.getType() == Double.class || field.getType() == double.class) {
                    field.set(bean, Float.parseFloat(value == null ? "0.0" : value));
                } else if (field.getType() == Integer.class || field.getType() == int.class) {
                    field.set(bean, Integer.parseInt(value == null ? "0" : value));
                } else if (field.getType() == Long.class || field.getType() == long.class) {
                    field.set(bean, Long.parseLong(value == null ? "0" : value));
                } else if (field.getType() == String.class) {
                    field.set(bean, value);
                }
            }
            return bean;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <T> void delete(Class<T> clazz) {
        SharedPreferences.Editor editor = mPreferences.edit();
        for (Field field : clazz.getDeclaredFields()) {
            editor.remove(field.getName());
        }
        editor.commit();
    }

    protected <T> void fillEditor(SharedPreferences.Editor editor, T bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(bean);
                editor.putString(field.getName(), String.valueOf(value));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
