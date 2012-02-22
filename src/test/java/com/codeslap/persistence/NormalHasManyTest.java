package com.codeslap.persistence;

/**
 * @author cristian
 */
public class NormalHasManyTest extends HasManyTest{
    @Override
    protected SqlAdapter getAdapter() {
        return getNormalAdapter();
    }
}
