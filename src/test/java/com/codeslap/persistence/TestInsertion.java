package com.codeslap.persistence;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author cristian
 */
public class TestInsertion extends TestSqlite {

    @Test
    public void testSingleInsertion() {
        // create a simple object
        ExampleAutoincrement foo = new ExampleAutoincrement();
        foo.name = "Foo Bar";
        foo.number = 111;
        foo.decimal = 222f;
        foo.bool = true;

        // insert it into the database
        Object id = getAdapter().store(foo);

        // it should have inserted in the first record
        assertTrue(id instanceof Long);
        assertEquals(1L, ((Long) id).longValue());

        // if we retrieve all elements, it should be there in the first record
        List<ExampleAutoincrement> all = getAdapter().findAll(ExampleAutoincrement.class);
        assertEquals(1, all.size());
        assertEquals(foo, all.get(0));
    }

    @Test
    public void testCollectionInsertion() {
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

        // it should have stored all items
        assertEquals(collection.size(), getAdapter().count(ExampleAutoincrement.class));

        // now let's see if it stored everything
        for (ExampleAutoincrement exampleAutoincrement : collection) {
            ExampleAutoincrement found = getAdapter().findFirst(exampleAutoincrement);
            assertNotNull(found);
            exampleAutoincrement.id = found.id;
            assertEquals(exampleAutoincrement, found);
        }

        // now let's test test unique collection. In order to do so, we will remove half elements from
        // the original collection, and modify half elements of that sub-collection
        collection = getAdapter().findAll(ExampleAutoincrement.class);
        collection = collection.subList(0, collection.size() / 2);
        for (int i = 0, halfCollectionSize = collection.size() / 2; i < halfCollectionSize; i++) {
            ExampleAutoincrement foo = collection.get(i);
            foo.name = "Foo Bar Baz " + random.nextInt();
            foo.number = random.nextInt();
            foo.decimal = random.nextFloat();
            foo.bool = random.nextBoolean();
        }

        // now, using the store unique collection method there should be only 50 elements
        // it should have stored all items
        getAdapter().storeUniqueCollection(collection, null);
        assertEquals(collection.size(), getAdapter().count(ExampleAutoincrement.class));

        // and everything must have been saved correctly
        for (ExampleAutoincrement exampleAutoincrement : collection) {
            ExampleAutoincrement found = getAdapter().findFirst(exampleAutoincrement);
            assertNotNull(found);
            exampleAutoincrement.id = found.id;
            assertEquals(exampleAutoincrement, found);
        }
    }
}
