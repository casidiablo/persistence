package com.codeslap.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.codeslap.persistence.pref.Preference;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * This adapter is used to persist and retrieve single beans. This is an alternative
 * to the SqliteAdapter which is more useful when we want to save collection of
 * beans that can be organized in tables.
 */
class PrefsAdapterImpl implements PreferencesAdapter {

    private final Map<String, SharedPreferences> mPreferences = new HashMap<String, SharedPreferences>();
    private final Context mContext;
    private final String mName;

    public PrefsAdapterImpl(Context context, String name) {
        mContext = context;
        mName = name;
    }

    public PrefsAdapterImpl(Context context) {
        this(context, DEFAULT_PREFS);
    }

    @Override
    public <T> void store(T bean) {
        Class<? extends Object> theClass = bean.getClass();
        if (!PersistenceConfig.getPreference(mName).belongsToPreferences(theClass)) {
            throw new IllegalStateException("This object is not associated with a preference persister");
        }
        SharedPreferences.Editor editor = getSharedPreferences(theClass).edit();
        fillEditor(editor, bean);
        editor.commit();
    }

    @Override
    public <T> T retrieve(Class<T> clazz) {
        T bean;
        try {
            bean = clazz.newInstance();
        } catch (Exception e) {
            return null;
        }
        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Preference annotation = field.getAnnotation(Preference.class);
                String keyName;
                if (annotation == null) {
                    keyName = field.getName();
                } else {
                    keyName = annotation.key();
                }
                boolean defaultEnabled = annotation != null && !annotation.defaultValue().equals("");
                Object value = null;
                if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    boolean def = defaultEnabled && "true".equals(annotation.defaultValue());
                    value = getSharedPreferences(clazz).getBoolean(keyName, def);
                } else if (field.getType() == float.class || field.getType() == Float.class
                        || field.getType() == double.class || field.getType() == Double.class) {
                    float def = defaultEnabled ? Float.parseFloat(annotation.defaultValue()) : 0.0f;
                    value = getSharedPreferences(clazz).getFloat(keyName, def);
                } else if (field.getType() == Integer.class || field.getType() == int.class) {
                    int def = defaultEnabled ? Integer.parseInt(annotation.defaultValue()) : 0;
                    value = getSharedPreferences(clazz).getInt(keyName, def);
                } else if (field.getType() == Long.class || field.getType() == long.class) {
                    long def = defaultEnabled ? Long.parseLong(annotation.defaultValue()) : 0L;
                    value = getSharedPreferences(clazz).getLong(keyName, def);
                } else if (field.getType() == String.class) {
                    String def = defaultEnabled ? annotation.defaultValue() : null;
                    value = getSharedPreferences(clazz).getString(keyName, def);
                }
                field.set(bean, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bean;
    }

    @Override
    public <T> void delete(Class<T> clazz) {
        SharedPreferences.Editor editor = getSharedPreferences(clazz).edit();
        for (Field field : clazz.getDeclaredFields()) {
            editor.remove(field.getName());
        }
        editor.commit();
    }

    protected <T> void fillEditor(SharedPreferences.Editor editor, T bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Preference preferenceAnnotation = field.getAnnotation(Preference.class);
            try {
                Object value = field.get(bean);
                String keyName;
                if (preferenceAnnotation == null) {
                    keyName = field.getName();
                } else {
                    keyName = preferenceAnnotation.key();
                }
                if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    editor.putBoolean(keyName, (Boolean) value);
                } else if (field.getType() == float.class || field.getType() == Float.class
                        || field.getType() == double.class || field.getType() == Double.class) {
                    editor.putFloat(keyName, ((Double) value).floatValue());
                } else if (field.getType() == Integer.class || field.getType() == int.class) {
                    editor.putInt(keyName, (Integer) value);
                } else if (field.getType() == Long.class || field.getType() == long.class) {
                    editor.putLong(keyName, (Long) value);
                } else if (field.getType() == String.class) {
                    editor.putString(keyName, String.valueOf(value));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private SharedPreferences getSharedPreferences(Class<? extends Object> theClass) {
        String key = theClass.getSimpleName();
        if (!mPreferences.containsKey(key)) {
            SharedPreferences prefs;
            if (DEFAULT_PREFS.equals(mName)) {
                prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            } else {
                prefs = mContext.getSharedPreferences(mName, Context.MODE_PRIVATE);
            }
            mPreferences.put(key, prefs);
        }
        return mPreferences.get(key);
    }
}
