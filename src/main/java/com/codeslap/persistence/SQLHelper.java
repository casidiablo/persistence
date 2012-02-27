/*
 * Copyright 2012 CodeSlap
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

import java.lang.reflect.Field;
import java.util.*;

class SQLHelper {

    static final String ID = "id";
    static final String PRIMARY_KEY = "id INTEGER PRIMARY KEY";
    private static final String PRIMARY_KEY_TEXT = "id TEXT PRIMARY KEY";

    private static final Map<Class<?>, String> INSERT_COLUMNS_CACHE = new HashMap<Class<?>, String>();
    private static final Map<Class<?>, String> TABLE_NAMES_CACHE = new HashMap<Class<?>, String>();
    private static final Map<Field, String> COLUMN_NAMES_CACHE = new HashMap<Field, String>();
    static final String SELECT_AUTOINCREMENT_FORMAT = "(SELECT seq FROM sqlite_sequence WHERE name = '%s')";

    static final String STATEMENT_SEPARATOR = "b05f72bb_STATEMENT_SEPARATOR";

    static String getCreateTableSentence(String dbName, Class clazz) {
        if (clazz == Object.class) {
            throw new IllegalArgumentException("You cannot pass an Object type");
        }
        List<String> fieldSentences = new ArrayList<String>();
        // loop through all the fields and add sql statements
        Field[] declaredFields = clazz.getDeclaredFields();
        List<String> columns = new ArrayList<String>();
        for (Field field : declaredFields) {
            String columnName = getColumnName(field);
            if (isPrimaryKey(field)) {
                String primaryKeySentence = PRIMARY_KEY;
                if (field.getType() == String.class) {// what types are supported
                    primaryKeySentence = PRIMARY_KEY_TEXT;
                } else if (PersistenceConfig.getDatabase(dbName).isAutoincrement(clazz)) {
                    primaryKeySentence += " AUTOINCREMENT";
                }
                if (!columns.contains(columnName)) {
                    fieldSentences.add(primaryKeySentence);
                    columns.add(columnName);
                }
            } else if (field.getType() != List.class) {
                if (!columns.contains(columnName)) {
                    columns.add(columnName);
                    boolean notNull = false;
                    Column columnAnnotation = field.getAnnotation(Column.class);
                    if (columnAnnotation != null) {
                        notNull = columnAnnotation.notNull();
                    }
                    fieldSentences.add(getFieldSentence(columnName, field.getType(), notNull));
                }
            }
        }

        // check whether this class belongs to a has-many relation, in which case we need to create an additional field
        HasMany belongsTo = PersistenceConfig.getDatabase(dbName).belongsTo(clazz);
        if (belongsTo != null) {
            // if so, add a new field to the table creation statement to create the relation
            Class<?> containerClass = belongsTo.getClasses()[0];
            try {
                Field field = containerClass.getDeclaredField(belongsTo.getThrough());
                String columnName = String.format("%s_%s", normalize(containerClass.getSimpleName()), normalize(belongsTo.getThrough()));
                if (!columns.contains(columnName)) {
                    fieldSentences.add(getFieldSentence(columnName, field.getType(), true));
                    columns.add(getColumnName(field));
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
        builder.append("CREATE TABLE IF NOT EXISTS ").append(getTableName(clazz)).append(" (");
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
     * @param name    the name of the field
     * @param type    the type
     * @param notNull
     * @return the sql statement to create that kind of field
     */
    private static String getFieldSentence(String name, Class<?> type, boolean notNull) {
        name = normalize(name);
        String notNullSentence = "";
        if (notNull) {
            notNullSentence = " NOT NULL";
        }
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
            return String.format("%s INTEGER%s", name, notNullSentence);
        }
        if (type == boolean.class || type == Boolean.class) {
            return String.format("%s BOOLEAN%s", name, notNullSentence);
        }
        if (type == float.class || type == Float.class || type == double.class || type == Double.class) {
            return String.format("%s REAL%s", name, notNullSentence);
        }
        return String.format("%s TEXT%s", name, notNullSentence);
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
                        String columnName = getColumnName(field);
                        if (args == null) {
                            if (field.getType() == String.class) {
                                conditions.add(String.format("%s LIKE '%s'", columnName, value));
                            } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                                int intValue = ((Boolean) value).booleanValue() ? 1 : 0;
                                conditions.add(String.format("%s = '%d'", columnName, intValue));
                            } else {
                                conditions.add(String.format("%s = '%s'", columnName, value));
                            }
                        } else {
                            if (field.getType() == String.class) {
                                conditions.add(String.format("%s LIKE ?", columnName));
                            } else {
                                conditions.add(String.format("%s = ?", columnName));
                            }
                            if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                                value = ((Boolean) value).booleanValue() ? 1 : 0;
                            }
                            args.add(String.valueOf(value));
                        }
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }

        // if there is an attachment
        if (attachedTo != null) {
            switch (PersistenceConfig.getDatabase(dbName).getRelationship(attachedTo.getClass(), theClass)) {
                case HAS_MANY: {
                    try {
                        HasMany hasMany = PersistenceConfig.getDatabase(dbName).belongsTo(theClass);
                        Field primaryForeignKey = attachedTo.getClass().getDeclaredField(hasMany.getThrough());
                        primaryForeignKey.setAccessible(true);
                        Object foreignValue = primaryForeignKey.get(attachedTo);
                        if (foreignValue != null) {
                            if (args == null) {
                                conditions.add(String.format("%s = '%s'", hasMany.getForeignKey(), foreignValue.toString()));
                            } else {
                                conditions.add(String.format("%s = ?", hasMany.getForeignKey()));
                                args.add(foreignValue.toString());
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return join(conditions, " AND ");
    }

    private static <T> String getSet(T bean) {
        List<String> sets = new ArrayList<String>();
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
                    boolean isBoolean = field.getType() == Boolean.class || field.getType() == boolean.class;
                    if (isBoolean || hasData(type, value)) {
                        if (isBoolean) {
                            int intValue = ((Boolean) value).booleanValue() ? 1 : 0;
                            sets.add(String.format("%s = '%d'", getColumnName(field), intValue));
                        } else {
                            sets.add(String.format("%s = '%s'", getColumnName(field), String.valueOf(value).replace("'", "''")));
                        }
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }

        return join(sets, ", ");
    }

    private static String join(List<String> sets, String glue) {
        StringBuilder builder = new StringBuilder();
        boolean glued = false;
        for (String condition : sets) {
            if (glued) {
                builder.append(glue);
            }
            builder.append(condition);
            glued = true;
        }
        return builder.toString();
    }

    static boolean hasData(Class<?> type, Object value) {
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
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            if (value instanceof Integer) {
                return ((Integer) value) != 0;
            }
            return false;
        }
        return value != null;
    }

    /**
     * @param name string to normalize
     * @return converts a camel-case string into a lowercase, _ separated string
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

    /**
     * @param field field to get the column name from
     * @return gets the column name version of the specified field
     */
    static String getColumnName(Field field) {
        if (COLUMN_NAMES_CACHE.containsKey(field)) {
            return COLUMN_NAMES_CACHE.get(field);
        }
        if (isPrimaryKey(field)) {
            return ID;
        }
        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            return column.value();
        }
        String name = field.getName();
        StringBuilder newName = new StringBuilder();
        newName.append(name.charAt(0));
        for (int i = 1; i < name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i))) {
                newName.append("_");
            }
            newName.append(name.charAt(i));
        }
        String columnName = newName.toString().toLowerCase();
        COLUMN_NAMES_CACHE.put(field, columnName);
        return columnName;
    }

    public static <T> String getUpdateStatement(T bean, Object sample) {
        String where = getWhere(PersistenceConfig.sFirstDatabase, bean.getClass(), sample, null, null);
        String set = getSet(bean);
        return String.format("UPDATE %s SET %s WHERE %s;%s", getTableName(bean), set, where, STATEMENT_SEPARATOR);
    }

    public static <T> String getUpdateStatement(T bean, String where) {
        String set = getSet(bean);
        return String.format("UPDATE %s SET %s WHERE %s;%s", getTableName(bean), set, where, STATEMENT_SEPARATOR);
    }

    static <T, G> String getInsertStatement(T bean, G attachedTo, SqlPersistence persistence) {
        List<String> values = new ArrayList<String>();
        List<String> columns = null;
        if (!INSERT_COLUMNS_CACHE.containsKey(bean.getClass())) {
            columns = new ArrayList<String>();
        }
        populateColumnsAndValues(bean, attachedTo, values, columns, persistence);

        String columnsSet;
        if (INSERT_COLUMNS_CACHE.containsKey(bean.getClass())) {
            columnsSet = INSERT_COLUMNS_CACHE.get(bean.getClass());
        } else {
            columnsSet = join(columns, ", ");
            INSERT_COLUMNS_CACHE.put(bean.getClass(), columnsSet);
        }

        // build insert statement for the main object
        if (values.size() == 0 && persistence.isAutoincrement(bean.getClass())) {
            String hack = String.format("(SELECT seq FROM sqlite_sequence WHERE name = '%s')+1", getTableName(bean));
            return String.format("INSERT OR IGNORE INTO %s (%s) VALUES (%s);%s", getTableName(bean), ID, hack, STATEMENT_SEPARATOR);
        }
        return String.format("INSERT OR IGNORE INTO %s (%s) VALUES (%s);%s", getTableName(bean), columnsSet, join(values, ", "), STATEMENT_SEPARATOR);
    }

    private static <T, G> void populateColumnsAndValues(T bean, G attachedTo, List<String> values, List<String> columns, SqlPersistence persistence) {
        if (bean != null) {
            Class<?> theClass = bean.getClass();
            Field[] fields = theClass.getDeclaredFields();
            for (Field field : fields) {
                // if the class has an autoincrement, remove the ID
                if (persistence.isAutoincrement(theClass) && isPrimaryKey(field)) {
                    continue;
                }
                try {
                    Class<?> type = field.getType();
                    if (type == List.class) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(bean);
                    if (columns != null) {
                        columns.add(getColumnName(field));
                    }
                    if (values != null) {
                        if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                            int intValue = ((Boolean) value).booleanValue() ? 1 : 0;
                            values.add(String.format("%d", intValue));
                        } else if (value == null) {
                            Column columnAnnotation = field.getAnnotation(Column.class);
                            boolean hasDefault = !columnAnnotation.defaultValue().equals(Column.NULL);
                            if (columnAnnotation != null && columnAnnotation.notNull() && !hasDefault) {
                                String msg = String.format("Field %s from class %s cannot be null. It was marked with the @Column not null annotation and it has not a default value", field.getName(), theClass.getSimpleName());
                                throw new IllegalStateException(msg);
                            }
                            if (hasDefault) {
                                values.add(String.format("'%s'", columnAnnotation.defaultValue().replace("'", "''")));
                            } else {
                                values.add("NULL");
                            }
                        } else {
                            values.add(String.format("'%s'", String.valueOf(value).replace("'", "''")));
                        }
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            if (attachedTo != null) {
                switch (persistence.getRelationship(attachedTo.getClass(), bean.getClass())) {
                    case HAS_MANY: {
                        try {
                            HasMany hasMany = persistence.belongsTo(bean.getClass());
                            Field primaryForeignKey = attachedTo.getClass().getDeclaredField(hasMany.getThrough());
                            primaryForeignKey.setAccessible(true);
                            Object foreignValue = primaryForeignKey.get(attachedTo);
                            if (columns != null) {
                                columns.add(hasMany.getForeignKey());
                            }
                            if (values != null) {
                                if (persistence.isAutoincrement(theClass)) {
                                    values.add(String.format(SELECT_AUTOINCREMENT_FORMAT, getTableName(attachedTo.getClass())));
                                } else {
                                    values.add(String.valueOf(foreignValue));
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }

    private static boolean isPrimaryKey(Field field) {
        PrimaryKey annotation = field.getAnnotation(PrimaryKey.class);
        if (annotation != null) {
            return true;
        }
        return field.getName().equals(SQLHelper.ID);
    }

    static String getTableName(Class<?> clazz) {
        if (TABLE_NAMES_CACHE.containsKey(clazz)) {
            return TABLE_NAMES_CACHE.get(clazz);
        }
        String name = clazz.getSimpleName();
        if (name.endsWith("y")) {
            name = name.substring(0, name.length() - 1) + "ies";
        } else if (!name.endsWith("s")) {
            name += "s";
        }
        String tableName = normalize(name);
        TABLE_NAMES_CACHE.put(clazz, tableName);
        return tableName;
    }

    private static <T> String getTableName(T bean) {
        return getTableName(bean.getClass());
    }
}