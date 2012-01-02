package com.egoclean.persistence;

/**
 * This class define a set of constraint for sqlite queries
 */
public class Constraint {
    private String mOrderBy;
    private int mLimit;

    public Constraint(int limit, String orderBy) {
        limit(limit).orderBy(orderBy);
    }

    private Constraint orderBy(String column) {
        mOrderBy = column;
        return this;
    }

    private Constraint limit(int limit) {
        mLimit = limit;
        return this;
    }
}
