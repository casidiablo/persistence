package com.codeslap.persistence;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cristian
 */
@SuppressWarnings("UnusedDeclaration")
public class PrefsPersistence {
    private final List<Class<?>> PREFS_MAP = new ArrayList<Class<?>>();

    public PrefsPersistence() {
    }

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
