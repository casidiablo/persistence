package com.egoclean.persistence;

/**
 * @author cristian
 */
class DaoNotFoundException extends RuntimeException {
    public DaoNotFoundException(Class<?> clazz) {
        super("Could not find persistence for class " + clazz);
    }
}
