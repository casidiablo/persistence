package com.codeslap.hongo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicitly marks this class as belonging to another one. See {@link HasMany}
 * <p/>
 * Note: To use in a class definition.
 */
@Retention(RetentionPolicy.RUNTIME) @Target(value = ElementType.TYPE)
public @interface Belongs {
  /** The class this interface belongs to. */
  Class<?> to();
}
