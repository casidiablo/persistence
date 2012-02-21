package com.codeslap.persistence;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author cristian
 */
public class TestDelete extends TestSqlite {
    @Test
    public void testDelete() {
        // let's first insert a collection of data
        List<ExampleAutoincrement> collection = new ArrayList<ExampleAutoincrement>();
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            ExampleAutoincrement foo = new ExampleAutoincrement();
            foo.name = "Foo Bar " + random.nextInt();
            foo.number = random.nextInt();
            foo.decimal = random.nextFloat();
            foo.bool = random.nextBoolean();
            collection.add(foo);
        }
        getAdapter().storeCollection(collection, null);

        // now let's delete some data!
        int deleted = getAdapter().delete(collection.get(0));
        assertEquals(1, deleted);

        ExampleAutoincrement foo = collection.get(1);
        foo.name = null;
        assertTrue(getAdapter().delete(foo) > 0);

        int count = getAdapter().count(ExampleAutoincrement.class);
        assertTrue(count > 0);

        deleted = getAdapter().delete(ExampleAutoincrement.class, "name LIKE ?", new String[]{"Foo%"});
        assertTrue(deleted > 0);

        count = getAdapter().count(ExampleAutoincrement.class);
        assertEquals(0, count);
    }
}
