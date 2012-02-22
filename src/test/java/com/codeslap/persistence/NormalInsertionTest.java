package com.codeslap.persistence;

/**
 * @author cristian
 */
public class NormalInsertionTest extends InsertionTest{
    @Override
    public SqlAdapter getAdapter() {
        return getNormalAdapter();
    }
}
