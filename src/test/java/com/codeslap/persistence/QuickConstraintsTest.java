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
public class QuickConstraintsTest extends SqliteTest {
    @Test
    public void testConstraints() {
        List<ExampleAutoincrement> list = new ArrayList<ExampleAutoincrement>();
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            ExampleAutoincrement foo = new ExampleAutoincrement();
            foo.name = "Foo bar " + i;
            foo.number = random.nextInt();
            foo.decimal = random.nextFloat();
            foo.bool = i < 25;
            list.add(foo);
        }
        getNormalAdapter().storeCollection(list, null);

        ExampleAutoincrement sample = new ExampleAutoincrement();
        sample.bool = true;

        List<ExampleAutoincrement> all = getNormalAdapter().findAll(sample);
        assertEquals(list.size() / 2, all.size());

        Constraint constraint = new Constraint().limit(2).groupBy("name").orderBy("number DESC");
        List<ExampleAutoincrement> found = getNormalAdapter().findAll(sample, constraint);
        assertEquals(2, found.size());

        assertTrue(found.get(0).number >= found.get(1).number);
    }
}
