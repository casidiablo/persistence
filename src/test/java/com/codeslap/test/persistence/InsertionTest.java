/*
 * Copyright 2012 CodeSlap
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

import com.codeslap.persistence.SqlAdapter;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author cristian
 */
public abstract class InsertionTest extends SqliteTest {
    @Test
    public void testSingleInsertion() {
        assertNull(getAdapter().store(null));
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
        Assert.assertEquals(collection.size(), getAdapter().count(ExampleAutoincrement.class));

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
        Assert.assertEquals(collection.size(), getAdapter().count(ExampleAutoincrement.class));

        // and everything must have been saved correctly
        for (ExampleAutoincrement exampleAutoincrement : collection) {
            ExampleAutoincrement found = getAdapter().findFirst(exampleAutoincrement);
            assertNotNull(found);
            exampleAutoincrement.id = found.id;
            assertEquals(exampleAutoincrement, found);
        }
    }

    @Test
    public void testAnnotationInsertion() {
        assertNull(getAdapter().store(null));
        // create a simple object
        AnnotationAutoincrement foo = new AnnotationAutoincrement();
        foo.name = "Foo Bar";
        foo.lastName = "Darwin";
        foo.number = 111;
        foo.decimal = 222f;
        foo.bool = true;

        // insert it into the database
        Object id = getAdapter().store(foo);

        // it should have inserted in the first record
        assertTrue(id instanceof Long);
        assertEquals(1L, ((Long) id).longValue());

        // if we retrieve one element by ID it should be equal to the one inserted
        AnnotationAutoincrement bar = getAdapter().findFirst(AnnotationAutoincrement.class, "id = 1", null);
        assertEquals(foo, bar);

        // if we retrieve one element by name it should be equal to the one inserted
        bar = getAdapter().findFirst(AnnotationAutoincrement.class, "char_sequence LIKE ?", new String[]{"Foo Bar"});
        assertEquals(foo, bar);

        // if we retrieve one element by number it should be equal to the one inserted
        bar = getAdapter().findFirst(AnnotationAutoincrement.class, "signed = ?", new String[]{"111"});
        assertEquals(foo, bar);

        // if we retrieve one element by decimal it should be equal to the one inserted
        bar = getAdapter().findFirst(AnnotationAutoincrement.class, "value = ?", new String[]{"222"});
        assertEquals(foo, bar);

        // if we retrieve one element by bool it should be equal to the one inserted
        bar = getAdapter().findFirst(AnnotationAutoincrement.class, "active = ?", new String[]{"1"});
        assertEquals(foo, bar);

        // if we retrieve one element by bool but false, it it should be null
        bar = getAdapter().findFirst(AnnotationAutoincrement.class, "active = ?", new String[]{"0"});
        assertNull(bar);
    }

    @Test(expected = IllegalStateException.class)
    public void testNotNullWithoutDefaultValue() {
        AnnotationAutoincrement foo = new AnnotationAutoincrement();
        foo.decimal = 222f;
        foo.bool = true;
        getAdapter().store(foo);
    }

    @Test
    public void testNotNull() {
        configure();
        AnnotationAutoincrement foo = new AnnotationAutoincrement();
        foo.name = "Blackened";
        foo.number = 111;
        foo.decimal = 222f;
        foo.bool = true;
        getAdapter().store(foo);

        AnnotationAutoincrement bar = getAdapter().findFirst(AnnotationAutoincrement.class, "char_sequence LIKE ?", new String[]{"Blackened"});
        assertEquals("Castiblanco", bar.lastName);
    }

    protected abstract SqlAdapter getAdapter();
}
