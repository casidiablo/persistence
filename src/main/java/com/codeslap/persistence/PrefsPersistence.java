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

import java.util.ArrayList;
import java.util.List;

/**
 * @author cristian
 */
@SuppressWarnings("UnusedDeclaration")
public class PrefsPersistence {
    private final List<Class<?>> PREFS_MAP = new ArrayList<Class<?>>();

    public void match(Class<?>... types) {
        for (Class<?> type : types) {
            if (!PREFS_MAP.contains(type)) {
                PREFS_MAP.add(type);
            }
        }
    }

    boolean belongsToPreferences(Class<?> clazz) {
        return PREFS_MAP.contains(clazz);
    }
}
