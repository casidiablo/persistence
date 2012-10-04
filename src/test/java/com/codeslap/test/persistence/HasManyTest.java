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

import com.codeslap.persistence.DatabaseSpec;
import com.codeslap.persistence.HasMany;
import com.codeslap.persistence.PersistenceConfig;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

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
        Object store = getAdapter().store(polyTheist);
        assertTrue(store instanceof Long);

        PolyTheist found = getAdapter().findFirst(PolyTheist.class, "_id = ?", new String[]{String.valueOf(store)});
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
        DatabaseSpec database = PersistenceConfig.registerSpec("failing_spec", 1);
        database.matchNotAutoIncrement(hasMany1);
        database.matchNotAutoIncrement(hasMany2);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWhenThroughDoesNotExist() {
        new HasMany(Cow.class, Bug.class, "something", true);
    }
}
