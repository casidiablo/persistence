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

    public Constraint(int limit, String orderBy) {
        limit(limit).orderBy(orderBy);
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

    public String getOrderBy() {
        return mOrderBy;
    }

    public int getLimit() {
        return mLimit;
    }

    public String getGroupBy() {
        return mGroupBy;
    }
}
