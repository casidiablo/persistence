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
    // let's create some gods. We men experts at this
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
    Object store = mAdapter.store(polyTheist);
    assertTrue(store instanceof Long);

    PolyTheist found = mAdapter.findFirst(PolyTheist.class, "_id = ?", new String[]{String.valueOf(store)});
    assertEquals(polyTheist, found);
  }
}
