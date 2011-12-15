package com.egoclean.persistence;

import org.junit.Test;

public class TestClass {

    @Test
    public void testMatch() {
        Persistence.matchSqlite(JuaneloMendieta.class);
        Persistence.matchPreference(Object.class);
    }

    @Test
    public void testTableGenerator() {
        String sql = SQLHelper.getCreateTableSentence(JuaneloMendieta.class);
        System.out.println(sql);
    }
}
