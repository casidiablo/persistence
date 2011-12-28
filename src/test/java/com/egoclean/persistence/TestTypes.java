package com.egoclean.persistence;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertNull;

public class TestTypes {
    @Test
    public void testTypes() {
        Types types = new Types();
        System.out.println(types);
    }

    @Test
    public void testWhere() {
        Persistence.match(new HasMany(Types.class, Foo.class, true));
        assertNull(SQLHelper.getWhere(null, new ArrayList<String>()));
        Types types = new Types();
        System.out.println(SQLHelper.getWhere(types, new ArrayList<String>()));
        types.setInteger(1);
        System.out.println(SQLHelper.getWhere(types, new ArrayList<String>()));
        types.setLonged(2);
        System.out.println(SQLHelper.getWhere(types, new ArrayList<String>()));
        types.setFloated(2.1f);
        System.out.println(SQLHelper.getWhere(types, new ArrayList<String>()));
        types.setDoubled(2.3);
        System.out.println(SQLHelper.getWhere(types, new ArrayList<String>()));
        types.setString("foo");
        System.out.println(SQLHelper.getWhere(types, new ArrayList<String>()));
        Foo foo = new Foo();
        foo.setBar("baz");
        types.id = 44;
        ArrayList<String> args = new ArrayList<String>();
        System.out.println(SQLHelper.getWhere(Foo.class, foo, args, types));
        System.out.println(args);
    }
    
    private static class Foo{
        private String bar;

        public String getBar() {
            return bar;
        }

        public void setBar(String bar) {
            this.bar = bar;
        }

        @Override
        public String toString() {
            return "Foo{" +
                    "bar='" + bar + '\'' +
                    '}';
        }
    }

    private static class Types{
        private long id;
        private int integer;
        private long longed;
        private float floated;
        private double doubled;
        private String string;
        private boolean bool;

        public int getInteger() {
            return integer;
        }

        public void setInteger(int integer) {
            this.integer = integer;
        }

        public long getLonged() {
            return longed;
        }

        public void setLonged(long longed) {
            this.longed = longed;
        }

        public float getFloated() {
            return floated;
        }

        public void setFloated(float floated) {
            this.floated = floated;
        }

        public double getDoubled() {
            return doubled;
        }

        public void setDoubled(double doubled) {
            this.doubled = doubled;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public boolean isBool() {
            return bool;
        }

        public void setBool(boolean bool) {
            this.bool = bool;
        }

        @Override
        public String toString() {
            return "Types{" +
                    "integer=" + integer +
                    ", longed=" + longed +
                    ", floated=" + floated +
                    ", doubled=" + doubled +
                    ", string='" + string + '\'' +
                    ", bool=" + bool +
                    '}';
        }
    }
}
