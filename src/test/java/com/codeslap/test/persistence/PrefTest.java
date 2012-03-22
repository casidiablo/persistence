/*
 * Copyright 2012 CodeSlap
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeslap.test.persistence;

import com.codeslap.persistence.PersistenceConfig;
import com.codeslap.persistence.PrefsPersistence;
import com.codeslap.persistence.pref.Preference;
import com.codeslap.robolectric.RobolectricSimpleRunner;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * @author cristian
 */
@RunWith(RobolectricSimpleRunner.class)
public abstract class PrefTest {
    @Before
    public void configure() {
        PrefsPersistence preference = PersistenceConfig.getPreference();
        preference.match(PreferenceBean.class, PrefImpossibleBean.class, PrefWithAnnotations.class);
        preference = PersistenceConfig.getPreference("test.pref");
        preference.match(PreferenceBean.class, PrefImpossibleBean.class, PrefWithAnnotations.class);
    }

    public static class PrefImpossibleBean {
        private final String mData;

        public PrefImpossibleBean(String data) {
            mData = data;
        }
    }

    public static class PrefWithAnnotations {
        @Preference(value = "the_long", defaultValue = Long.MIN_VALUE + "")
        long veryLong;
        @Preference(value = "the_string", defaultValue = "foo bar")
        String name;
        @Preference(value = "the_integer", defaultValue = Integer.MIN_VALUE + "")
        int number;
        @Preference(value = "the_decimal", defaultValue = Float.MIN_VALUE + "")
        double decimal;
        @Preference(value = "the_boolean", defaultValue = "true")
        boolean bool;
        @Preference("really_short")
        private short something;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PrefWithAnnotations that = (PrefWithAnnotations) o;

            if (bool != that.bool) return false;
            if (Double.compare(that.decimal, decimal) != 0) return false;
            if (number != that.number) return false;
            if (something != that.something) return false;
            if (veryLong != that.veryLong) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = (int) (veryLong ^ (veryLong >>> 32));
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + number;
            temp = decimal != +0.0d ? Double.doubleToLongBits(decimal) : 0L;
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + (bool ? 1 : 0);
            result = 31 * result + (int) something;
            return result;
        }

        @Override
        public String toString() {
            return "ExampleAutoincrement{" +
                    "id=" + veryLong +
                    ", name='" + name + '\'' +
                    ", number=" + number +
                    ", decimal=" + decimal +
                    ", bool=" + bool +
                    '}';
        }
    }

    public static class PreferenceBean {
        long veryLong;
        String name;
        int number;
        float decimal;
        boolean bool;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PreferenceBean that = (PreferenceBean) o;

            if (bool != that.bool) return false;
            if (Float.compare(that.decimal, decimal) != 0) return false;
            if (number != that.number) return false;
            if (veryLong != that.veryLong) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (veryLong ^ (veryLong >>> 32));
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + number;
            result = 31 * result + (decimal != +0.0f ? Float.floatToIntBits(decimal) : 0);
            result = 31 * result + (bool ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ExampleAutoincrement{" +
                    "id=" + veryLong +
                    ", name='" + name + '\'' +
                    ", number=" + number +
                    ", decimal=" + decimal +
                    ", bool=" + bool +
                    '}';
        }
    }
}
