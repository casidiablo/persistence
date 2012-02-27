/*
 * Copyright 2012 CodeSlap
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

package com.codeslap.persistence.pref;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by the preference adapters to get the preference's metadata
 *
 * @author cristian
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Preference {
    /**
     * @return the key to use in this preference
     */
    String key();

    /**
     * @return default value to use
     */
    String defaultValue() default "";

    /**
     * @return Android string resource to use when the summary is needed
     */
    int summary() default 0;

    /**
     * @return Android string resource to use when the tittle is needed
     */
    int title() default 0;

    /**
     * @return Android string resource to wrap this preference
     */
    int category() default -1;

    /**
     * @return order in the preference screen
     */
    int order() default -1;

    /**
     * @return order in the set of categories
     */
    int categoryOrder() default -1;

    /**
     * @return true if this key must be ignored when building a preference screen
     */
    boolean ignore() default false;
}