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

import android.app.Activity;
import com.codeslap.persistence.*;
import com.codeslap.robolectric.RobolectricSimpleRunner;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * @author cristian
 */
@RunWith(RobolectricSimpleRunner.class)
public abstract class SqliteTest {

    private SqlAdapter mAdapter;
    private SqlPersistence mDatabase;

    @Before
    public void configure() {
        PersistenceConfig.clear();
        mDatabase = PersistenceConfig.getDatabase("test.db", 1);
        mDatabase.match(ExampleAutoincrement.class, AnnotationAutoincrement.class, AnnotationNotAutoincrement.class);
        mDatabase.match(new HasMany(PolyTheist.class, God.class));
        mDatabase.match(new HasMany(Cow.class, Bug.class, true));
        mDatabase.match(new ManyToMany(Book.class, Author.class));
        mDatabase.matchNotAutoIncrement(ExampleNotAutoincrement.class);
        mDatabase.matchNotAutoIncrement(new ManyToMany(Cow.class, Bug.class));
        mDatabase.matchNotAutoIncrement(new HasMany(Cow.class, Bug.class, true));

        mAdapter = Persistence.getSqliteAdapter(new Activity());
        mAdapter.truncate(ExampleAutoincrement.class);
        mAdapter.truncate(ExampleNotAutoincrement.class);
        mAdapter.truncate(AnnotationAutoincrement.class);
        mAdapter.truncate(Book.class);
        mAdapter.truncate(God.class);
        mAdapter.truncate(PolyTheist.class);
        mAdapter.truncate(Author.class);
    }

    public SqlAdapter getNormalAdapter() {
        return mAdapter;
    }

    public SqlAdapter getQuickAdapter() {
        return Persistence.quick(new Activity());
    }

    public SqlPersistence getDatabase() {
        return mDatabase;
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

    public static class AnnotationAutoincrement {
        @PrimaryKey
        long something;
        @Column(value = "char_sequence", notNull = true)
        String name;
        @Column(value = "the_last_name", notNull = true, defaultValue = "Castiblanco")
        String lastName;
        @Column("signed")
        int number;
        @Column("value")
        float decimal;
        @Column("active")
        boolean bool;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AnnotationAutoincrement)) return false;

            AnnotationAutoincrement that = (AnnotationAutoincrement) o;

            if (bool != that.bool) return false;
            if (Float.compare(that.decimal, decimal) != 0) return false;
            if (number != that.number) return false;
            if (lastName != null ? !lastName.equals(that.lastName) : that.lastName != null) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
            result = 31 * result + number;
            result = 31 * result + (decimal != +0.0f ? Float.floatToIntBits(decimal) : 0);
            result = 31 * result + (bool ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "AnnotationAutoincrement{" +
                    "something=" + something +
                    ", name='" + name + '\'' +
                    ", lastName='" + lastName + '\'' +
                    ", number=" + number +
                    ", decimal=" + decimal +
                    ", bool=" + bool +
                    '}';
        }
    }
    public static class AnnotationNotAutoincrement {
        @PrimaryKey(autoincrement = false)
        long something;
        @Column(value = "char_sequence", notNull = true)
        String name;
        @Column(value = "the_last_name", notNull = true, defaultValue = "Castiblanco")
        String lastName;
        @Column("signed")
        int number;
        @Column("value")
        float decimal;
        @Column("active")
        boolean bool;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AnnotationAutoincrement)) return false;

            AnnotationNotAutoincrement that = (AnnotationNotAutoincrement) o;

            if (bool != that.bool) return false;
            if (Float.compare(that.decimal, decimal) != 0) return false;
            if (number != that.number) return false;
            if (lastName != null ? !lastName.equals(that.lastName) : that.lastName != null) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
            result = 31 * result + number;
            result = 31 * result + (decimal != +0.0f ? Float.floatToIntBits(decimal) : 0);
            result = 31 * result + (bool ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "AnnotationAutoincrement{" +
                    "something=" + something +
                    ", name='" + name + '\'' +
                    ", lastName='" + lastName + '\'' +
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

            ExampleNotAutoincrement that = (ExampleNotAutoincrement) o;

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

    public static class Book {
        long id;
        String name;
        List<Author> authors;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Book book = (Book) o;

            if (authors != null ? !authors.equals(book.authors) : book.authors != null) return false;
            if (name != null ? !name.equals(book.name) : book.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (authors != null ? authors.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            String authorString = "[";
            if (authors != null) {
                for (Author author : authors) {
                    authorString += "Author{" +
                            "id=" + author.id +
                            ", name='" + author.name + '\'' +
                            "},";
                }
            }
            authorString += "]";

            return "Book{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", authors=" + authorString +
                    '}';
        }
    }

    public static class Author {
        long id;
        String name;
        List<Book> books;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Author author = (Author) o;

            if (books != null ? !books.equals(author.books) : author.books != null) return false;
            if (name != null ? !name.equals(author.name) : author.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (books != null ? books.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            String bookString = "[";
            if (books != null) {
                for (Book author : books) {
                    bookString += "Author{" +
                            "id=" + author.id +
                            ", name='" + author.name + '\'' +
                            "},";
                }
            }
            bookString += "]";
            return "Author{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", books=" + bookString +
                    '}';
        }
    }

    public static class Cow {
        long id;
        String name;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Cow cow = (Cow) o;

            if (name != null ? !name.equals(cow.name) : cow.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "Cow{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    public static class Bug {
        long id;
        float itchFactor;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Bug bug = (Bug) o;

            if (Float.compare(bug.itchFactor, itchFactor) != 0) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return (itchFactor != +0.0f ? Float.floatToIntBits(itchFactor) : 0);
        }

        @Override
        public String toString() {
            return "Bug{" +
                    "id=" + id +
                    ", itchFactor=" + itchFactor +
                    '}';
        }
    }
}