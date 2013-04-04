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
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author cristian
 */
public class ManyToManyTest extends SqliteTest {
  @Test
  public void testManyToMany() {
    // let's create some dummy data
    Author fernando = new Author();
    fernando.name = "Vallejo";

    Author william = new Author();
    william.name = "Ospina";

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

    mAdapter.storeCollection(Arrays.asList(william, fernando), null);

    Author vallejo = mAdapter.findFirst(Author.class, "name LIKE 'Vallejo'", null);
    Author ospina = mAdapter.findFirst(Author.class, "name LIKE 'Ospina'", null);

    assertEquals(fernando, vallejo);
    assertEquals(william, ospina);

    mAdapter.delete(tautologia);
  }

  @Test
  public void testManyToManyWithAnnotations() {
    // let's create some dummy data
    Owner fernando = new Owner();
    fernando.name = "Fernando Vallejo";

    Owner william = new Owner();
    william.name = "William Ospina";

    Pet witch = new Pet();
    witch.nick = "Bruja";

    Pet tobby = new Pet();
    tobby.nick = "Tobby";

    Pet lazzy = new Pet();
    lazzy.nick = "Lazzy";

    Pet foo = new Pet();
    foo.nick = "Bar";

    // let's create some relations
    fernando.pets = Arrays.asList(witch, tobby);
    william.pets = Arrays.asList(witch, foo, lazzy);

    mAdapter.storeCollection(Arrays.asList(william, fernando), null);

    Owner vallejo = mAdapter.findFirst(Owner.class, "full_name LIKE ?", new String[]{"Fernando%"});
    Owner ospina = mAdapter.findFirst(Owner.class, "full_name LIKE '%Ospina%'", null);

    assertEquals(fernando, vallejo);
    assertEquals(william, ospina);

    mAdapter.delete(witch);
    mAdapter.delete(fernando);
    mAdapter.delete(ospina);
    mAdapter.delete(tobby);
  }
}
