package com.egoclean.persistence;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cristian
 */
@SuppressWarnings("UnusedDeclaration")
public class Persistence {
    private static final List<Class<?>> SQLITE_LIST = new ArrayList<Class<?>>();
    private static final List<Class<?>> AUTO_INCREMENT_LIST = new ArrayList<Class<?>>();
    private static final List<ManyToMany> MANY_TO_MANY_LIST = new ArrayList<ManyToMany>();
    private static final List<HasMany> HAS_MANY_LIST = new ArrayList<HasMany>();
    private static final List<Class<?>> PREFS_MAP = new ArrayList<Class<?>>();

    public static void match(Class<?>... types) {
        for (Class<?> type : types) {
            if (!SQLITE_LIST.contains(type)) {
                SQLITE_LIST.add(type);
            }
        }
    }

    public static void autoincrementPrimaryKey(Class<?>... types) {
        for (Class<?> type : types) {
            if (!AUTO_INCREMENT_LIST.contains(type)) {
                AUTO_INCREMENT_LIST.add(type);
            }
        }
    }

    public static void match(ManyToMany manyToMany) {
        match(manyToMany.getClasses());
        if (!MANY_TO_MANY_LIST.contains(manyToMany)) {
            MANY_TO_MANY_LIST.add(manyToMany);
        }
    }

    public static void match(HasMany hasMany) {
        Class<?>[] classes = hasMany.getClasses();
        match(classes);
        // make sure there are no inverted has many relations
        for (HasMany hasManyRelation : HAS_MANY_LIST) {
            Class<?>[] clazzes = hasManyRelation.getClasses();
            if (clazzes[0] == classes[1] && clazzes[1] == classes[0] && hasMany.getThrough().equals(hasManyRelation.getThrough())) {
                throw new IllegalStateException("There should not be two has-many relations with the same classes. Use Many-To-Many");
            }
        }
        // add the has many relation to the list
        if (!HAS_MANY_LIST.contains(hasMany)) {
            HAS_MANY_LIST.add(hasMany);
        }
    }

    public static void matchPreference(Class<?>... types) {
        for (Class<?> type : types) {
            if (!PREFS_MAP.contains(type)) {
                PREFS_MAP.add(type);
            }
        }
    }

    /**
     * @param theClass a class
     * @param collectionClass another class
     * @return the type of relationship between two classes
     */
    static Relationship getRelationship(Class<?> theClass, Class<?> collectionClass) {
        for (HasMany hasMany : HAS_MANY_LIST) {
            Class<?>[] classes = hasMany.getClasses();
            if (classes[0] == theClass && classes[1] == collectionClass) {
                return Relationship.HAS_MANY;
            }
        }

        for (ManyToMany manyToMany : MANY_TO_MANY_LIST) {
            Class<?>[] classes = manyToMany.getClasses();
            if ((classes[0] == theClass && classes[1] == collectionClass) ||
                    (classes[1] == theClass && classes[0] == collectionClass)) {
                return Relationship.MANY_TO_MANY;
            }
        }
        return Relationship.UNKNOWN;
    }

    /**
     * @param clazz the class that we are checking whether belongs to another class
     * @return the class that clazz belongs to or null if not such relation
     */
    static HasMany belongsTo(Class clazz) {
        for (HasMany hasMany : HAS_MANY_LIST) {
            Class<?>[] classes = hasMany.getClasses();
            if (classes[1] == clazz) {
                return hasMany;
            }
        }
        return null;
    }

    enum PersistenceType {SQLITE, PREFERENCES, UNKNOWN}

    enum Relationship {MANY_TO_MANY, HAS_MANY, UNKNOWN}

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

    static List<Class<?>> getSqliteClasses() {
        return SQLITE_LIST;
    }

    static List<Class<?>> getAutoIncrementList() {
        return AUTO_INCREMENT_LIST;
    }

    static List<ManyToMany> getSqliteManyToMany() {
        return MANY_TO_MANY_LIST;
    }
}
