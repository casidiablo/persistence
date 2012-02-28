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
    @Test
    public void testHelpers() {
        SqlAdapter sqliteAdapter = Persistence.getSqliteAdapter(new Activity());
        assertNotNull(sqliteAdapter);
        SqlAdapter test = Persistence.getSqliteAdapter(new Activity(), "test.db");
        assertNotNull(test);
        SqlAdapter quick = Persistence.getQuickAdapter(new Activity(), "test.db");
        assertNotNull(quick);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithMultipleHasManyRelationships() {
        SqlPersistence database = PersistenceConfig.getDatabase("some.db", 1);
        database.match(new HasMany(Author.class, Book.class));
        database.match(new HasMany(Book.class, Author.class));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithMultipleManyToManyRelationships() {
        SqlPersistence database = PersistenceConfig.getDatabase("some.db", 1);
        database.match(new ManyToMany(Author.class, Book.class));
        database.match(new ManyToMany(Book.class, Author.class));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithRepeatedManyToManyRelationships() {
        SqlPersistence database = PersistenceConfig.getDatabase("some.db", 1);
        database.match(new ManyToMany(Author.class, Book.class));
        database.match(new ManyToMany(Author.class, Book.class));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithWhenHasManyRelationDoesNotExist() {
        SqlPersistence database = PersistenceConfig.getDatabase("some.db", 1);
        database.match(new HasMany(ExampleAutoincrement.class, ExampleNotAutoincrement.class));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithInvalidHasManyRelation() {
        assertNotNull(new HasMany(Book.class, Cow.class));
    }

    @Test
    public void testGetRelationship() {
        SqlPersistence.Relationship unknown = getDatabase().getRelationship(ExampleAutoincrement.class, ExampleNotAutoincrement.class);
        assertEquals(SqlPersistence.Relationship.UNKNOWN, unknown);
        SqlPersistence.Relationship hasMany = getDatabase().getRelationship(PolyTheist.class, God.class);
        assertEquals(SqlPersistence.Relationship.HAS_MANY, hasMany);
        SqlPersistence.Relationship manyToMany = getDatabase().getRelationship(Author.class, Book.class);
        assertEquals(SqlPersistence.Relationship.MANY_TO_MANY, manyToMany);
    }

    @Test
    public void testGetManyToMany() {
        List<ManyToMany> manyToMany = getDatabase().getManyToMany(Book.class);
        assertNotNull(manyToMany);
        assertEquals(1, manyToMany.size());
        assertEquals(Book.class, manyToMany.get(0).getFirstRelation());
        assertEquals(Author.class, manyToMany.get(0).getSecondRelation());
    }

    @Test
    public void testHas() {
        HasMany hasMany = getDatabase().has(PolyTheist.class);
        assertEquals(PolyTheist.class, hasMany.getContainerClass());
        assertEquals(God.class, hasMany.getContainedClass());

        assertNull(getDatabase().has(God.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailWithInvalidDatabaseName() {
        PersistenceConfig.getDatabase(null, 1);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWhenNoDatabaseIsAssociated() {
        PersistenceConfig.getDatabase("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldFailWhenClosingAQuickAdapter() {
        SqlAdapter quick = Persistence.getQuickAdapter(new Activity(), "test.db");
        assertNotNull(quick);
        quick.close();
    }
}
