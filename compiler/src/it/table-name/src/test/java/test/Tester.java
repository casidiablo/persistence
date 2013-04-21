package test;

import com.codeslap.hongo.DataObject;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class Tester {
  @Test
  public void test() throws Exception {
    Class<?> rawClass = Class.forName(Foo.class.getName() + "DataObject");
    Class<? extends DataObject<Foo>> daoClass = (Class<? extends DataObject<Foo>>) rawClass;
    DataObject<Foo> testAppDataObject = daoClass.newInstance();

    Foo app = testAppDataObject.newInstance();
    assertNotNull("Test object must not be null", app);
  }
}