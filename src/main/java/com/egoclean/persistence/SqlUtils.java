package com.egoclean.persistence;

class SqlUtils {
    /**
     * @param name string to normalize
     * @return converts a camelcase string into a lowercase, _ separated string
     */
    static String normalize(String name) {
        StringBuilder newName = new StringBuilder();
        newName.append(name.charAt(0));
        for (int i = 1; i < name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i))) {
                newName.append("_");
            }
            newName.append(name.charAt(i));
        }
        return newName.toString().toLowerCase();
    }
}
