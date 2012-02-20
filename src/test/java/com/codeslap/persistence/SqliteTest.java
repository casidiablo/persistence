package com.codeslap.persistence;

import android.app.Activity;
import org.junit.Before;

import java.util.List;

/**
 * @author cristian
 */
public class SqliteTest {

    private SqlAdapter mAdapter;

    @Before
    public void configure() {
        SqlPersistence database = PersistenceConfig.getDatabase("test.db", 1);
        database.match(ExampleAutoincrement.class);
        database.match(new HasMany(PolyTheist.class, God.class));
        database.matchNotAutoIncrement(ExampleNotAutoincrement.class);

        mAdapter = Persistence.getSqliteAdapter(new Activity());
    }

    SqlAdapter getAdapter() {
        return mAdapter;
    }

    public static class ExampleAutoincrement {
        long id;
        String name;
        int number;
        float decimal;
        boolean bool;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ExampleAutoincrement that = (ExampleAutoincrement) o;

            if (bool != that.bool) return false;
            if (Float.compare(that.decimal, decimal) != 0) return false;
            if (id != that.id) return false;
            if (number != that.number) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + number;
            result = 31 * result + (decimal != +0.0f ? Float.floatToIntBits(decimal) : 0);
            result = 31 * result + (bool ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ExampleAutoincrement{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", number=" + number +
                    ", decimal=" + decimal +
                    ", bool=" + bool +
                    '}';
        }
    }

    public static class ExampleNotAutoincrement {
        long id;
        String name;
        int number;
        float decimal;
        boolean bool;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ExampleAutoincrement that = (ExampleAutoincrement) o;

            if (bool != that.bool) return false;
            if (Float.compare(that.decimal, decimal) != 0) return false;
            if (id != that.id) return false;
            if (number != that.number) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + number;
            result = 31 * result + (decimal != +0.0f ? Float.floatToIntBits(decimal) : 0);
            result = 31 * result + (bool ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ExampleAutoincrement{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", number=" + number +
                    ", decimal=" + decimal +
                    ", bool=" + bool +
                    '}';
        }
    }

    public static class PolyTheist {
        long id;
        List<God> gods;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PolyTheist that = (PolyTheist) o;

            if (id != that.id) return false;
            if (gods != null ? !gods.equals(that.gods) : that.gods != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (gods != null ? gods.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "PolyTheist{" +
                    "id=" + id +
                    ", gods=" + gods +
                    '}';
        }
    }

    public static class God {
        long id;
        String name;
        double power;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            God god = (God) o;

            if (id != god.id) return false;
            if (Double.compare(god.power, power) != 0) return false;
            if (name != null ? !name.equals(god.name) : god.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = (int) (id ^ (id >>> 32));
            result = 31 * result + (name != null ? name.hashCode() : 0);
            temp = power != +0.0d ? Double.doubleToLongBits(power) : 0L;
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "God{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", power=" + power +
                    '}';
        }
    }
}
