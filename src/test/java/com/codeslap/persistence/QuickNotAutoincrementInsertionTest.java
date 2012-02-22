package com.codeslap.persistence;

/**
 * @author cristian
 */
public class QuickNotAutoincrementInsertionTest extends NotAutoincrementInsertionTest{
    @Override
    protected SqlAdapter getAdapter() {
        return getQuickAdapter();
    }
}
