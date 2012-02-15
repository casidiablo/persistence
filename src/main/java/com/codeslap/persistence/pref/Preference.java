package com.codeslap.persistence.pref;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation used by the preference adapters to get the preference's metadata
 *
 * @author cristian
 */
@Retention(RetentionPolicy.RUNTIME)
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