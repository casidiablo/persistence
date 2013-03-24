package com.codeslap.persistence;

class StrUtil {
    public static String concat(Object... strings) {
        StringBuilder builder = new StringBuilder();
        for (Object string : strings) {
            builder.append(string);
        }
        return builder.toString();
    }
}
