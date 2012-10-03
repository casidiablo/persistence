package com.codeslap.test.persistence;

import com.codeslap.persistence.SqlAdapter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CollectionInsertionTest extends SqliteTest {
    @Test
    public void testCollectionInsertionTest() {
        // create list to save
        List<ExampleAutoincrement> collection = new ArrayList<ExampleAutoincrement>();
        Random random = new Random();
        for (int i = 0; i < 50000; i++) {
            ExampleAutoincrement foo = new ExampleAutoincrement();
            foo.name = "Foo Bar " + random.nextInt();
            foo.number = random.nextInt();
            foo.decimal = random.nextFloat();
            foo.bool = random.nextBoolean();
            foo.blob = foo.name.getBytes();
            collection.add(foo);
        }

        // save collection using progress listener
        getAdapter().storeCollection(collection, new SqlAdapter.ProgressListener() {
            @Override
            public void onProgressChange(int percentage) {
            }
        });
        performTesting(collection);

        // save collection without progress listener to speed things up
        getAdapter().delete(ExampleAutoincrement.class, null, null);
        getAdapter().storeCollection(collection, null);
        performTesting(collection);
    }

    private void performTesting(List<ExampleAutoincrement> collection) {
        // it should have stored all items
        assertEquals(collection.size(), getAdapter().findAll(ExampleAutoincrement.class, null, null).size());
        assertEquals(collection.size(), getAdapter().count(ExampleAutoincrement.class));
        assertEquals(collection.size(), getAdapter().count(ExampleAutoincrement.class, null, null));

        // now let's see if it stored everything
        int i = 0;
        for (ExampleAutoincrement exampleAutoincrement : collection) {
            exampleAutoincrement.id = 0;
            ExampleAutoincrement found = getAdapter().findFirst(exampleAutoincrement);
            assertNotNull(found);
            exampleAutoincrement.id = found.id;
            assertEquals(exampleAutoincrement, found);
            i++;
            if (i > 1000) {
                // just check first 1000
                break;
            }
        }
    }
}
