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

package com.codeslap.persistence.pref;

import android.content.Context;
import android.preference.CheckBoxPreference;

/**
 * @author cristian
 */
public class SmartCheckBoxPreference extends CheckBoxPreference{
    public SmartCheckBoxPreference(Context context) {
        super(context);
        setOnPreferenceChangeListener(new TypeChangeListener(boolean.class));
    }

    @Override
    public void setOnPreferenceChangeListener(OnPreferenceChangeListener onPreferenceChangeListener) {
        if (onPreferenceChangeListener instanceof TypeChangeListener) {
            super.setOnPreferenceChangeListener(onPreferenceChangeListener);
        } else {
            TypeChangeListener listener = (TypeChangeListener) getOnPreferenceChangeListener();
            listener.setOnPreferenceChangeListener(onPreferenceChangeListener);
        }
    }
}
