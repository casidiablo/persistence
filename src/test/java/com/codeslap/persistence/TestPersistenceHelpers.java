package com.codeslap.persistence;

import android.app.Activity;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author cristian
 */
public class TestPersistenceHelpers extends TestSqlite {
    @Test
    public void testHelpers() {
        SqlAdapter sqliteAdapter = Persistence.getSqliteAdapter(new Activity());
        assertNotNull(sqliteAdapter);
        SqlAdapter test = Persistence.getSqliteAdapter(new Activity(), "test.db");
        assertNotNull(test);
        SqlAdapter quick = Persistence.getQuickAdapter(new Activity(), "test.db");
        assertNotNull(quick);

        assertEquals(test, sqliteAdapter);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithMultipleHasManyRelationships() {
        SqlPersistence database = PersistenceConfig.getDatabase("some.db", 1);
        database.match(new HasMany(Author.class, Book.class));
        database.match(new HasMany(Book.class, Author.class));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithWhenHasManyRelationDoesNotExist() {
        SqlPersistence database = PersistenceConfig.getDatabase("some.db", 1);
        database.match(new HasMany(ExampleAutoincrement.class, ExampleNotAutoincrement.class));
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
        assertEquals(Book.class, manyToMany.get(0).getClasses()[0]);
        assertEquals(Author.class, manyToMany.get(0).getClasses()[1]);
    }

    @Test
    public void testHas() {
        HasMany hasMany = getDatabase().has(PolyTheist.class);
        assertEquals(PolyTheist.class, hasMany.getClasses()[0]);
        assertEquals(God.class, hasMany.getClasses()[1]);
        
        assertNull(getDatabase().has(God.class));
    }
}
