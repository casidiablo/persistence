package com.egoclean.persistence;

import java.util.ArrayList;
import java.util.List;

/**
 * Predicate builder class... some examples of use:
 * new Predicate().that("name").like("Cris%").and().that("age").equalsTo("1");
 * new Predicate().that("week").notEqualsTo("2").or().that("week").notEqualsTo("4");
 */
public class Predicate {

    private boolean completeComparison = true;
    private String currentThat;
    private Object currentWhat;
    private StringBuilder builder;
    private boolean complete = true;
    private String lastGlue;

    private boolean orderByComplete = true;
    private String orderBy;
    private int limit;
    private String groupBy = "";
    private List<String> extraTables;

    public Predicate() {
        builder = new StringBuilder();
        extraTables = new ArrayList<String>();
    }

    public Predicate that(String s) {
        completeComparison = false;
        currentThat = s;
        return this;
    }

    public Predicate that(String table, String column) {
        if (!extraTables.contains(table)) {
            extraTables.add(table);
        }
        return that(table + "." + column);
    }

    public Predicate equalsTo(Object s) {
        return compare(s, " = ", "equalsTo");
    }

    public Predicate equalsTo(String tableName, String nameId) {
        if (!extraTables.contains(tableName)) {
            extraTables.add(tableName);
        }
        if (!completeComparison && currentThat != null && currentThat.length() > 0) {
            completeComparison = true;
            addGlue();
            builder.append(currentThat).append("=").append(tableName).append(".").append(nameId);
            currentThat = null;
            currentWhat = null;
            return this;
        }
        throw new RuntimeException("Called 'equalsTo' without having called 'that'");
    }

    public Predicate notEqualsTo(Object s) {
        return compare(s, " != ", "notEqualsTo");
    }

    public Predicate like(Object s) {
        return compare(s, " LIKE ", "like");
    }

    public Predicate lesserThan(Object o) {
        return compare(o, " < ", "lesserThan");
    }

    public Predicate greaterThan(Object o) {
        return compare(o, " > ", "greaterThan");
    }

    public Predicate in(Object... objects) {
        if (!completeComparison && currentThat != null && currentThat.length() > 0) {
            completeComparison = true;
            addGlue();
            String elements = "";
            String glue = "";
            for (Object o : objects) {
                elements += glue + o;
                glue = ", ";
            }
            builder.append(currentThat).append(" IN (").append(elements).append(")");
            currentThat = null;
            return this;
        }
        throw new RuntimeException("Called 'in' without having called 'that'");
    }

    public Predicate groupBy(String tableName, String column) {
        if (!extraTables.contains(tableName)) {
            extraTables.add(tableName);
        }
        groupBy = tableName + "." + column;
        return this;
    }

    private Predicate compare(Object s, String comparator, String name) {
        if (!completeComparison && currentThat != null && currentThat.length() > 0) {
            completeComparison = true;
            currentWhat = s;
            addLastCondition(comparator);
            currentThat = null;
            currentWhat = null;
            return this;
        }
        throw new RuntimeException("Called '" + name + "' without having called 'that'");
    }

    private void addLastCondition(String s) {
        addGlue();
        builder.append(currentThat).append(s).append("'").append(currentWhat).append("'");
    }

    private void addGlue() {
        if (lastGlue != null) {
            builder.append(lastGlue);
            lastGlue = null;
            complete = true;
        }
    }

    public Predicate and() {
        complete = false;
        lastGlue = " AND ";
        return this;
    }

    public Predicate or() {
        complete = false;
        lastGlue = " OR ";
        return this;
    }

    public Predicate orderBy(String column) {
        orderByComplete = false;
        orderBy = column;
        return this;
    }

    public Predicate asc() {
        if (!orderByComplete) {
            orderByComplete = true;
            orderBy += " ASC";
            return this;
        }
        throw new RuntimeException("'asc' called without having called 'orderBy'");
    }

    public Predicate desc() {
        if (!orderByComplete) {
            orderByComplete = true;
            orderBy += " DESC";
            return this;
        }
        throw new RuntimeException("'desc' called without having called 'orderBy'");
    }

    public String getWhere() {
        if (!complete) {
            addGlue();
            throw new RuntimeException("Malformed where condition: " + builder);
        } else if (!completeComparison) {
            throw new RuntimeException("Malformed where condition. You may have forgotten to " +
                    "use a comparator after calling 'that'");
        }
        return builder.toString();
    }

    public String getOrder() {
        if (orderBy == null) {
            return "";
        }
        if (!orderByComplete) {
            throw new RuntimeException("Malformed ORDER BY statement");
        }
        return orderBy;
    }

    public int getLimit() {
        return limit;
    }

    public Predicate limit(int l) {
        limit = l;
        return this;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public String[] getExtraTables() {
        if (extraTables.size() == 0) {
            return null;
        }
        return extraTables.toArray(new String[extraTables.size()]);
    }
}
