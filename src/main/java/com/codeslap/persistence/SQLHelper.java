/*
 * Copyright 2013 CodeSlap
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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class SQLHelper {

    static final String ID = "id";
    static final String _ID = "_id";
    static final String PRIMARY_KEY = "%s INTEGER PRIMARY KEY";
    private static final String HEXES = "0123456789ABCDEF";

    private static final Map<Class<?>, String> INSERT_COLUMNS_CACHE = new HashMap<Class<?>, String>();
    private static final Map<Class<?>, String> TABLE_NAMES_CACHE = new HashMap<Class<?>, String>();
    private static final Map<Field, String> COLUMN_NAMES_CACHE = new HashMap<Field, String>();
    private static final Map<Class<?>, Field[]> FIELDS_CACHE = new HashMap<Class<?>, Field[]>();
    static final String SELECT_AUTOINCREMENT_FORMAT = "(SELECT seq FROM sqlite_sequence WHERE name = '%s')";

    static final String STATEMENT_SEPARATOR = "b05f72bb_STATEMENT_SEPARATOR";

    public static String getCreateTableSentence(Class clazz, DatabaseSpec databaseSpec) {
        List<String> fieldSentences = new ArrayList<String>();
        // loop through all the fields and add sql statements
        Field[] declaredFields = SQLHelper.getDeclaredFields(clazz);
        List<String> columns = new ArrayList<String>();
        for (Field field : declaredFields) {
            String columnName = getColumnName(field);
            if (isPrimaryKey(field)) {
                String primaryKeySentence = getCreatePrimaryKey(field);
                if (field.getType() == String.class) {// what types are supported
                    primaryKeySentence = primaryKeySentence.replace("INTEGER PRIMARY KEY", "TEXT PRIMARY KEY");
                } else if (databaseSpec.isAutoincrement(clazz)) {
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
        HasMany belongsTo = databaseSpec.belongsTo(clazz);
        if (belongsTo != null) {
            // if so, add a new field to the table creation statement to create the relation
            Class<?> containerClass = belongsTo.getContainerClass();
            Field field = belongsTo.getThroughField();
            String columnName = String.format("%s_%s", normalize(containerClass.getSimpleName()), normalize(belongsTo.getThroughField().getName()));
            if (!columns.contains(columnName)) {
                fieldSentences.add(getFieldSentence(columnName, field.getType(), true));
                columns.add(getColumnName(field));
            }
        }

        // sort sentences
        Collections.sort(fieldSentences, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                if (s1.contains(String.format(PRIMARY_KEY, ""))) {
                    return -1;
                }
                if (s2.contains(String.format(PRIMARY_KEY, ""))) {
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

    static Field[] getDeclaredFields(Class theClass) {
        if (!FIELDS_CACHE.containsKey(theClass)) {
            List<Field> list = new ArrayList<Field>();
            for (Field field : theClass.getDeclaredFields()) {
                // - If it has the ignore annotation, ignore it.
                // - Oh, really? What a brilliant idea.
                if (!field.isAnnotationPresent(Ignore.class) &&
                        !Modifier.isStatic(field.getModifiers()) &&// ignore static fields
                        !Modifier.isFinal(field.getModifiers())) {// ignore final fields
                    list.add(field);
                }
            }
            FIELDS_CACHE.put(theClass, list.toArray(new Field[list.size()]));
        }
        return FIELDS_CACHE.get(theClass);
    }

    /**
     * @param name    the name of the field
     * @param type    the type
     * @param notNull true if the column should be not null
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
        if (type == byte[].class || type == Byte[].class) {
            return String.format("%s BLOB%s", name, notNullSentence);
        }
        return String.format("%s TEXT%s", name, notNullSentence);
    }

    static <T, G> String getWhere(Class<?> theClass, T bean, List<String> args, G attachedTo, DatabaseSpec databaseSpec) {
        List<String> conditions = new ArrayList<String>();
        if (bean != null) {
            Class<?> clazz = bean.getClass();
            Field[] fields = getDeclaredFields(clazz);
            for (Field field : fields) {
                if (field.getType() == byte[].class || field.getType() == Byte[].class) {
                    continue;
                }
                try {
                    Class<?> type = field.getType();
                    if (type == List.class) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(bean);
                    if (!hasData(type, value)) {
                        continue;
                    }
                    String columnName = getColumnName(field);
                    if (args == null) {
                        if (field.getType() == String.class) {
                            conditions.add(String.format("%s LIKE '%s'", columnName,
                                    String.valueOf(value).replace("'", "''")));
                        } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                            int intValue = (Boolean) value ? 1 : 0;
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
                            value = (Boolean) value ? 1 : 0;
                        }
                        args.add(String.valueOf(value));
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }

        // if there is an attachment
        if (attachedTo != null) {
            switch (databaseSpec.getRelationship(attachedTo.getClass(), theClass)) {
                case HAS_MANY: {
                    try {
                        HasMany hasMany = databaseSpec.belongsTo(theClass);
                        Field primaryForeignKey = hasMany.getThroughField();
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
            Field[] fields = getDeclaredFields(bean.getClass());
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
                            int intValue = (Boolean) value ? 1 : 0;
                            sets.add(String.format("%s = '%d'", getColumnName(field), intValue));
                        } else if (field.getType() == byte[].class || field.getType() == Byte[].class) {
                            String hex = getHex((byte[]) value);
                            sets.add(String.format("%s = X'%s'", getColumnName(field), hex));
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
        if (type == byte[].class || type == Byte[].class) {
            return value != null && ((byte[]) value).length > 0;
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
    public static String getColumnName(Field field) {
        if (COLUMN_NAMES_CACHE.containsKey(field)) {
            return COLUMN_NAMES_CACHE.get(field);
        }
        if (isPrimaryKey(field) && !forcedName(field)) {
            return getIdColumn(field);
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

    static <T> String buildUpdateStatement(T bean, Object sample, DatabaseSpec databaseSpec) {
        String where = getWhere(bean.getClass(), sample, null, null, databaseSpec);
        String set = getSet(bean);
        return String.format("UPDATE %s SET %s WHERE %s;%s", getTableName(bean), set, where, STATEMENT_SEPARATOR);
    }

    public static <T> String buildUpdateStatement(T bean, String where) {
        String set = getSet(bean);
        return String.format("UPDATE %s SET %s WHERE %s;%s", getTableName(bean), set, where, STATEMENT_SEPARATOR);
    }

    static <T, G> String getInsertStatement(T bean, G attachedTo, DatabaseSpec persistence) {
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
            return String.format("INSERT OR IGNORE INTO %s (%s) VALUES (%s);%s", getTableName(bean), getIdColumn(getPrimaryKeyField(bean.getClass())), hack, STATEMENT_SEPARATOR);
        }
        return String.format("INSERT OR IGNORE INTO %s (%s) VALUES (%s);%s", getTableName(bean), columnsSet, join(values, ", "), STATEMENT_SEPARATOR);
    }

    private static <T, G> void populateColumnsAndValues(T bean, G attachedTo, List<String> values, List<String> columns, DatabaseSpec persistence) {
        if (bean == null) {
            return;
        }
        Class<?> theClass = bean.getClass();
        Field[] fields = getDeclaredFields(theClass);
        for (Field field : fields) {
            // if the class has an autoincrement, ignore the ID
            if (isPrimaryKey(field) && persistence.isAutoincrement(theClass)) {
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
                if (values == null) {
                    continue;
                }
                if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                    int intValue = (Boolean) value ? 1 : 0;
                    values.add(String.valueOf(intValue));
                } else if (field.getType() == Byte[].class || field.getType() == byte[].class) {
                    if (value == null) {
                        values.add("NULL");
                    } else {
                        String hex = getHex((byte[]) value);
                        values.add(String.format("X'%s'", hex));
                    }
                } else if (value == null) {
                    Column columnAnnotation = field.getAnnotation(Column.class);
                    boolean hasDefault = false;
                    if (columnAnnotation != null) {
                        hasDefault = !columnAnnotation.defaultValue().equals(Column.NULL);
                    }
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
            } catch (IllegalAccessException ignored) {
            }
        }
        if (attachedTo != null) {
            switch (persistence.getRelationship(attachedTo.getClass(), bean.getClass())) {
                case HAS_MANY: {
                    try {
                        HasMany hasMany = persistence.belongsTo(bean.getClass());
                        Field primaryForeignKey = hasMany.getThroughField();
                        primaryForeignKey.setAccessible(true);
                        Object foreignValue = primaryForeignKey.get(attachedTo);
                        if (columns != null) {
                            columns.add(hasMany.getForeignKey());
                        }
                        if (values != null) {
                            if (foreignValue != null && hasData(foreignValue.getClass(), foreignValue)) {
                                values.add(String.valueOf(foreignValue));
                            } else {
                                values.add(String.format(SELECT_AUTOINCREMENT_FORMAT, getTableName(attachedTo.getClass())));
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private static boolean isPrimaryKey(Field field) {
        if (field.isAnnotationPresent(PrimaryKey.class)) {
            return true;
        }
        return field.getName().equals(ID) || field.getName().equals(getIdColumn(field));
    }

    private static boolean forcedName(Field field) {
        return field.isAnnotationPresent(Column.class) && field.getAnnotation(Column.class).forceName();
    }

    public static String getTableName(Class<?> theClass) {
        if (TABLE_NAMES_CACHE.containsKey(theClass)) {
            return TABLE_NAMES_CACHE.get(theClass);
        }
        Table table = theClass.getAnnotation(Table.class);
        String tableName;
        if (table != null) {
            tableName = table.value();
            if (TextUtils.isEmpty(tableName)) {
                String msg = String.format("You cannot leave a table name empty (class %s)", theClass.getSimpleName());
                throw new IllegalArgumentException(msg);
            }
            if (tableName.contains(" ")) {
                String msg = String.format("Table name cannot have spaces: '%s'; found in class %s",
                        tableName, theClass.getSimpleName());
                throw new IllegalArgumentException(msg);
            }
        } else {
            String name = theClass.getSimpleName();
            if (name.endsWith("y")) {
                name = name.substring(0, name.length() - 1) + "ies";
            } else if (!name.endsWith("s")) {
                name += "s";
            }
            tableName = normalize(name);
        }
        TABLE_NAMES_CACHE.put(theClass, tableName);
        return tableName;
    }

    private static <T> String getTableName(T bean) {
        return getTableName(bean.getClass());
    }

    private static String getHex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4))
                    .append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    /**
     * @param theClass the class to the get primary key from
     * @return the primary key from a class
     */
    static String getPrimaryKey(Class<?> theClass) {
        for (Field field : getDeclaredFields(theClass)) {
            if (isPrimaryKey(field)) {
                return field.getName();
            }
        }
        throw new IllegalStateException("Class " + theClass + " does not have a primary key");
    }

    /**
     * @param theClass the class to the get primary key from
     * @return the primary key field from a class
     */
    static Field getPrimaryKeyField(Class<?> theClass) {
        for (Field field : getDeclaredFields(theClass)) {
            if (isPrimaryKey(field)) {
                return field;
            }
        }
        throw new IllegalStateException("Class " + theClass + " does not have a primary key");
    }

    /**
     * @param theClass the class to inspect
     * @return the primary key column name
     */
    public static String getPrimaryKeyColumnName(Class<?> theClass) {
        Field collectionId = getPrimaryKeyField(theClass);
        return getIdColumn(collectionId);
    }

    static String getIdColumn(Field field) {
        if (forcedName(field)) {
            return getColumnName(field);
        }
        return _ID;
    }

    static String getCreatePrimaryKey(Field field) {
        return String.format(PRIMARY_KEY, getIdColumn(field));
    }

    static <T, G> Cursor getCursorFindAllWhere(SQLiteDatabase db, Class<? extends T> clazz, T sample, G attachedTo,
                                               Constraint constraint, DatabaseSpec databaseSpec) {
        String[] selectionArgs = null;
        String where = null;
        if (sample != null || attachedTo != null) {
            ArrayList<String> args = new ArrayList<String>();
            where = getWhere(clazz, sample, args, attachedTo, databaseSpec);
            if (TextUtils.isEmpty(where)) {
                where = null;
            } else {
                selectionArgs = args.toArray(new String[args.size()]);
            }
        }
        String orderBy = null;
        String limit = null;
        String groupBy = null;
        if (constraint != null) {
            orderBy = constraint.getOrderBy();
            if (constraint.getLimit() != null) {
                limit = constraint.getLimit().toString();
            }
            groupBy = constraint.getGroupBy();
        }
        return db.query(getTableName(clazz), null, where, selectionArgs, groupBy, null, orderBy, limit);
    }

    static <T> String getFastInsertSqlHeader(T bean, DatabaseSpec persistence) {
        ArrayList<String> values = new ArrayList<String>();
        ArrayList<String> columns = new ArrayList<String>();
        populateColumnsAndValues(bean, null, values, columns, persistence);

        StringBuilder result = new StringBuilder();

        result.append("INSERT OR IGNORE INTO ").append(getTableName(bean.getClass())).append(" ");
        // set insert columns
        result.append("(");
        result.append(join(columns, ", "));
        result.append(")");
        // add first insertion body
        result.append(" SELECT ");

        ArrayList<String> columnsAndValues = new ArrayList<String>();
        for (int i = 0, valuesSize = values.size(); i < valuesSize; i++) {
            String column = columns.get(i);
            String value = values.get(i);
            StringBuilder columnAndValue = new StringBuilder();
            columnAndValue.append(value).append(" AS ").append(column);
            columnsAndValues.add(columnAndValue.toString());
        }
        result.append(join(columnsAndValues, ", "));
        return result.toString();
    }

    static <T> String getUnionInsertSql(T bean, DatabaseSpec persistence) {
        ArrayList<String> values = new ArrayList<String>();
        populateColumnsAndValues(bean, null, values, null, persistence);
        StringBuilder builder = new StringBuilder();
        builder.append(" UNION SELECT ");
        builder.append(join(values, ", "));
        return builder.toString();
    }
}
