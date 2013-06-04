package com.codeslap.hongo;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.tools.JavaFileObject;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

@SupportedAnnotationTypes("com.codeslap.hongo.Table")
public class HongoAnnotationsProcessor extends AbstractProcessor {
  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment env) {
    for (TypeElement typeElement : typeElements) {
      Set<? extends Element> tables = env.getElementsAnnotatedWith(typeElement);
      for (Element table : tables) {
        ProcessorObjectType objectType = new ProcessorObjectType(table);
        try {
          String mainClassName = table.getSimpleName().toString();
          String className = mainClassName + "DataObject";
          PackageElement packageElement = CodeGenHelper.getPackage(table);
          String sourceName = packageElement.getQualifiedName() + "." + className;
          JavaFileObject sourceFile = createSourceFile(sourceName, table);

          Writer out = sourceFile.openWriter();

          Properties props = new Properties();
          URL url = this.getClass().getClassLoader().getResource("velocity.properties");
          props.load(url.openStream());

          // first, get and initialize an engine
          VelocityEngine ve = new VelocityEngine(props);
          ve.init();

          // next, get the Template
          Template t = ve.getTemplate("data_object_impl.vm");

          // create a context and add data
          VelocityContext context = new VelocityContext();
          context.put("packageName", packageElement.getSimpleName().toString());
          context.put("className", mainClassName);
          context.put("hasAutoincrement", shouldBeAutoIncrement(table));
          context.put("tableName", getTableName(table));
          context.put("createTableSentence", getCreateTableStatement(table));
          context.put("columnFieldBuilding", getColumnFieldsBuilding(objectType));
          context.put("hasManyListBuilding", getHasManyListBuilding(objectType));

          // now render the template into a StringWriter
          t.merge(context, out);
          out.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return true;
  }

  private static String getColumnFieldsBuilding(ObjectType table) {
    ColumnField[] declaredFields = table.getDeclaredFields();

    StringBuilder sb = new StringBuilder();
    //sb.append("columnFields[] = ")
    return sb.toString();
  }

  private static String getHasManyListBuilding(ObjectType objectType) {
    StringBuilder sb = new StringBuilder();
    for (HasManySpec hasManySpec : ClassAnalyzer.getHasManySpecs(objectType)) {
      sb.append("columnFields.add();\n");
    }
    return sb.toString();
  }

  private static String getCreateTableStatement(Element table) {
    CreateTableHelper createTable = CreateTableHelper.init(getTableName(table));

    for (Element element : table.getEnclosedElements()) {
      if (element.getKind() != ElementKind.FIELD) {
        continue;
      }
      ProcessorColumnField columnField = new ProcessorColumnField(element);
      String columnName = ColumnHelper.getColumnName(columnField);
      SqliteType type = getTypeFrom(element);
      if (CodeGenHelper.isPrimaryKey(columnField)) {
        String column = ColumnHelper.getIdColumn(columnField);
        createTable.addPk(column, type, shouldBeAutoIncrement(element));
      } else /*if (field.getType() != List.class) */ {
        boolean notNull = false;
        Column columnAnnotation = element.getAnnotation(Column.class);
        if (columnAnnotation != null) {
          notNull = columnAnnotation.notNull();
        }
        createTable.add(columnName, type, notNull);
      }
    }
    return createTable.build();
  }

  private static SqliteType getTypeFrom(Element element) {
    TypeMirror typeMirror = element.asType();
    return typeMirror.accept(new SimpleTypeVisitor6<SqliteType, Void>() {
      @Override public SqliteType visitPrimitive(PrimitiveType primitiveType, Void aVoid) {
        switch (primitiveType.getKind()) {
          case BOOLEAN:
          case BYTE:
          case SHORT:
          case INT:
          case LONG:
            return SqliteType.INTEGER;
          case FLOAT:
          case DOUBLE:
            return SqliteType.REAL;
          case CHAR:
          default:
            return SqliteType.TEXT;
        }
      }

      @Override protected SqliteType defaultAction(TypeMirror typeMirror, Void aVoid) {
        return SqliteType.TEXT;
      }
    }, null);
  }

  private static String getTableName(Element tableName) {
    return tableName.getAnnotation(Table.class).value();
  }

  private JavaFileObject createSourceFile(String name, Element element) throws IOException {
    return processingEnv.getFiler().createSourceFile(name, element);
  }

  private static boolean shouldBeAutoIncrement(Element type) {
    boolean autoincrement = true;
    for (Element element : type.getEnclosedElements()) {
      if (element.getKind() != ElementKind.FIELD) {
        continue;
      }

      if (element.getAnnotation(Ignore.class) != null) {
        continue;
      }

      PrimaryKey primaryKey = element.getAnnotation(PrimaryKey.class);
      if (primaryKey != null) {
        TypeMirror typeMirror = element.asType();
        TypeKind kind = typeMirror.getKind();
        if (kind == TypeKind.LONG || kind == TypeKind.INT) {
          autoincrement = primaryKey.autoincrement();
        } else {
          autoincrement = false;
        }
        break;
      }
    }
    return autoincrement;
  }
}
