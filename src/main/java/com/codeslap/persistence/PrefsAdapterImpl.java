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

package com.codeslap.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.codeslap.persistence.pref.Preference;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

    PrefsAdapterImpl(Context context, String name) {
        mContext = context;
        mName = name;
    }

    PrefsAdapterImpl(Context context) {
        this(context, DEFAULT_PREFS);
    }

    @Override
    public <T> void store(T bean) {
        Class<?> theClass = bean.getClass();
        if (!PersistenceConfig.getPreference(mName).belongsToPreferences(theClass)) {
            throw new IllegalStateException("This object is not associated with a preference persister");
        }
        SharedPreferences.Editor editor = getSharedPreferences(theClass).edit();
        fillEditor(editor, bean);
        editor.commit();
    }

    @Override
    public <T> T retrieve(Class<T> theClass) {
        T bean;
        try {
            bean = theClass.newInstance();
        } catch (Exception e) {
            return null;
        }
        try {
            for (Field field : theClass.getDeclaredFields()) {
                // ignore static and final fields
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                Preference annotation = field.getAnnotation(Preference.class);
                String keyName;
                if (annotation == null) {
                    keyName = field.getName();
                } else {
                    keyName = annotation.value();
                }
                boolean defaultEnabled = annotation != null && !annotation.defaultValue().equals("");
                Object value = null;
                Class<?> type = field.getType();
                if (type == boolean.class || type == Boolean.class) {
                    boolean def = defaultEnabled && "true".equals(annotation.defaultValue());
                    value = getSharedPreferences(theClass).getBoolean(keyName, def);
                } else if (type == float.class || type == Float.class
                        || type == double.class || type == Double.class) {
                    float def = defaultEnabled ? Float.parseFloat(annotation.defaultValue()) : 0.0f;
                    value = getSharedPreferences(theClass).getFloat(keyName, def);
                } else if (type == Integer.class || type == int.class) {
                    int def = defaultEnabled ? Integer.parseInt(annotation.defaultValue()) : 0;
                    value = getSharedPreferences(theClass).getInt(keyName, def);
                } else if (type == Long.class || type == long.class) {
                    long def = defaultEnabled ? Long.parseLong(annotation.defaultValue()) : 0L;
                    value = getSharedPreferences(theClass).getLong(keyName, def);
                } else if (type == String.class) {
                    String def = defaultEnabled ? annotation.defaultValue() : null;
                    value = getSharedPreferences(theClass).getString(keyName, def);
                } else {
                    String msg = String.format("Current object (%s) has incompatible fields (%s of type %s)", bean, field, type);
                    PersistenceLogManager.e("pref", msg);
                }
                field.set(bean, value);
            }
        } catch (Exception ignored) {
        }
        return bean;
    }

    @Override
    public <T> boolean delete(Class<T> theClass) {
        SharedPreferences.Editor editor = getSharedPreferences(theClass).edit();
        for (Field field : theClass.getDeclaredFields()) {
            Preference preferenceAnnotation = field.getAnnotation(Preference.class);
            String keyName;
            if (preferenceAnnotation == null) {
                keyName = field.getName();
            } else {
                keyName = preferenceAnnotation.value();
            }
            editor.remove(keyName);
        }
        return editor.commit();
    }

    private <T> void fillEditor(SharedPreferences.Editor editor, T bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            // ignore static and final fields
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            Preference preferenceAnnotation = field.getAnnotation(Preference.class);
            try {
                Object value = field.get(bean);
                String keyName = preferenceAnnotation == null ? field.getName() : preferenceAnnotation.value();
                if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    editor.putBoolean(keyName, (Boolean) value);
                } else if (field.getType() == float.class || field.getType() == Float.class) {
                    editor.putFloat(keyName, (Float) value);
                } else if (field.getType() == double.class || field.getType() == Double.class) {
                    editor.putFloat(keyName, ((Double) value).floatValue());
                } else if (field.getType() == Integer.class || field.getType() == int.class) {
                    editor.putInt(keyName, (Integer) value);
                } else if (field.getType() == Long.class || field.getType() == long.class) {
                    editor.putLong(keyName, (Long) value);
                } else if (field.getType() == String.class) {
                    editor.putString(keyName, String.valueOf(value));
                }
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    private SharedPreferences getSharedPreferences(Class<?> theClass) {
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
