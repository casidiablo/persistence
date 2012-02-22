package com.codeslap.persistence;

import com.codeslap.robolectric.RobolectricSimpleRunner;
import org.junit.runner.RunWith;

/**
 * @author cristian
 */
@RunWith(RobolectricSimpleRunner.class)
public class QuickInsertionTest extends InsertionTest{
    @Override
    public SqlAdapter getAdapter() {
        return getQuickAdapter();
    }
}
