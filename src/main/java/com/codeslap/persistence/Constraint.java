package com.codeslap.persistence;

/**
 * This class define a set of constraint for sqlite queries
 */
public class Constraint {
    private String mOrderBy;
    private int mLimit;
    private String mGroupBy;

    public Constraint() {
    }

    public Constraint orderBy(String column) {
        mOrderBy = column;
        return this;
    }

    public Constraint limit(int limit) {
        mLimit = limit;
        return this;
    }

    public Constraint groupBy(String groupBy) {
        mGroupBy = groupBy;
        return this;
    }

    String getOrderBy() {
        return mOrderBy;
    }

    int getLimit() {
        return mLimit;
    }

    String getGroupBy() {
        return mGroupBy;
    }
}
