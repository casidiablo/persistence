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
public class TestHasMany extends TestSqlite {
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

        PolyTheist found = getAdapter().findFirst(PolyTheist.class, "id = ?", new String[]{String.valueOf(store)});
        assertNotNull(found);
        assertEquals(polyTheist, found);
    }
    
}
