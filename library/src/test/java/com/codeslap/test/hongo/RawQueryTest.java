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

package com.codeslap.test.hongo;

import android.app.Activity;
import android.database.Cursor;
import com.codeslap.hongo.Hongo;
import com.codeslap.hongo.RawQuery;
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
    mAdapter.storeCollection(collection, null);

    RawQuery rawQuery = Hongo.getRawQuery(new Activity());
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

    mAdapter.store(foo);

    foo = new ExampleAutoincrement();
    foo.name = "Bar Foo";
    foo.number = 333;
    foo.decimal = 444f;
    foo.bool = true;
    foo.blob = foo.name.getBytes();

    mAdapter.store(foo);

    RawQuery rawQuery = Hongo.getRawQuery(new Activity());
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

    mAdapter.store(foo);

    foo = new ExampleAutoincrement();
    foo.name = "Bar Foo";
    foo.number = 333;
    foo.decimal = 444f;
    foo.bool = true;
    foo.blob = foo.name.getBytes();

    mAdapter.store(foo);

    RawQuery rawQuery = Hongo.getRawQuery(new Activity());
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
}
