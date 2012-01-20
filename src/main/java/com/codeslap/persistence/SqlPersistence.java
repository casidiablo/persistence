package com.codeslap.persistence;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cristian
 */
@SuppressWarnings("UnusedDeclaration")
public class SqlPersistence {
    private final List<Class<?>> SQLITE_LIST = new ArrayList<Class<?>>();
    private final List<Class<?>> AUTO_INCREMENT_LIST = new ArrayList<Class<?>>();
    private final List<ManyToMany> MANY_TO_MANY_LIST = new ArrayList<ManyToMany>();
    private final List<HasMany> HAS_MANY_LIST = new ArrayList<HasMany>();

    private final String mName;
    private final int mVersion;

    public SqlPersistence(String name, int version) {
        mName = name;
        mVersion = version;
    }

    public String getName() {
        return mName;
    }

    public int getVersion() {
        return mVersion;
    }

    public void match(Class<?>... types) {
        for (Class<?> type : types) {
            if (!SQLITE_LIST.contains(type)) {
                SQLITE_LIST.add(type);
                AUTO_INCREMENT_LIST.add(type);
            }
        }
    }

    public void matchNotAutoIncrement(Class<?>... types) {
        for (Class<?> type : types) {
            if (!SQLITE_LIST.contains(type)) {
                SQLITE_LIST.add(type);
            }
        }
    }

    public void match(ManyToMany manyToMany) {
        match(manyToMany.getClasses());
        if (!MANY_TO_MANY_LIST.contains(manyToMany)) {
            MANY_TO_MANY_LIST.add(manyToMany);
        }
    }

    public void matchNotAutoIncrement(ManyToMany manyToMany) {
        matchNotAutoIncrement(manyToMany.getClasses());
        if (!MANY_TO_MANY_LIST.contains(manyToMany)) {
            MANY_TO_MANY_LIST.add(manyToMany);
        }
    }

    public void match(HasMany hasMany) {
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

    public void matchNotAutoIncrement(HasMany hasMany) {
        Class<?>[] classes = hasMany.getClasses();
        matchNotAutoIncrement(classes);
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

    /**
     * @param theClass a class
     * @param collectionClass another class
     * @return the type of relationship between two classes
     */
    Relationship getRelationship(Class<?> theClass, Class<?> collectionClass) {
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
    HasMany belongsTo(Class clazz) {
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

    List<Class<?>> getSqliteClasses() {
        return SQLITE_LIST;
    }

    List<Class<?>> getAutoIncrementList() {
        return AUTO_INCREMENT_LIST;
    }

    List<ManyToMany> getSqliteManyToMany() {
        return MANY_TO_MANY_LIST;
    }
}
