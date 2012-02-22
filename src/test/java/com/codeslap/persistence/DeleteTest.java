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

package com.codeslap.persistence;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author cristian
 */
public abstract class DeleteTest extends SqliteTest {
    @Test
    public void testDelete() {
        // let's first insert a collection of data
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

        // now let's delete some data!
        int deleted = getAdapter().delete(collection.get(0));
        assertEquals(1, deleted);

        ExampleAutoincrement foo = collection.get(1);
        foo.name = null;
        assertTrue(getAdapter().delete(foo) > 0);

        int count = getAdapter().count(ExampleAutoincrement.class);
        assertTrue(count > 0);

        deleted = getAdapter().delete(ExampleAutoincrement.class, "name LIKE ?", new String[]{"Foo%"});
        assertTrue(deleted > 0);

        count = getAdapter().count(ExampleAutoincrement.class);
        assertEquals(0, count);
    }

    @Test
    public void testDeleteHasMany() {
        God jesus = new God();
        jesus.name = "Jesus";
        God thor = new God();
        thor.name = "Thor";

        PolyTheist dummy = new PolyTheist();
        dummy.gods = Arrays.asList(thor, jesus);

        getAdapter().store(dummy);

        assertEquals(1, getAdapter().findAll(PolyTheist.class).size());
        assertEquals(2, getAdapter().findAll(God.class).size());

        getAdapter().delete(PolyTheist.class, null, null);

        assertEquals(0, getAdapter().findAll(PolyTheist.class).size());
        assertEquals(2, getAdapter().findAll(God.class).size());
    }

    @Test
    public void testDeleteHasManyOnCascade() {
        God jesus = new God();
        jesus.name = "Jesus";
        God thor = new God();
        thor.name = "Thor";

        PolyTheist dummy = new PolyTheist();
        dummy.gods = Arrays.asList(thor, jesus);

        getAdapter().store(dummy);

        assertEquals(1, getAdapter().findAll(PolyTheist.class).size());
        assertEquals(2, getAdapter().findAll(God.class).size());

        getAdapter().delete(PolyTheist.class, null, null, true);

        assertEquals(0, getAdapter().findAll(PolyTheist.class).size());
        assertEquals(0, getAdapter().findAll(God.class).size());
    }

    @Test
    public void testDeleteManyToMany() {
        Book puta = new Book();
        puta.id = 1;
        puta.name = "La puta de Babilonia";

        Book vida = new Book();
        vida.id = 2;
        vida.name = "El Don de la Vida";

        Author author = new Author();
        author.name = "Fernando Vallejo";
        author.books = Arrays.asList(puta, vida);

        Author william = new Author();
        william.name = "William Ospina";
        william.books = Arrays.asList(puta);

        getAdapter().store(author);
        getAdapter().store(william);

        assertEquals(2, getAdapter().findAll(Author.class).size());
        assertEquals(2, getAdapter().findAll(Book.class).size());

        getAdapter().delete(Author.class, "name LIKE ?", new String[]{"Fernando Vallejo"});

        assertEquals(1, getAdapter().findAll(Author.class).size());
        assertEquals(2, getAdapter().findAll(Book.class).size());

        getAdapter().delete(Author.class, "name LIKE ?", new String[]{"William Ospina"});

        assertEquals(0, getAdapter().findAll(Author.class).size());
        assertEquals(2, getAdapter().findAll(Book.class).size());
    }

    @Test
    public void testDeleteManyToManyOnCascade() {
        Book puta = new Book();
        puta.id = 1;
        puta.name = "La puta de Babilonia";

        Book vida = new Book();
        vida.id = 2;
        vida.name = "El Don de la Vida";

        Author author = new Author();
        author.name = "Fernando Vallejo";
        author.books = Arrays.asList(puta, vida);

        Author william = new Author();
        william.name = "William Ospina";
        william.books = Arrays.asList(puta);

        getAdapter().store(author);
        getAdapter().store(william);

        assertEquals(2, getAdapter().findAll(Author.class).size());
        assertEquals(2, getAdapter().findAll(Book.class).size());

        getAdapter().delete(Author.class, "name LIKE ?", new String[]{"Fernando Vallejo"}, true);

        assertEquals(1, getAdapter().findAll(Author.class).size());
        assertEquals(1, getAdapter().findAll(Book.class).size());

        getAdapter().delete(Author.class, "name LIKE ?", new String[]{"William Ospina"}, true);

        assertEquals(0, getAdapter().findAll(Author.class).size());
        assertEquals(0, getAdapter().findAll(Book.class).size());
    }

    protected abstract SqlAdapter getAdapter();
}
