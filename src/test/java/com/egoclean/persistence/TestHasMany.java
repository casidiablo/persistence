package com.egoclean.persistence;

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
        Persistence.match(new HasMany(Feed.class, County.class));
        Persistence.match(new HasMany(County.class, Feed.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testRecursiveHasMany() {
        Persistence.match(new HasMany(Recursive.class, Recursive.class));
    }

    @Test
    public void testTableCreation() {
        Persistence.match(new HasMany(Feed.class, County.class));
        assertEquals("CREATE TABLE IF NOT EXISTS feed (id INTEGER PRIMARY KEY, feed TEXT NOT NULL);",
                SQLHelper.getCreateTableSentence(Feed.class));
        assertEquals("CREATE TABLE IF NOT EXISTS county (id INTEGER PRIMARY KEY, county TEXT NOT NULL, feed_id INTEGER NOT NULL);",
                SQLHelper.getCreateTableSentence(County.class));
    }

    private static class Recursive {
        private List<Recursive> recursiveList;
    }
}
