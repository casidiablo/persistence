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

import com.codeslap.persistence.Constraint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author cristian
 */
public class ConstraintsTest extends SqliteTest {
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
            foo.blob = foo.name.getBytes();
            list.add(foo);
        }
        getAdapter().storeCollection(list, null);

        ExampleAutoincrement sample = new ExampleAutoincrement();
        sample.bool = true;

        List<ExampleAutoincrement> all = getAdapter().findAll(sample);
        assertEquals(list.size() / 2, all.size());

        Constraint constraint = new Constraint().limit(2).groupBy("name").orderBy("number DESC");
        List<ExampleAutoincrement> found = getAdapter().findAll(sample, constraint);
        assertEquals(2, found.size());

        assertTrue(found.get(0).number >= found.get(1).number);
    }
}
