package com.codeslap.persistence;

public interface PreferencesAdapter {
    String DEFAULT_PREFS = "default.prefs";

    public <T> void store(T bean);

    public <T> T retrieve(Class<T> clazz);

    public <T> boolean delete(Class<T> clazz);
}
