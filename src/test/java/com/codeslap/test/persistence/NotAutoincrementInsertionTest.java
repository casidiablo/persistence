/*
 * Copyright 2013 CodeSlap
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeslap.test.persistence;

import com.codeslap.robolectric.RobolectricSimpleRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author cristian
 */
@RunWith(RobolectricSimpleRunner.class)
public class NotAutoincrementInsertionTest extends SqliteTest {

    @Test
    public void testSingleInsertion() {
        // create a simple object
        ExampleNotAutoincrement foo = new ExampleNotAutoincrement();
        foo.id = 1;
        foo.name = "Foo Bar";
        foo.number = 111;
        foo.decimal = 222f;
        foo.bool = true;

        // insert it into the database
        Object id = getAdapter().store(foo);

        // it should have inserted in the first record
        assertTrue(id instanceof Long);
        foo.id = (Long) id;
        assertEquals(1L, ((Long) id).longValue());

        // if we retrieve all elements, it should be there in the first record
        List<ExampleNotAutoincrement> all = getAdapter().findAll(ExampleNotAutoincrement.class);
        assertEquals(1, all.size());
        assertEquals(foo, all.get(0));
    }

    @Test
    public void testCollectionInsertion() {
        List<ExampleNotAutoincrement> collection = new ArrayList<ExampleNotAutoincrement>();
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            ExampleNotAutoincrement foo = new ExampleNotAutoincrement();
            foo.id = i + 1;
            foo.name = "Foo Bar " + random.nextInt();
            foo.number = random.nextInt();
            foo.decimal = random.nextFloat();
            foo.bool = random.nextBoolean();
            collection.add(foo);
        }
        getAdapter().storeCollection(collection, null);

        // it should have stored all items
        Assert.assertEquals(collection.size(), getAdapter().count(ExampleNotAutoincrement.class));

        // now let's see if it stored everything
        for (ExampleNotAutoincrement ExampleNotAutoincrement : collection) {
            ExampleNotAutoincrement found = getAdapter().findFirst(ExampleNotAutoincrement);
            assertNotNull(found);
            ExampleNotAutoincrement.id = found.id;
            assertEquals(ExampleNotAutoincrement, found);
        }

        // now let's test test unique collection. In order to do so, we will remove half elements from
        // the original collection, and modify half elements of that sub-collection
        collection = getAdapter().findAll(ExampleNotAutoincrement.class);
        collection = collection.subList(0, collection.size() / 2);
        for (int i = 0, halfCollectionSize = collection.size() / 2; i < halfCollectionSize; i++) {
            ExampleNotAutoincrement foo = collection.get(i);
            foo.name = "Foo Bar Baz " + random.nextInt();
            foo.number = random.nextInt();
            foo.decimal = random.nextFloat();
            foo.bool = random.nextBoolean();
        }

        // now, using the store unique collection method there should be only 50 elements
        // it should have stored all items
        getAdapter().storeUniqueCollection(collection, null);
        Assert.assertEquals(collection.size(), getAdapter().count(ExampleNotAutoincrement.class));

        // and everything must have been saved correctly
        for (ExampleNotAutoincrement ExampleNotAutoincrement : collection) {
            ExampleNotAutoincrement found = getAdapter().findFirst(ExampleNotAutoincrement);
            assertNotNull(found);
            ExampleNotAutoincrement.id = found.id;
            assertEquals(ExampleNotAutoincrement, found);
        }
    }
}
