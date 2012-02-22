package com.codeslap.persistence;

/**
 * @author cristian
 */
public class NormalConstraintsTest extends ConstraintsTest{

    @Override
    protected SqlAdapter getAdapter() {
        return getNormalAdapter();
    }
}
