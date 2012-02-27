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
}
