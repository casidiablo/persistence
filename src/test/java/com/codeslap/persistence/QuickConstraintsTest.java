package com.codeslap.persistence;

/**
 * @author cristian
 */
public class QuickConstraintsTest extends ConstraintsTest{
    @Override
    protected SqlAdapter getAdapter() {
        return getQuickAdapter();
    }
}
