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

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author cristian
 */
public class UpdateTest extends SqliteTest {
    @Test
    public void updateTest() {
        // let's insert a record
        Random random = new Random();
        ExampleAutoincrement foo = new ExampleAutoincrement();
        foo.name = "Cristo Loco";
        foo.number = random.nextInt();
        foo.decimal = random.nextFloat();
        foo.bool = random.nextBoolean();
        foo.blob = foo.name.getBytes();
        getAdapter().store(foo);

        // now, let's create a new object with different data
        ExampleAutoincrement bar = new ExampleAutoincrement();
        String name = bar.name = "Cristo Loco";
        int number = bar.number = random.nextInt();
        float decimal = bar.decimal = random.nextFloat();
        boolean bool = bar.bool = random.nextBoolean();
        byte[] blob = bar.blob = bar.name.getBytes();

        // after updating this record, all its data should have changed...
        int update = getAdapter().update(bar, foo);
        assertEquals(1, update);
        ExampleAutoincrement baz = getAdapter().findAll(ExampleAutoincrement.class).get(0);
        assertEquals(name, baz.name);
        assertEquals(number, baz.number);
        assertEquals(decimal, baz.decimal, 0.0);
        assertEquals(bool, baz.bool);
        assertEquals(new String(blob), new String(baz.blob));
    }
    @Test
    public void updateTestAnnotations() {
        // let's insert a record
        Random random = new Random();
        AnnotationAutoincrement foo = new AnnotationAutoincrement();
        foo.name = "Cristo Loco";
        foo.number = random.nextInt();
        foo.decimal = random.nextFloat();
        foo.bool = random.nextBoolean();
        getAdapter().store(foo);

        // now, let's create a new object with different data
        AnnotationAutoincrement bar = new AnnotationAutoincrement();
        String name = bar.name = "Cristo Loco";
        int number = bar.number = random.nextInt();
        float decimal = bar.decimal = random.nextFloat();
        boolean bool = bar.bool = random.nextBoolean();

        // after updating this record, all its data should have changed...
        int update = getAdapter().update(bar, foo);
        assertEquals(1, update);
        AnnotationAutoincrement baz = getAdapter().findAll(AnnotationAutoincrement.class).get(0);
        assertEquals(name, baz.name);
        assertEquals(number, baz.number);
        assertEquals(decimal, baz.decimal, 0.0);
        assertEquals(bool, baz.bool);
    }

    @Test
    public void manualUpdateTest() {
        // let's insert a record
        Random random = new Random();
        ExampleAutoincrement foo = new ExampleAutoincrement();
        foo.name = "Cristo Loco";
        foo.number = random.nextInt();
        foo.decimal = random.nextFloat();
        foo.bool = random.nextBoolean();
        foo.blob = foo.name.getBytes();
        getAdapter().store(foo);

        // now, let's create a new object with different data
        ExampleAutoincrement bar = new ExampleAutoincrement();
        String name = bar.name = "Cristo Loco";
        int number = bar.number = random.nextInt();
        float decimal = bar.decimal = random.nextFloat();
        boolean bool = bar.bool = random.nextBoolean();
        byte[] blob = bar.blob = bar.name.getBytes();

        // after updating this record, all its data should have changed...
        int update = getAdapter().update(bar, "name LIKE ?", new String[]{foo.name});
        assertEquals(1, update);
        ExampleAutoincrement baz = getAdapter().findAll(ExampleAutoincrement.class).get(0);
        assertEquals(name, baz.name);
        assertEquals(number, baz.number);
        assertEquals(decimal, baz.decimal, 0.0);
        assertEquals(bool, baz.bool);
        assertEquals(new String(blob), new String(baz.blob));
    }

    @Test
    public void manualUpdateTestAnnotation() {
        // let's insert a record
        Random random = new Random();
        AnnotationAutoincrement foo = new AnnotationAutoincrement();
        foo.name = "Cristo Loco";
        foo.number = random.nextInt();
        foo.decimal = random.nextFloat();
        foo.bool = random.nextBoolean();
        getAdapter().store(foo);

        // now, let's create a new object with different data
        AnnotationAutoincrement bar = new AnnotationAutoincrement();
        String name = bar.name = "Cristo Loco";
        int number = bar.number = random.nextInt();
        float decimal = bar.decimal = random.nextFloat();
        boolean bool = bar.bool = random.nextBoolean();

        // after updating this record, all its data should have changed...
        int update = getAdapter().update(bar, "char_sequence LIKE ?", new String[]{foo.name});
        assertEquals(1, update);
        AnnotationAutoincrement baz = getAdapter().findAll(AnnotationAutoincrement.class).get(0);
        assertEquals(name, baz.name);
        assertEquals(number, baz.number);
        assertEquals(decimal, baz.decimal, 0.0);
        assertEquals(bool, baz.bool);
    }
}
