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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author cristian
 */
public class InsertionTest extends SqliteTest {
    @Test
    public void testSingleInsertion() {
        assertNull(getAdapter().store(null));
        // create a simple object
        ExampleAutoincrement foo = new ExampleAutoincrement();
        foo.name = "Foo Bar";
        foo.number = 111;
        foo.decimal = 222f;
        foo.bool = true;
        foo.blob = "Foo Bar".getBytes();

        // insert it into the database
        Object id = getAdapter().store(foo);

        // it should have inserted in the first record
        assertTrue(id instanceof Long);
        assertEquals(1L, ((Long) id).longValue());

        // if we retrieve all elements, it should be there in the first record
        List<ExampleAutoincrement> all = getAdapter().findAll(ExampleAutoincrement.class);
        assertEquals(1, all.size());
        assertEquals(foo, all.get(0));
        assertEquals(1, getAdapter().count(ExampleAutoincrement.class));
        assertEquals(1, getAdapter().count(ExampleAutoincrement.class, null, null));
        assertEquals(1, getAdapter().count(foo));
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
            foo.blob = foo.name.getBytes();
            collection.add(foo);
        }
        getAdapter().storeCollection(collection, null);

        // it should have stored all items
        assertEquals(collection.size(), getAdapter().findAll(ExampleAutoincrement.class, null, null).size());
        assertEquals(collection.size(), getAdapter().count(ExampleAutoincrement.class));
        assertEquals(collection.size(), getAdapter().count(ExampleAutoincrement.class, null, null));

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
            foo.blob = foo.name.getBytes();
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
        
        getAdapter().truncate(ExampleAutoincrement.class);
        assertEquals(0, getAdapter().findAll(ExampleAutoincrement.class, null, null).size());
        assertEquals(0, getAdapter().count(ExampleAutoincrement.class));
        assertEquals(0, getAdapter().count(ExampleAutoincrement.class, null, null));
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
        AnnotationAutoincrement bar = getAdapter().findFirst(AnnotationAutoincrement.class, "_id = 1", null);
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

    @Test
    public void testAnnotationNotAutoincrementInsertion() {
        assertNull(getAdapter().store(null));
        // create a simple object
        AnnotationNotAutoincrement foo = new AnnotationNotAutoincrement();
        foo.something = 3;
        foo.name = "Foo Bar";
        foo.lastName = "Darwin";
        foo.number = 111;
        foo.decimal = 222f;
        foo.bool = true;

        // insert it into the database
        Object id = getAdapter().store(foo);

        // it should have inserted in the first record
        assertTrue(id instanceof Long);
        assertEquals(3L, ((Long) id).longValue());
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
        AnnotationAutoincrement foo = new AnnotationAutoincrement();
        foo.name = "Blackened";
        foo.number = 111;
        foo.decimal = 222f;
        foo.bool = true;
        getAdapter().store(foo);

        AnnotationAutoincrement bar = getAdapter().findFirst(AnnotationAutoincrement.class, "char_sequence LIKE ?", new String[]{"Blackened"});
        assertEquals("Castiblanco", bar.lastName);
    }

    @Test
    public void testAttachedTo() {
        Cow cow = new Cow();
        cow.name = "Super Cow";

        Bug garrapata = new Bug();
        garrapata.itchFactor = new Random().nextFloat();

        Bug pulga = new Bug();
        pulga.itchFactor = new Random().nextFloat();

        Object store = getAdapter().store(cow);
        assertNotNull(store);

        cow.id = 1;
        store = getAdapter().store(cow);
        assertNotNull(store);
        
        cow.name = "Ugly Cow";
        store = getAdapter().store(cow);
        assertNotNull(store);

        List<Cow> cows = getAdapter().findAll(Cow.class, "name = 'Ugly Cow'", null);
        assertEquals(1, cows.size());
        List<Cow> cowsEquals = getAdapter().findAll(cow);
        assertEquals(cows, cowsEquals);

        getAdapter().storeCollection(Arrays.asList(garrapata, pulga), cow, null);

        List<Bug> bugs = getAdapter().findAll(Bug.class);
        assertEquals(2, bugs.size());
        List<Bug> sameBugs = getAdapter().findAll(Bug.class, "cow_id = ?", new String[]{"1"});
        assertEquals(bugs, sameBugs);

        getAdapter().truncate(Cow.class, Bug.class);
        assertTrue(getAdapter().findAll(Bug.class).isEmpty());
        assertTrue(getAdapter().findAll(Cow.class).isEmpty());

        Object id = getAdapter().store(cow);
        assertNotNull(id);
        assertTrue(id instanceof Long);
        assertEquals(1L, id);

        getAdapter().store(garrapata, cow);
        getAdapter().store(pulga, cow);

        bugs = getAdapter().findAll(Bug.class);
        assertEquals(2, bugs.size());
        sameBugs = getAdapter().findAll(Bug.class, "cow_id = ?", new String[]{"1"});
        assertEquals(bugs, sameBugs);
        List<Bug> allAttached = getAdapter().findAll(new Bug(), cow);
        assertEquals(bugs, allAttached);
    }

    @Test
    public void testStringPrimaryKey() {
        StringAsPrimaryKey sample = new StringAsPrimaryKey();
        sample.primaryKey = "baz";
        sample.foo = "bar";

        Object id = getAdapter().store(sample);
        assertNotNull(id);
        assertTrue(id instanceof String);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailStringPrimaryKeyNull() {
        StringAsPrimaryKey sample = new StringAsPrimaryKey();
        sample.foo = "bar";
        getAdapter().store(sample);
    }
}
