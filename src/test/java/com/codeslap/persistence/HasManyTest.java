package com.codeslap.persistence;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author cristian
 */
public class HasManyTest extends SqliteTest {
    @Test
    public void testHasMany() {
        // let's create some gods. We men have a lot of practice creating gods
        List<God> gods = new ArrayList<God>();
        Random random = new Random();
        for (String name : new String[]{"Jesús", "Shiva", "Ganesh", "Odin"}) {
            God god = new God();
            god.name = name;
            god.power = random.nextFloat();
            gods.add(god);
        }

        PolyTheist polyTheist = new PolyTheist();
        polyTheist.gods = gods;

        // let's save our polytheist friend...
        Object store = getNormalAdapter().store(polyTheist);
        assertTrue(store instanceof Long);

        PolyTheist found = getNormalAdapter().findFirst(PolyTheist.class, "id = ?", new String[]{String.valueOf(store)});
        assertNotNull(found);
        assertEquals(polyTheist, found);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailWithDuplicatedClasses() {
        new HasMany(ExampleAutoincrement.class, ExampleAutoincrement.class);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithDuplicatedRelations() {
        HasMany hasMany1 = new HasMany(Author.class, Book.class);
        HasMany hasMany2 = new HasMany(Book.class, Author.class);
        SqlPersistence database = PersistenceConfig.getDatabase("foo.db", 1);
        database.matchNotAutoIncrement(hasMany1);
        database.matchNotAutoIncrement(hasMany2);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWhenThroughDoesNotExist() {
        new HasMany(Cow.class, Bug.class, "something", true);
    }
}
