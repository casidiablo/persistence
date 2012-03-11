/*
 * Copyright 2012 CodeSlap
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

package com.codeslap.persistence.pref;

import android.content.SharedPreferences;
import android.preference.*;
import android.preference.Preference;

/**
 * @author cristian
 */
class TypeChangeListener implements android.preference.Preference.OnPreferenceChangeListener {
    private final Class<?> mType;
    private Preference.OnPreferenceChangeListener mOnPreferenceChangeListener;

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
            ((CheckBoxPreference) preference).setChecked((Boolean) o);
        } else if (mType == String.class) {
            editor.putString(key, (String) o);
        }
        editor.commit();
        if (mOnPreferenceChangeListener != null) {
            return mOnPreferenceChangeListener.onPreferenceChange(preference, o);
        }
        return false;
    }

    public void setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener onPreferenceChangeListener) {
        mOnPreferenceChangeListener = onPreferenceChangeListener;
    }
}
