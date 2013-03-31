package com.codeslap.persistence.processor;

import com.codeslap.persistence.DataObject;
import com.squareup.java.JavaWriter;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.Set;

@SupportedAnnotationTypes("com.codeslap.persistence.Table")
public class PersistenceProcessor extends AbstractProcessor {
  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment env) {
    for (TypeElement typeElement : typeElements) {
      Set<? extends Element> tables = env.getElementsAnnotatedWith(typeElement);
      for (Element table : tables) {
        PackageElement packageElement = CodeGenHelper.getPackage(table);
        boolean isClass = table.getKind() == ElementKind.CLASS;
        if (isClass) {
          try {
            String mainClassName = table.getSimpleName().toString();
            String className = mainClassName + "DataObject";
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(className);

            Writer out = sourceFile.openWriter();
            JavaWriter jw = new JavaWriter(out);
            jw.emitPackage(packageElement.getSimpleName().toString());

            String superClass = DataObject.class.getName() + "<" + mainClassName + ">";
            jw.beginType(className, "class", Modifier.PUBLIC, null, superClass);

            jw.beginMethod(mainClassName, "newInstance", Modifier.PUBLIC);
            jw.emitStatement("return new " + mainClassName + "()");
            jw.endMethod();

            jw.endType();

            out.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return true;
  }
}
