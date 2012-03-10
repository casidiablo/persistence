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
import android.text.InputType;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author cristian
 */
public class PreferenceScreenHelper {

    public static void addPreferencesFrom(PreferenceActivity activity, Class<?>... classes) {
        Map<Integer, List<PrefMetadata>> fieldMap = new HashMap<Integer, List<PrefMetadata>>();
        List<CategoryMetadata> categories = new ArrayList<CategoryMetadata>();
        // loop through all the classes and fields.
        for (Class<?> theClass : classes) {
            for (Field field : theClass.getDeclaredFields()) {
                Preference annotation = field.getAnnotation(Preference.class);
                // only fields with annotations and ignore == false will be used in the preference screen
                if (annotation == null || annotation.ignore()) {
                    continue;
                }
                int title = annotation.title();
                int summary = annotation.summary();
                // if the annotation does not title or summary, ignore it
                if (title == 0 && summary == 0) {
                    continue;
                }
                Class<?> type = field.getType();
                // make sure the field has a valid type
                if (type != String.class && type != int.class && type != Integer.class &&
                        type != float.class && type != Float.class && type != double.class &&
                        type != Double.class && type != boolean.class && type != Boolean.class) {
                    continue;
                }
                // add the preference data to the category list
                PrefMetadata prefMetaData = new PrefMetadata()
                        .setTitle(title)
                        .setSummary(summary)
                        .setOrder(annotation.order())
                        .setType(type)
                        .setDefaultValue(annotation.defaultValue())
                        .setKey(annotation.value())
                        .setDialogIcon(annotation.dialogIcon())
                        .setDialogMessage(annotation.dialogMessage())
                        .setDialogTitle(annotation.dialogTitle());
                if (fieldMap.containsKey(annotation.category())) {
                    fieldMap.get(annotation.category()).add(prefMetaData);
                } else {
                    ArrayList<PrefMetadata> list = new ArrayList<PrefMetadata>();
                    list.add(prefMetaData);
                    fieldMap.put(annotation.category(), list);
                    categories.add(new CategoryMetadata(annotation.category(), annotation.categoryOrder()));
                }
            }
        }

        // sort categories by order
        Collections.sort(categories, new Comparator<CategoryMetadata>() {
            @Override
            public int compare(CategoryMetadata foo, CategoryMetadata bar) {
                return foo.categoryOrder - bar.categoryOrder;
            }
        });

        // now add each field to the preference screen
        for (CategoryMetadata category : categories) {
            // sort the list of fields
            List<PrefMetadata> fields = fieldMap.get(category.categoryTitle);
            Collections.sort(fields, new Comparator<PrefMetadata>() {
                @Override
                public int compare(PrefMetadata foo, PrefMetadata bar) {
                    return foo.getOrder() - bar.getOrder();
                }
            });

            // if the category exist, create it
            if (category.categoryTitle != -1) {
                PreferenceCategory preferenceCategory = new PreferenceCategory(activity);
                preferenceCategory.setTitle(category.categoryTitle);
                activity.getPreferenceScreen().addPreference(preferenceCategory);
            }

            // add each field to the preference screen
            for (PrefMetadata field : fields) {
                android.preference.Preference preference = null;
                // define a default value
                boolean hasDefault = !"".equals(field.getDefaultValue());
                TypeChangeListener typeChangeListener = null;
                // depending on the type of the field, create different kind of preferences
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                Class<?> type = field.getType();
                String key = field.getKey();
                String defaultValue = field.getDefaultValue();
                if (type == boolean.class || type == Boolean.class) {
                    preference = new CheckBoxPreference(activity);
                    boolean checked = prefs.getBoolean(key, hasDefault ? Boolean.parseBoolean(defaultValue) : false);
                    ((CheckBoxPreference) preference).setChecked(checked);
                    typeChangeListener = new TypeChangeListener(boolean.class);
                } else if (type == int.class || type == Integer.class
                        || type == long.class || type == Long.class) {
                    if (type == long.class || type == Long.class) {
                        long value = prefs.getLong(key, hasDefault ? Long.parseLong(defaultValue) : 0L);
                        preference = new SmartEditTextPreference(activity, long.class, String.valueOf(value));
                        ((EditTextPreference) preference).setText(String.valueOf(value));
                        typeChangeListener = new TypeChangeListener(long.class);
                    } else {
                        int value = prefs.getInt(key, hasDefault ? Integer.parseInt(defaultValue) : 0);
                        preference = new SmartEditTextPreference(activity, int.class, String.valueOf(value));

                        ((EditTextPreference) preference).setText(String.valueOf(value));
                        typeChangeListener = new TypeChangeListener(int.class);
                    }
                    setEditTextType(preference, InputType.TYPE_CLASS_NUMBER);
                } else if (type == float.class || type == Float.class ||
                        type == double.class || type == Double.class) {
                    float def = hasDefault ? ((Double) Double.parseDouble(defaultValue)).floatValue() : 0.0f;
                    float value = prefs.getFloat(key, def);
                    preference = new SmartEditTextPreference(activity, float.class, String.valueOf(value));
                    ((EditTextPreference) preference).setText(String.valueOf(value));
                    setEditTextType(preference, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    typeChangeListener = new TypeChangeListener(float.class);
                } else if (type == String.class) {
                    String def = hasDefault ? defaultValue : null;
                    preference = new SmartEditTextPreference(activity, String.class, def);
                    ((EditTextPreference) preference).setText(def);
                    typeChangeListener = new TypeChangeListener(String.class);
                }

                // if a preference was created, add it to the preference screen
                if (preference != null) {
                    preference.setTitle(field.getTitle());
                    preference.setKey(key);
                    preference.setSummary(field.getSummary());
                    if (preference instanceof SmartEditTextPreference) {
                        SmartEditTextPreference pref = (SmartEditTextPreference) preference;
                        int dialogTitle = field.getDialogTitle();
                        if (dialogTitle != 0) {
                            pref.setDialogTitle(dialogTitle);
                        }
                        int dialogIcon = field.getDialogIcon();
                        if (dialogIcon != 0) {
                            pref.setDialogIcon(dialogIcon);
                        }
                        int dialogMessage = field.getDialogMessage();
                        if (dialogMessage != 0) {
                            pref.setDialogMessage(dialogMessage);
                        }
                    }
                    preference.setOnPreferenceChangeListener(typeChangeListener);
                    activity.getPreferenceScreen().addPreference(preference);
                }
            }
        }
    }

    private static class CategoryMetadata {

        final int categoryTitle;

        final int categoryOrder;

        private CategoryMetadata(int categoryTitle, int categoryOrder) {
            this.categoryTitle = categoryTitle;
            this.categoryOrder = categoryOrder;
        }

    }

    private static void setEditTextType(android.preference.Preference preference, int type) {
        try {
            // get the internal EditText
            Field internal = EditTextPreference.class.getDeclaredField("mEditText");
            internal.setAccessible(true);
            Object editText = internal.get(preference);
            // set the input type
            Method setInputType = TextView.class.getDeclaredMethod("setInputType", int.class);
            setInputType.invoke(editText, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
