package com.codeslap.persistence;

import java.util.HashMap;
import java.util.Map;

public class PersistenceConfig {

    private static final Map<String, SqlPersistence> SQL = new HashMap<String, SqlPersistence>();
    private static final Map<String, PrefsPersistence> PREFS = new HashMap<String, PrefsPersistence>();
    static String sFirstDatabase;

    public static SqlPersistence getDatabase(String name, int version) {
        if (name == null) {
            throw new IllegalStateException("You must provide a valid database name");
        }
        if (sFirstDatabase == null) {
            sFirstDatabase = name;
        }
        if (SQL.containsKey(name)) {
            return SQL.get(name);
        }
        SqlPersistence sqlPersistence = new SqlPersistence(name, version);
        SQL.put(name, sqlPersistence);
        return sqlPersistence;
    }

    static SqlPersistence getDatabase(String name) {
        if (SQL.containsKey(name)) {
            return SQL.get(name);
        }
        throw new IllegalStateException(String.format("There is no sql persistence for '%s'", name));
    }

    public static PrefsPersistence getPreference(String name) {
        if (PREFS.containsKey(name)) {
            return PREFS.get(name);
        }
        PrefsPersistence persistence = new PrefsPersistence();
        PREFS.put(name, persistence);
        return persistence;
    }
}
