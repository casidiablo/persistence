package com.egoclean.persistence;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cristian
 */
public class DaoFactory {
    private static final List<Class> SQLITE_MAP = new ArrayList<Class>();
    private static final List<Class> PREFS_MAP = new ArrayList<Class>();

    @SuppressWarnings({"unchecked"})
    public static void matchSqlite(Class<?> type) {
        SQLITE_MAP.add(type);
    }

    public static void matchPreference(Class<?> type) {
        PREFS_MAP.add(type);
    }

    static List<Class> getRegisteredObjects() {
        List<Class> objects = new ArrayList();
        objects.addAll(SQLITE_MAP);
        objects.addAll(PREFS_MAP);
        return objects;
    }
}
