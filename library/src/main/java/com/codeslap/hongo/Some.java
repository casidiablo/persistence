/*
 * Copyright 2013 CodeSlap
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

package com.codeslap.hongo;

import java.util.List;

/**
 * // TODO write description
 *
 * @author cristian
 */
public class Some {

    public void test(Nono hongo) {
        Customer customer = new Customer();
        hongo.save(customer);

        HongoQuery<Customer> hongoQuery = hongo.query(Customer.class)
                .where("id").equalsTo("hola");

        Customer first = hongo.findFirst(hongoQuery);
        System.out.println(first);

        HongoCursor<Customer> cursor = hongo.findAll(hongoQuery);
        for (Customer c : cursor) {
            System.out.println(c);
        }
        List<Customer> customers = cursor.asList();
    }

    public static interface Nono {
        <T> void save(T customer);

        <T> HongoQuery<T> query(Class<T> type);

        <T> HongoCursor<T> findAll(HongoQuery<T> hongoQuery);

        <T> T findFirst(HongoQuery<T> hongoQuery);
    }

    public static class Customer {
        String id;
        String name;
    }

    public static interface HongoQuery<T> {
        HongoQuery<T> equalsTo(Object object);

        HongoQuery<T> lessThan(Object object);

        HongoQuery<T> greaterThan(Object object);

        HongoQuery<T> or(HongoQuery<T> query);

        HongoQuery<T> and(HongoQuery<T> query);

        HongoQuery<T> where(String where);
    }

    public static void main(String... args) {
    }

    private static interface HongoCursor<T> extends Iterable<T> {
        List<T> asList();
    }
}
