package com.egoclean.persistence;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class SQLHelper {

    static final String ID = "id";
    private static final String PRIMARY_KEY = "id INTEGER PRIMARY KEY";

    static String getCreateTableSentence(Class clazz) {
        if (clazz == Object.class) {
            throw new IllegalArgumentException("You cannot pass an Object type");
        }
        String tableName = clazz.getSimpleName();
        List<String> fieldSentences = new ArrayList<String>();
        // loop through the hierarchy and get all the fields
        do {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (declaredField.getName().equals(ID)) {
                    fieldSentences.add(PRIMARY_KEY);
                } else {
                    fieldSentences.add(getFieldSentence(declaredField.getName(), declaredField.getType()));
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);

        // sort sentences
        Collections.sort(fieldSentences, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                if (s1.equals(PRIMARY_KEY)) {
                    return -1;
                }
                if (s2.equals(PRIMARY_KEY)) {
                    return 1;
                }
                return 0;
            }
        });

        // build create table sentence
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE IF NOT EXISTS ").append(SqlUtils.normalize(tableName)).append(" (");
        for (String fieldSentence : fieldSentences) {
            if (!fieldSentence.equals(PRIMARY_KEY)) {
                builder.append(", ");
            }
            builder.append(fieldSentence);
        }
        builder.append(");");
        return builder.toString();
    }

    /**
     * @param name the name of the field
     * @param type the type
     * @return the sql statement to create that kind of field
     */
    private static String getFieldSentence(String name, Class<?> type) {
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
            return String.format("%s INTEGER NOT NULL", SqlUtils.normalize(name));
        }
        if (type == boolean.class || type == Boolean.class) {
            return String.format("%s BOOLEAN NOT NULL", SqlUtils.normalize(name));
        }
        if (type == float.class || type == Float.class || type == double.class || type == Double.class) {
            return String.format("%s REAL NOT NULL", SqlUtils.normalize(name));
        }
        return String.format("%s TEXT NOT NULL", SqlUtils.normalize(name));
    }

    static String getWhere(Object object) {
        if (object == null) {
            return null;
        }

        List<String> conditions = new ArrayList<String>();
        Class<?> clazz = object.getClass();
        do {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                try {
                    Class<?> type = field.getType();
                    field.setAccessible(true);
                    Object value = field.get(object);
                    if (hasData(type, value)) {
                        conditions.add(String.format("%s = '%s'", SqlUtils.normalize(field.getName()), value));
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);

        StringBuilder builder = new StringBuilder();
        boolean glue = false;
        for (String condition : conditions) {
            if (glue) {
                builder.append(" AND ");
            }
            builder.append(condition);
            glue = true;
        }
        return builder.toString();
    }

    private static boolean hasData(Class<?> type, Object value) {
        if (type == long.class || type == Long.class) {
            return ((Long) value) != 0L;
        }
        if (type == int.class || type == Integer.class) {
            return ((Integer) value) != 0L;
        }
        if (type == float.class || type == Float.class) {
            return ((Float) value) != 0.0;
        }
        if (type == double.class || type == Double.class) {
            return ((Double) value) != 0.0;
        }
        if (type == boolean.class || type == Boolean.class) {
            return false;
        }
        return value != null;
    }
}