package com.egoclean.persistence;

import org.junit.Test;

import static org.junit.Assert.assertNull;

public class TestTypes {
    @Test
    public void testTypes() {
        Types types = new Types();
        System.out.println(types);
    }

    @Test
    public void testWhere() {
        assertNull(SQLHelper.getWhere(null));
        Types types = new Types();
        System.out.println(SQLHelper.getWhere(types));
        types.setInteger(1);
        System.out.println(SQLHelper.getWhere(types));
        types.setLonged(2);
        System.out.println(SQLHelper.getWhere(types));
        types.setFloated(2.1f);
        System.out.println(SQLHelper.getWhere(types));
        types.setDoubled(2.3);
        System.out.println(SQLHelper.getWhere(types));
        types.setString("foo");
        System.out.println(SQLHelper.getWhere(types));
    }

    private static class Types{
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
