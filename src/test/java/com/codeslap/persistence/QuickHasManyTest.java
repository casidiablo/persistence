package com.codeslap.persistence;

/**
 * @author cristian
 */
public class QuickHasManyTest extends HasManyTest{
    @Override
    protected SqlAdapter getAdapter() {
        return getQuickAdapter();
    }
}
