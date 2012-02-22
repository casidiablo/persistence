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

import android.content.Context;
import android.preference.EditTextPreference;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
* @author cristian
*/
class SmartEditTextPreference extends EditTextPreference implements android.preference.Preference.OnPreferenceClickListener {
    private final Class<?> mType;
    private String mDefVal;

    public SmartEditTextPreference(Context context, Class<?> type, String defVal) {
        super(context);
        mType = type;
        mDefVal = defVal;
        setOnPreferenceClickListener(this);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        String value = null;
        if (mType == int.class) {
            value = String.valueOf(getPersistedInt(Integer.parseInt(mDefVal)));
        } else if (mType == float.class) {
            Double val = Double.parseDouble(mDefVal);
            value = String.valueOf(getPersistedFloat(val.floatValue()));
        } else if (mType == boolean.class) {
            value = String.valueOf(getPersistedBoolean(Boolean.parseBoolean(mDefVal)));
        } else if (mType == long.class) {
            value = String.valueOf(getPersistedLong(Long.parseLong(mDefVal)));
        } else if (mType == String.class) {
            value = super.getPersistedString(mDefVal);
        }
        return value;
    }

    @Override
    protected boolean persistString(String value) {
        if (mType == int.class) {
            return persistInt(Integer.parseInt(value));
        } else if (mType == float.class) {
            return persistFloat(Float.parseFloat(value));
        } else if (mType == boolean.class) {
            return persistBoolean(Boolean.parseBoolean(value));
        } else if (mType == long.class) {
            return persistLong(Long.parseLong(value));
        } else {
            return super.persistString(value);
        }
    }

    @Override
    public boolean onPreferenceClick(android.preference.Preference preference) {
        if (!(preference instanceof EditTextPreference)) {
            return true;
        }

        String persistedString = getPersistedString(mDefVal);
        try {
            // get the internal EditText
            Field internal = EditTextPreference.class.getDeclaredField("mEditText");
            internal.setAccessible(true);
            Object editText = internal.get(preference);
            // set the text
            Method setText = TextView.class.getDeclaredMethod("setText", CharSequence.class);
            setText.invoke(editText, persistedString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
