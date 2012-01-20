package com.codeslap.persistence;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ManyToManyTest {
    @Test
    public void testManyToMany() {
        SqlPersistence database = PersistenceConfig.getDatabase("test.db", 1);
        ManyToMany relation = new ManyToMany(Feed.class, "id", String.class, "id");
        String createTableStatement = relation.getCreateTableStatement();
        relation = new ManyToMany(Feed.class, String.class);
        String createTableStatement2 = relation.getCreateTableStatement();
        assertEquals(createTableStatement, createTableStatement2);
        database.match(relation);
    }

    @Test
    public void testManyToMany2() {
        SqlPersistence database = PersistenceConfig.getDatabase("test.db", 1);
        database.match(new ManyToMany(Feed.class, County.class));
        // create all tables for registered daos
        List<Class<?>> objects = database.getSqliteClasses();
        for (Class clazz : objects) {
            System.out.println(SQLHelper.getCreateTableSentence("test.db", clazz));
        }
        List<ManyToMany> sqliteManyToMany = database.getSqliteManyToMany();
        for (ManyToMany manyToMany : sqliteManyToMany) {
            System.out.println(manyToMany.getCreateTableStatement().replaceAll(";", ";\n"));
        }
    }
}