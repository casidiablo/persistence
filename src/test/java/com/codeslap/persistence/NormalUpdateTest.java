package com.codeslap.persistence;

/**
 * @author cristian
 */
public class NormalUpdateTest extends UpdateTest{
    @Override
    protected SqlAdapter getAdapter() {
        return getNormalAdapter();
    }
}
