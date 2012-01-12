package com.codeslap.persistence;

import java.util.List;

public interface SqliteAdapter {
    <T> Object store(T object);
    
    <T, G> Object store(T object, G attachedTo);

    <T> int update(T object, T where);

    void close();

    <T> T findFirst(T where);

    <T> List<T> findAll(Class<T> clazz);

    <T> List<T> findAll(T where);

    <T, G> List<T> findAll(T where, G attachedTo);

    <T> List<T> findAll(Class<T> clazz, T where, Constraint constraint);

    <T> int delete(T where);

    <T> int count(T bean);
}