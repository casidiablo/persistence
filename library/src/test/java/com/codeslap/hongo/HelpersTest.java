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

package com.codeslap.hongo;

import android.app.Activity;
import com.codeslap.test.hongo.SqliteTest;
import java.util.Collection;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HelpersTest extends SqliteTest {

  @Test
  public void testHelpers() {
    SqlAdapter sqliteAdapter = Hongo.getAdapter(new Activity());
    assertNotNull(sqliteAdapter);
    SqlAdapter test = Hongo.getAdapter(new Activity(), Hongo.DEFAULT_DATABASE_NAME);
    assertNotNull(test);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFailWithMultipleHasManyRelationships() {
    DataObjectFactory.getDataObject(Cyclic1.class).hasMany();
  }

  @Belongs(to = Cyclic2.class)
  public static class Cyclic1 {
    @PrimaryKey long id;
    @HasMany List<Cyclic2> list;
  }

  @Belongs(to = Cyclic1.class)
  public static class Cyclic2 {
    @PrimaryKey long id;
    @HasMany List<Cyclic1> list;
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFailWithWhenHasManyRelationDoesNotExist() {
    DataObjectFactory.getDataObject(Mother.class).hasMany();
  }

  public static class Mother {
    @PrimaryKey long id;
    @HasMany List<Daughter> children;
  }

  @Belongs(to = Parent.class)
  public static class Daughter {
    @PrimaryKey long id;
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFailWithInvalidHasManyRelation() {
    DataObjectFactory.getDataObject(Parent.class).hasMany();
  }

  public static class Parent {
    @PrimaryKey long id;
    @HasMany List<Child> children;
  }

  public static class Child {
    @PrimaryKey long id;
  }

  @Test
  public void testGetManyToMany() {
    DataObject<Book> dataObject = DataObjectFactory.getDataObject(Book.class);
    Collection<ManyToManySpec> manyToManySpecs = dataObject.manyToMany();
    assertNotNull(manyToManySpecs);
    assertEquals(1, manyToManySpecs.size());
    ManyToManySpec[] manyToManySpecsArr = manyToManySpecs.toArray(new ManyToManySpec[0]);
    ObjectType<Book> bookObjectType = dataObject.getObjectType();
    ObjectType<Author> authorObjectType =
        DataObjectFactory.getDataObject(Author.class).getObjectType();
    boolean either = manyToManySpecsArr[0].getFirstRelation().equals(authorObjectType)
        && manyToManySpecsArr[0].getSecondRelation().equals(bookObjectType);
    boolean or = manyToManySpecsArr[0].getFirstRelation().equals(bookObjectType)
        && manyToManySpecsArr[0].getSecondRelation().equals(authorObjectType);
    assertTrue(either || or);
  }

  @Test
  public void testHas() {
    DataObject<PolyTheist> dataObject = DataObjectFactory.getDataObject(PolyTheist.class);
    ObjectType<PolyTheist> polyTheistObjectType = dataObject.getObjectType();
    DataObject<God> godDataObject = DataObjectFactory.getDataObject(God.class);
    ObjectType<God> godObjectType = godDataObject.getObjectType();
    assertNotNull(dataObject);
    assertNotNull(dataObject.hasMany());
    assertFalse(dataObject.hasMany().isEmpty());
    assertNotNull(dataObject.hasMany(godObjectType));
    assertEquals(dataObject.hasMany(godObjectType).container, polyTheistObjectType);
    assertEquals(dataObject.hasMany(godObjectType).contained, godObjectType);
    assertNotNull(dataObject.hasMany(godObjectType).listField);
    assertNotNull(dataObject.hasMany(godObjectType).getThroughColumnName());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailWithInvalidSpecName() {
    HongoConfig.registerSpec(null, 1);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFailWhenNoSpecIsAssociated() {
    HongoConfig.getDatabaseSpec("foo");
  }
}
