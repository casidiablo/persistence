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

import android.app.Activity;
import android.database.Cursor;
import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.RawQuery;
import com.codeslap.persistence.SqlAdapter;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author cristian
 */
public class RawQueryTest extends SqliteTest {
    @Test
    public void findAllByClassTest() {
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

        RawQuery rawQuery = Persistence.getRawQuery(new Activity());
        Cursor cursor = rawQuery.findAll("automatic", null, null, null, null, null, "number ASC", null);
        assertNotNull(cursor);
        assertEquals(100, cursor.getCount());
        assertTrue(cursor.moveToFirst());

        Collections.sort(collection, new Comparator<ExampleAutoincrement>() {
            @Override
            public int compare(ExampleAutoincrement foo, ExampleAutoincrement bar) {
                if (foo.number < 0 && bar.number >= 0) {
                    return -1;
                }
                if (foo.number >= 0 && bar.number < 0) {
                    return 1;
                }
                return foo.number - bar.number;
            }
        });

        int i = 0;
        do {
            ExampleAutoincrement item = collection.get(i);
            assertEquals(item.name, cursor.getString(cursor.getColumnIndex("name")));
            assertEquals(item.number, cursor.getInt(cursor.getColumnIndex("number")));
            assertEquals(item.decimal, cursor.getFloat(cursor.getColumnIndex("decimal")), 0.0f);
            assertEquals(item.bool, cursor.getInt(cursor.getColumnIndex("bool")) == 1);
            assertEquals(new String(item.blob), new String(cursor.getBlob(cursor.getColumnIndex("blob"))));
            i++;
        } while (cursor.moveToNext());
    }

    @Test
    public void findAllBySample() {
        ExampleAutoincrement foo = new ExampleAutoincrement();
        foo.name = "Foo Bar";
        foo.number = 111;
        foo.decimal = 222f;
        foo.bool = false;
        foo.blob = foo.name.getBytes();

        getAdapter().store(foo);

        foo = new ExampleAutoincrement();
        foo.name = "Bar Foo";
        foo.number = 333;
        foo.decimal = 444f;
        foo.bool = true;
        foo.blob = foo.name.getBytes();

        getAdapter().store(foo);

        RawQuery rawQuery = Persistence.getRawQuery(new Activity());
        ExampleAutoincrement sample = new ExampleAutoincrement();
        sample.bool = true;
        Cursor cursor = rawQuery.findAll(sample);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());

