package com.codeslap.persistence;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class SQLHelper {

    static final String ID = "id";
    static final String PRIMARY_KEY = "id INTEGER PRIMARY KEY";
    private static final String PRIMARY_KEY_TEXT = "id TEXT PRIMARY KEY";

    static String getCreateTableSentence(String dbName, Class clazz) {
        if (clazz == Object.class) {
            throw new IllegalArgumentException("You cannot pass an Object type");
        }
        String tableName = clazz.getSimpleName();
        List<String> fieldSentences = new ArrayList<String>();
        // loop through all the fields and add sql statements
        Field[] declaredFields = clazz.getDeclaredFields();
        List<String> columns = new ArrayList<String>();
        for (Field declaredField : declaredFields) {
            if (declaredField.getName().equals(ID)) {
                String primaryKeySentence = PRIMARY_KEY;
                if (declaredField.getType() == String.class) {
                    primaryKeySentence = PRIMARY_KEY_TEXT;
                } else if (Persistence.getDatabase(dbName).getAutoIncrementList().contains(clazz)) {
                    primaryKeySentence += " AUTOINCREMENT";
                }
                if (!columns.contains(normalize(declaredField.getName()))) {
                    fieldSentences.add(primaryKeySentence);
                    columns.add(normalize(declaredField.getName()));
                }
            } else if (declaredField.getType() != List.class) {
                if (!columns.contains(normalize(declaredField.getName()))) {
                    fieldSentences.add(getFieldSentence(declaredField.getName(), declaredField.getType()));
                    columns.add(normalize(declaredField.getName()));
                }
            }
        }

        // check whether this class belongs to a has-many relation, in which case we need to create an additional field
        HasMany belongsTo = Persistence.getDatabase(dbName).belongsTo(clazz);
        if (belongsTo != null) {
            // if so, add a new field to the table creation statement to create the relation
            Class<?> containerClass = belongsTo.getClasses()[0];
            try {
                Field field = containerClass.getDeclaredField(belongsTo.getThrough());
                String columnName = String.format("%s_%s", containerClass.getSimpleName(), normalize(belongsTo.getThrough()));
                if (!columns.contains(columnName)) {
                    fieldSentences.add(getFieldSentence(columnName, field.getType()));
                    columns.add(normalize(columnName));
                }
            } catch (NoSuchFieldException ignored) {
            }
        }

        // sort sentences
        Collections.sort(fieldSentences, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                if (s1.contains(PRIMARY_KEY)) {
                    return -1;
                }
                if (s2.contains(PRIMARY_KEY)) {
                    return 1;
                }
                return 0;
            }
        });

        // build create table sentence
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE IF NOT EXISTS ").append(normalize(tableName)).append(" (");
        boolean first = true;
        for (String fieldSentence : fieldSentences) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(fieldSentence);
            first = false;
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
            return String.format("%s INTEGER NOT NULL", normalize(name));
        }
        if (type == boolean.class || type == Boolean.class) {
            return String.format("%s BOOLEAN NOT NULL", normalize(name));
        }
        if (type == float.class || type == Float.class || type == double.class || type == Double.class) {
            return String.format("%s REAL NOT NULL", normalize(name));
        }
        return String.format("%s TEXT NOT NULL", normalize(name));
    }

    static <T> String getWhere(String dbName, T object, List<String> args) {
        if (object == null) {
            return null;
        }
        return getWhere(dbName, object.getClass(), object, args, null);
    }

    static <T, G> String getWhere(String dbName, Class<?> theClass, T bean, List<String> args, G attachedTo) {
        List<String> conditions = new ArrayList<String>();
        if (bean != null) {
            Class<?> clazz = bean.getClass();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                try {
                    Class<?> type = field.getType();
                    if (type == List.class) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(bean);
                    if (hasData(type, value)) {
                        if (field.getType() == String.class) {
                            conditions.add(String.format("%s LIKE ?", normalize(field.getName())));
                        } else {
                            conditions.add(String.format("%s = ?", normalize(field.getName())));
                        }
                        args.add(String.valueOf(value));
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }

        // if there is an attachment
        if (attachedTo != null) {
            switch (Persistence.getDatabase(dbName).getRelationship(attachedTo.getClass(), theClass)) {
                case HAS_MANY: {
                    try {
                        HasMany hasMany = Persistence.getDatabase(dbName).belongsTo(theClass);
                        Field primaryForeignKey = attachedTo.getClass().getDeclaredField(hasMany.getThrough());
                        primaryForeignKey.setAccessible(true);
                        Object foreignValue = primaryForeignKey.get(attachedTo);
                        if (foreignValue != null) {
                            args.add(foreignValue.toString());
                            conditions.add(String.format("%s = ?", hasMany.getForeignKey()));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

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
            return value != null && ((Long) value) != 0L;
        }
        if (type == int.class || type == Integer.class) {
            return value != null && ((Integer) value) != 0;
        }
        if (type == float.class || type == Float.class) {
            return value != null && ((Float) value) != 0.0;
        }
        if (type == double.class || type == Double.class) {
            return value != null && ((Double) value) != 0.0;
        }
        if (type == boolean.class || type == Boolean.class) {
            return false;
        }
        return value != null;
    }

    /**
     * @param name string to normalize
     * @return converts a camelcase string into a lowercase, _ separated string
     */
    static String normalize(String name) {
        StringBuilder newName = new StringBuilder();
        newName.append(name.charAt(0));
        for (int i = 1; i < name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i))) {
                newName.append("_");
            }
            newName.append(name.charAt(i));
        }
        return newName.toString().toLowerCase();
    }
}