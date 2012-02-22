package com.codeslap.persistence;

/**
 * @author cristian
 */
public class QuickDeleteTest extends DeleteTest{
    @Override
    protected SqlAdapter getAdapter() {
        return getQuickAdapter();
    }
}
