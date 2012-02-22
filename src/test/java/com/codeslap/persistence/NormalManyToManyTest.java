package com.codeslap.persistence;

/**
 * @author cristian
 */
public class NormalManyToManyTest extends ManyToManyTest{
    @Override
    protected SqlAdapter getAdapter() {
        return getNormalAdapter();
    }
}
