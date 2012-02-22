package com.codeslap.persistence;

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
        getNormalAdapter().store(foo);

        // now, let's create a new object with different data
        ExampleAutoincrement bar = new ExampleAutoincrement();
        String name = bar.name = "Cristo Loco";
        int number = bar.number = random.nextInt();
        float decimal = bar.decimal = random.nextFloat();
        boolean bool = bar.bool = random.nextBoolean();

        // after updating this record, all its data should have changed...
        getNormalAdapter().update(bar, foo);
        ExampleAutoincrement baz = getNormalAdapter().findAll(ExampleAutoincrement.class).get(0);
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
        getNormalAdapter().store(foo);

        // now, let's create a new object with different data
        ExampleAutoincrement bar = new ExampleAutoincrement();
        String name = bar.name = "Cristo Loco";
        int number = bar.number = random.nextInt();
        float decimal = bar.decimal = random.nextFloat();
        boolean bool = bar.bool = random.nextBoolean();

        // after updating this record, all its data should have changed...
        getNormalAdapter().update(bar, "name LIKE ?", new String[]{foo.name});
        ExampleAutoincrement baz = getNormalAdapter().findAll(ExampleAutoincrement.class).get(0);
        assertEquals(name, baz.name);
        assertEquals(number, baz.number);
        assertEquals(decimal, baz.decimal, 0.0);
        assertEquals(bool, baz.bool);
    }
}
