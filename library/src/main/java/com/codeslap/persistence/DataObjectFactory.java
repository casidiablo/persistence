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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Used to get {@link DataObject} instances. Will try to load generated ones if possible; otherwise
 * it will fallback to {@link ReflectDataObject} ones.
 *
 * @author cristian
 */
class DataObjectFactory {

  private static final ConcurrentMap<Class<?>, DataObject<?>> dataObjects;

  static {
    dataObjects = new ConcurrentHashMap<Class<?>, DataObject<?>>();
  }

  static <T> DataObject<T> getDataObject(Class<T> type) {
    DataObject<T> dataObject = (DataObject<T>) dataObjects.get(type);
    if (dataObject == null) {
      try {
        Class<?> rawClass = Class.forName(type.getName() + "DataObject");
        Class<? extends DataObject<T>> dataObjectClass = (Class<? extends DataObject<T>>) rawClass;
        if (dataObjectClass != null) {
          dataObject = dataObjectClass.getDeclaredConstructor().newInstance();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (dataObject == null) {
        dataObject = (DataObject<T>) new ReflectDataObject(type);
      }
      dataObjects.put(type, dataObject);
    }
    return dataObject;
  }
}
