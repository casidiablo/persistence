package com.codeslap.persistence;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestHasMany {

    @Test
    public void testHasMany() {
        new HasMany(Feed.class, County.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testHasNotMany() {
        new HasMany(Feed.class, String.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInvertedHasManyRelation() {
        SqlPersistence database = Persistence.getDatabase("test.db", 1);
        database.match(new HasMany(Feed.class, County.class));
        database.match(new HasMany(County.class, Feed.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testRecursiveHasMany() {
        SqlPersistence database = Persistence.getDatabase("test.db", 1);
        database.match(new HasMany(Recursive.class, Recursive.class));
    }

    @Test
    public void testTableCreation() {
        SqlPersistence database = Persistence.getDatabase("test.db", 1);
        database.match(new HasMany(Feed.class, County.class));
        assertEquals("CREATE TABLE IF NOT EXISTS feed (id INTEGER PRIMARY KEY, feed TEXT NOT NULL);",
                SQLHelper.getCreateTableSentence("test.db", Feed.class));
        assertEquals("CREATE TABLE IF NOT EXISTS county (id INTEGER PRIMARY KEY, county TEXT NOT NULL, feed_id INTEGER NOT NULL);",
                SQLHelper.getCreateTableSentence("test.db", County.class));
    }

    private static class Recursive {
        private List<Recursive> recursiveList;
    }
}
