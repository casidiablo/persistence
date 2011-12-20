package com.egoclean.persistence;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cristian
 */
public class Persistence {
    private static final List<Class> SQLITE_MAP = new ArrayList<Class>();
    private static final List<Class> PREFS_MAP = new ArrayList<Class>();

    public static void matchSqlite(Class<?> type) {
        SQLITE_MAP.add(type);
    }

    public static void matchPreference(Class<?> type) {
        PREFS_MAP.add(type);
    }

    enum PersistenceType{SQLITE, PREFERENCES, UNKNOWN}

    /**
     * @param clazz the class to search in the list of registered classes
     * @return the type of persistence to use for the specified type
     */
    public static PersistenceType getPersistenceType(Class clazz) {
        if (SQLITE_MAP.contains(clazz)) {
            return PersistenceType.SQLITE;
        } else if(PREFS_MAP.contains(clazz)) {
            return PersistenceType.PREFERENCES;
        }
        return PersistenceType.UNKNOWN;
    }

    static List<Class> getSqliteClasses() {
        return SQLITE_MAP;
    }
}
