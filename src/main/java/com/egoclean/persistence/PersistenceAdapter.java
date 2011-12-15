package com.egoclean.persistence;

import java.util.List;

public interface PersistenceAdapter {
    <T> void store(Class<? extends T> clazz, T object, Predicate predicate);

    void close();

    <T> T findFirst(Class<T> clazz, Predicate where);

    <T> List<T> findAll(Class<T> clazz);

    <T> List<T> findAll(Class<T> clazz, Predicate where);

    <T> void delete(Class<T> clazz, Predicate where);
}