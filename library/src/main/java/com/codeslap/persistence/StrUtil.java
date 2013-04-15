package com.codeslap.persistence;

import java.util.List;

class StrUtil {
  public static String concat(Object... strings) {
    StringBuilder builder = new StringBuilder();
    for (Object string : strings) {
      builder.append(string);
    }
    return builder.toString();
  }

  static String join(List<String> sets, String glue) {
    StringBuilder builder = new StringBuilder();
    boolean glued = false;
    for (String condition : sets) {
      if (glued) {
        builder.append(glue);
      }
      builder.append(condition);
      glued = true;
    }
    return builder.toString();
  }
}