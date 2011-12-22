package com.egoclean.persistence;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cristian
 */
public class Persistence {
    private static final List<Class> SQLITE_LIST = new ArrayList<Class>();
    private static final List<ManyToMany> MANY_TO_MANY_LIST = new ArrayList<ManyToMany>();
    private static final List<Class> PREFS_MAP = new ArrayList<Class>();

    public static void matchSqlite(Class<?>... types) {
        for (Class<?> type : types) {
            if (!SQLITE_LIST.contains(type)) {
                SQLITE_LIST.add(type);
            }
        }
    }

    public static void matchPreference(Class<?>... types) {
        for (Class<?> type : types) {
            if (!PREFS_MAP.contains(type)) {
                PREFS_MAP.add(type);
            }
        }
    }

    public static void matchSqlite(ManyToMany manyToMany) {
        matchSqlite(manyToMany.getClasses());
        if (!MANY_TO_MANY_LIST.contains(manyToMany)) {
            MANY_TO_MANY_LIST.add(manyToMany);
        }
    }

    static Relationship getRelationship(Class<?> theClass, Class<?> collectionClass) {
        for (ManyToMany manyToMany : MANY_TO_MANY_LIST) {
            Class<?>[] classes = manyToMany.getClasses();
            if ((classes[0] == theClass && classes[1] == collectionClass) ||
                    (classes[1] == theClass && classes[0] == collectionClass)) {
                return Relationship.MANY_TO_MANY;
            }
        }
        return Relationship.UNKNOWN;
    }

    enum PersistenceType {SQLITE, PREFERENCES, UNKNOWN}

    enum Relationship {MANY_TO_MANY, UNKNOWN}

    /**
     * @param clazz the class to search in the list of registered classes
     * @return the type of persistence to use for the specified type
     */
    public static PersistenceType getPersistenceType(Class clazz) {
        if (SQLITE_LIST.contains(clazz)) {
            return PersistenceType.SQLITE;
        } else if (PREFS_MAP.contains(clazz)) {
            return PersistenceType.PREFERENCES;
        }
        return PersistenceType.UNKNOWN;
    }

    static List<Class> getSqliteClasses() {
        return SQLITE_LIST;
    }

    static List<ManyToMany> getSqliteManyToMany() {
        return MANY_TO_MANY_LIST;
    }
}
