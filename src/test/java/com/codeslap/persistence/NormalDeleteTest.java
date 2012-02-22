package com.codeslap.persistence;

/**
 * @author cristian
 */
public class NormalDeleteTest extends DeleteTest{
    @Override
    protected SqlAdapter getAdapter() {
        return getNormalAdapter();
    }
}
