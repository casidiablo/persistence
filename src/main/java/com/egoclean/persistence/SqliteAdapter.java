package com.egoclean.persistence;

import java.util.List;

public interface SqliteAdapter {
    <T> long store(T object);
    
    <T, G> long store(T object, G attachedTo);

    <T> int update(T object, T where);

    void close();

    <T> T findFirst(Class<T> clazz, T where);

    <T> List<T> findAll(Class<T> clazz);

    <T> List<T> findAll(Class<T> clazz, T where);

    <T, G> List<T> findAll(Class<T> clazz, T where, G attachedTo);

    <T> List<T> findAll(Class<T> clazz, T where, Constraint constraint);

    <T> int delete(T where);
}