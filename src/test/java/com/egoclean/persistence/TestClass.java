package com.egoclean.persistence;

import org.junit.Test;

public class TestClass {

    @Test
    public void testMatch() {
        DaoFactory.matchSqlite(JuaneloMendieta.class);
        DaoFactory.matchPreference(Object.class);
    }

    @Test
    public void testTableGenerator() {
        String sql = SQLHelper.getCreateTableSentence(JuaneloMendieta.class);
        System.out.println(sql);
    }
}
