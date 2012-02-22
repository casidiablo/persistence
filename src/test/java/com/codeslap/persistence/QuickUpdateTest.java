package com.codeslap.persistence;

/**
 * @author cristian
 */
public class QuickUpdateTest extends UpdateTest{
    @Override
    protected SqlAdapter getAdapter() {
        return getQuickAdapter();
    }
}
