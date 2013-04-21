package com.codeslap.hongo;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper to build a create table SQL statement
 *
 * @author cristian
 */
class CreateTableHelper {

  private static final String NOT_NULL = " NOT NULL";
  private static final String AUTOINCREMENT = "AUTOINCREMENT";

  private final StringBuilder builder = new StringBuilder();
  private final List<String> columnSentences = new ArrayList<String>();
  private final List<String> columns = new ArrayList<String>();

  private boolean alreadyBuilt = false;
  private String pkSentence;

  private CreateTableHelper(String name) {
    builder.append("CREATE TABLE IF NOT EXISTS ").append(name).append(" (");
  }

  public static CreateTableHelper init(String name) {
    return new CreateTableHelper(name);
  }

  public CreateTableHelper add(String columnName, SqliteType type, boolean notNull) {
    String notNullSentence = notNull ? NOT_NULL : "";
    if (columns.contains(columnName)) {
      throw new IllegalStateException(columnName + " already added");
    }
    columns.add(columnName);
    columnSentences.add(columnName + " " + type.toString() + notNullSentence);
    return this;
  }

  public CreateTableHelper addPk(String name, SqliteType type, boolean hasAutoincrement) {
    if (columns.contains(name)) {
      throw new IllegalStateException(name + " already added");
    }
    if (pkSentence != null) {
      throw new IllegalStateException("Already have a primary key: " + pkSentence);
    }
    String autoincrement = hasAutoincrement && type == SqliteType.INTEGER ? AUTOINCREMENT : "";
    columns.add(name);
    pkSentence = name + " " + type.toString() + " PRIMARY KEY " + autoincrement;
    return this;
  }

  public String build() {
    if (alreadyBuilt) {
      throw new IllegalStateException("Create table statement already created. Can be created only once");
    }
    alreadyBuilt = true;

    if (pkSentence != null) {
      builder.append(pkSentence);
    }

    boolean first = pkSentence == null;
    for (String columnSentence : columnSentences) {
      if (!first) {
        builder.append(", ");
      }
      builder.append(columnSentence);
      first = false;
    }

    return builder.append(");").toString();
  }
}
