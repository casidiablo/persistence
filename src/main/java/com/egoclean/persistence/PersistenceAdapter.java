package com.egoclean.persistence;

import java.util.List;

public interface PersistenceAdapter {
    <T> void store(Class<T> clazz, T object);

    <T> int update(Class<T> clazz, T object, T where);

    void close();

    <T> T findFirst(Class<T> clazz, T where);

    <T> List<T> findAll(Class<T> clazz);

    <T> List<T> findAll(Class<T> clazz, T where);

    <T> void delete(Class<T> clazz, T where);
}