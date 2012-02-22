package com.codeslap.persistence;

/**
 * @author cristian
 */
public class QuickManyToManyTest extends ManyToManyTest{
    @Override
    protected SqlAdapter getAdapter() {
        return getQuickAdapter();
    }
}
