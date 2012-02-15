package com.codeslap.persistence.pref;

import android.content.SharedPreferences;

/**
* @author cristian
*/
class TypeChangeListener implements android.preference.Preference.OnPreferenceChangeListener {
    private final Class<?> mType;

    TypeChangeListener(Class<?> type) {
        mType = type;
    }

    @Override
    public boolean onPreferenceChange(android.preference.Preference preference, Object o) {
        SharedPreferences.Editor editor = preference.getEditor();
        String key = preference.getKey();
        if (mType == int.class) {
            editor.putInt(key, Integer.parseInt((String) o));
        } else if (mType == float.class) {
            editor.putFloat(key, Float.parseFloat((String) o));
        } else if (mType == long.class) {
            editor.putLong(key, Long.parseLong((String) o));
        } else if (mType == boolean.class) {
            editor.putBoolean(key, (Boolean) o);
        } else if (mType == String.class) {
            editor.putString(key, (String) o);
        }
        editor.commit();
        return false;
    }
}
