package com.codeslap.persistence;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ManyToManyTest {
    @Test
    public void testManyToMany() {
        Persistence.matchPreference(String.class, Long.class);
        ManyToMany relation = new ManyToMany(Feed.class, "id", String.class, "id");
        String createTableStatement = relation.getCreateTableStatement();
        relation = new ManyToMany(Long.class, String.class);
        String createTableStatement2 = relation.getCreateTableStatement();
        assertEquals(createTableStatement, createTableStatement2);
        Persistence.match(relation);
    }

    @Test
    public void testManyToMany2() {
        Persistence.match(new ManyToMany(Feed.class, County.class));
        // create all tables for registered daos
        List<Class<?>> objects = Persistence.getSqliteClasses();
        for (Class clazz : objects) {
            System.out.println(SQLHelper.getCreateTableSentence(clazz));
        }
        List<ManyToMany> sqliteManyToMany = Persistence.getSqliteManyToMany();
        for (ManyToMany manyToMany : sqliteManyToMany) {
            System.out.println(manyToMany.getCreateTableStatement().replaceAll(";", ";\n"));
        }
    }
}