        assertEquals(2, cursor.getLong(cursor.getColumnIndex("_id")));
        assertEquals(foo.name, cursor.getString(cursor.getColumnIndex("name")));
        assertEquals(foo.number, cursor.getInt(cursor.getColumnIndex("number")));
        assertEquals(foo.decimal, cursor.getFloat(cursor.getColumnIndex("decimal")), 0.0f);
        assertEquals(foo.bool, cursor.getInt(cursor.getColumnIndex("bool")) == 1);
    }
    @Test
    public void findAllByQuery() {
        ExampleAutoincrement foo = new ExampleAutoincrement();
        foo.name = "Foo Bar";
        foo.number = 111;
        foo.decimal = 222f;
        foo.bool = false;
        foo.blob = foo.name.getBytes();

        getAdapter().store(foo);

        foo = new ExampleAutoincrement();
        foo.name = "Bar Foo";
        foo.number = 333;
        foo.decimal = 444f;
        foo.bool = true;
        foo.blob = foo.name.getBytes();

        getAdapter().store(foo);

        RawQuery rawQuery = Persistence.getRawQuery(new Activity());
        ExampleAutoincrement sample = new ExampleAutoincrement();
        sample.bool = true;
        Cursor cursor = rawQuery.findAll(ExampleAutoincrement.class, "bool = 1", null);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());

        assertEquals(2, cursor.getLong(cursor.getColumnIndex("_id")));
        assertEquals(foo.name, cursor.getString(cursor.getColumnIndex("name")));
        assertEquals(foo.number, cursor.getInt(cursor.getColumnIndex("number")));
        assertEquals(foo.decimal, cursor.getFloat(cursor.getColumnIndex("decimal")), 0.0f);
        assertEquals(foo.bool, cursor.getInt(cursor.getColumnIndex("bool")) == 1);
        assertEquals(new String(foo.blob), new String(cursor.getBlob(cursor.getColumnIndex("blob"))));

        Cursor rawCursor = rawQuery.rawQuery("SELECT * FROM automatic WHERE bool = 1");
        assertNotNull(rawCursor);
        assertTrue(rawCursor.moveToFirst());

        assertEquals(2, rawCursor.getLong(rawCursor.getColumnIndex("_id")));
        assertEquals(foo.name, rawCursor.getString(rawCursor.getColumnIndex("name")));
        assertEquals(foo.number, rawCursor.getInt(rawCursor.getColumnIndex("number")));
        assertEquals(foo.decimal, rawCursor.getFloat(rawCursor.getColumnIndex("decimal")), 0.0f);
        assertEquals(foo.bool, rawCursor.getInt(rawCursor.getColumnIndex("bool")) == 1);
        assertEquals(new String(foo.blob), new String(rawCursor.getBlob(rawCursor.getColumnIndex("blob"))));
    }

    @Test
    public void testAttachedTo() {
        Cow cow = new Cow();
        cow.name = "Super Cow";

        Bug garrapata = new Bug();
        garrapata.itchFactor = new Random().nextFloat();

        Bug pulga = new Bug();
        pulga.itchFactor = new Random().nextFloat();

        SqlAdapter adapter = getAdapter();
        
        Object store = adapter.store(cow);
        assertNotNull(store);

        cow.id = 1;
        store = adapter.store(cow);
        assertNotNull(store);

        cow.name = "Ugly Cow";
        store = adapter.store(cow);
        assertNotNull(store);

        List<Cow> cows = adapter.findAll(Cow.class, "name = 'Ugly Cow'", null);
        assertEquals(1, cows.size());
        List<Cow> cowsEquals = adapter.findAll(cow);
        assertEquals(cows, cowsEquals);

        adapter.storeCollection(Arrays.asList(garrapata, pulga), cow, null);

        List<Bug> bugs = adapter.findAll(Bug.class);
        assertEquals(2, bugs.size());
        List<Bug> sameBugs = adapter.findAll(Bug.class, "cow_id = ?", new String[]{"1"});
        assertEquals(bugs, sameBugs);

        adapter.truncate(Cow.class, Bug.class);
        assertTrue(adapter.findAll(Bug.class).isEmpty());
        assertTrue(adapter.findAll(Cow.class).isEmpty());

        Object id = adapter.store(cow);
        assertNotNull(id);
        assertTrue(id instanceof Long);
        assertEquals(1L, id);

        adapter.store(garrapata, cow);
        adapter.store(pulga, cow);

        bugs = adapter.findAll(Bug.class);
        assertEquals(2, bugs.size());
        sameBugs = adapter.findAll(Bug.class, "cow_id = ?", new String[]{"1"});
        assertEquals(bugs, sameBugs);
        RawQuery rawQuery = Persistence.getRawQuery(new Activity());
        Cursor allAttached = rawQuery.findAll(new Bug(), cow);
        assertNotNull(allAttached);
        assertEquals(bugs.size(),  allAttached.getCount());
        assertTrue(allAttached.moveToFirst());

        List<Bug> listFromCursor = new ArrayList<Bug>();
        do {
            long cowId = allAttached.getLong(allAttached.getColumnIndex("cow_id"));
            assertEquals(1, cowId);

            float itchFactor = allAttached.getFloat(allAttached.getColumnIndex("itch_factor"));
            Bug bug = new Bug();
            bug.itchFactor = itchFactor;
            listFromCursor.add(bug);
        } while (allAttached.moveToNext());

        Comparator<Bug> comparator = new Comparator<Bug>() {
            @Override
            public int compare(Bug foo, Bug bar) {
                return Float.floatToIntBits(foo.itchFactor) - Float.floatToIntBits(bar.itchFactor);
            }
        };
        Collections.sort(bugs, comparator);
        Collections.sort(listFromCursor, comparator);

        assertEquals(bugs, listFromCursor);
    }
}
