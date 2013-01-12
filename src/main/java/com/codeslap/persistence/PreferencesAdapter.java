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

package com.codeslap.persistence;

/**
 * Persistence adapter that retrieves and saves one bean from and to a shared preferences file.
 * Keep in mind that this won't save collections of beans; if you want to persist more than
 * one bean use the {@link SqlAdapter}
 *
 * @author cristian
 * @version 1.0
 */
public interface PreferencesAdapter {
    String DEFAULT_PREFS = "default.prefs";

    /**
     * Persist a bean to the shared preferences
     *
     * @param bean the bean to persist
     * @param <T>  this can be any kind of bean with primitive data
     */
    public <T> void store(T bean);

    /**
     * Retrieves an object from the database
     *
     * @param theClass the class of the bean to retrieve
     * @param <T>      this can be any kind of bean with primitive data
     * @return the persisted object
     */
    public <T> T retrieve(Class<T> theClass);

    /**
     * Removes an object from the shared preferences
     *
     * @param theClass the class of the bean to delete
     * @param <T>      this can be any kind of bean with primitive data
     * @return true if everything went fine
     */
    public <T> boolean delete(Class<T> theClass);
}
