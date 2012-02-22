package com.codeslap.persistence;

/**
 * @author cristian
 */
public class NormalNotAutoincrementInsertionTest extends NotAutoincrementInsertionTest{
    @Override
    protected SqlAdapter getAdapter() {
        return getNormalAdapter();
    }
}
