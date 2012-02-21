package com.codeslap.persistence;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author cristian
 */
public class TestManyToMany extends TestSqlite {
    @Test
    public void testManyToMany() {
        // let's create some dummy data
        Author fernando = new Author();
        fernando.name = "Vallejo";
        
        Author william = new Author();
        william.name = "Ospinas";

        Book tautologia = new Book();
        tautologia.name = "Tautología Darwinista";

        Book imposturologia = new Book();
        imposturologia.name = "Manualito de Imposturología Física";

        Book puta = new Book();
        puta.name = "La puta de Babilonia";

        Book foo = new Book();
        foo.name = "Bar";

        // let's create some relations
        fernando.books = Arrays.asList(tautologia, imposturologia, puta);
        william.books = Arrays.asList(tautologia, foo);

        getAdapter().storeCollection(Arrays.asList(william, fernando), null);

        Author vallejo = getAdapter().findFirst(Author.class, "name LIKE 'Vallejo'", null);
        Author ospina = getAdapter().findFirst(Author.class, "name LIKE 'Ospinas'", null);

        assertEquals(fernando, vallejo);
        assertEquals(william, ospina);
    }
}
