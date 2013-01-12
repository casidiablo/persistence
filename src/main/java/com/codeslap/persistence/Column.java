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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Using this annotation, it is possible to customize the basic column data that will be associated
 * with an specific field
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
    String NULL = "This is a default value... you are not going to use this, do you?";

    /**
     * @return the column name
     */
    String value();

    /**
     * @return true if you do not want to allow this field to be null
     */
    boolean notNull() default false;

    /**
     * @return the value to set to this element when no argument is assigned. This works for TEXT columns only.
     */
    String defaultValue() default NULL;

    /**
     * @return true if the name of this column must be forced. Useful when handling primary keys whose name is not _id
     */
    boolean forceName() default false;
}
