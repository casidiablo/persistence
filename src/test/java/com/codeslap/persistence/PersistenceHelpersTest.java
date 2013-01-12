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

package com.codeslap.persistence;

import android.app.Activity;
import com.codeslap.test.persistence.SqliteTest;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author cristian
 */
public class PersistenceHelpersTest extends SqliteTest {

    private static final String FAILING_SPEC_ID = "some.db";

    @Test
    public void testHelpers() {
        SqlAdapter sqliteAdapter = Persistence.getAdapter(new Activity());
        assertNotNull(sqliteAdapter);
        SqlAdapter test = Persistence.getAdapter(new Activity(), Persistence.DEFAULT_DATABASE_NAME);
        assertNotNull(test);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithMultipleHasManyRelationships() {
        DatabaseSpec dbSpec = PersistenceConfig.registerSpec(FAILING_SPEC_ID, 1);
        dbSpec.match(new HasMany(Author.class, Book.class));
        dbSpec.match(new HasMany(Book.class, Author.class));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithMultipleManyToManyRelationships() {
        DatabaseSpec dbSpec = PersistenceConfig.registerSpec(FAILING_SPEC_ID, 1);
        dbSpec.match(new ManyToMany(Author.class, Book.class));
        dbSpec.match(new ManyToMany(Book.class, Author.class));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithRepeatedManyToManyRelationships() {
        DatabaseSpec dbSpec = PersistenceConfig.registerSpec(FAILING_SPEC_ID, 1);
        dbSpec.match(new ManyToMany(Author.class, Book.class));
        dbSpec.match(new ManyToMany(Author.class, Book.class));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithWhenHasManyRelationDoesNotExist() {
        DatabaseSpec dbSpec = PersistenceConfig.registerSpec(FAILING_SPEC_ID, 1);
        dbSpec.match(new HasMany(ExampleAutoincrement.class, ExampleNotAutoincrement.class));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithInvalidHasManyRelation() {
        assertNotNull(new HasMany(Book.class, Cow.class));
    }

    @Test
    public void testGetRelationship() {
        DatabaseSpec.Relationship unknown = getDatabase().getRelationship(ExampleAutoincrement.class, ExampleNotAutoincrement.class);
        assertEquals(DatabaseSpec.Relationship.UNKNOWN, unknown);
        DatabaseSpec.Relationship hasMany = getDatabase().getRelationship(PolyTheist.class, God.class);
        assertEquals(DatabaseSpec.Relationship.HAS_MANY, hasMany);
        DatabaseSpec.Relationship manyToMany = getDatabase().getRelationship(Author.class, Book.class);
        assertEquals(DatabaseSpec.Relationship.MANY_TO_MANY, manyToMany);
    }

    @Test
    public void testGetManyToMany() {
        List<ManyToMany> manyToMany = getDatabase().getManyToMany(Book.class);
        assertNotNull(manyToMany);
        assertEquals(1, manyToMany.size());
        assertEquals(Author.class, manyToMany.get(0).getFirstRelation());
        assertEquals(Book.class, manyToMany.get(0).getSecondRelation());
    }

    @Test
    public void testHas() {
        HasMany hasMany = getDatabase().has(PolyTheist.class);
        assertEquals(PolyTheist.class, hasMany.getContainerClass());
        assertEquals(God.class, hasMany.getContainedClass());

        assertNull(getDatabase().has(God.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailWithInvalidSpecName() {
        PersistenceConfig.registerSpec(null, 1);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWhenNoSpecIsAssociated() {
        PersistenceConfig.getDatabaseSpec("foo");
    }
}
