package com.egoclean.persistence;

public interface PreferencesAdapter {
    public <T> void store(T bean);

    public <T> T retrieve(Class<T> clazz);

    public <T> void delete(Class<T> clazz);
}
