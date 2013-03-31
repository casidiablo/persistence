package test;

import com.codeslap.persistence.DataObject;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class Tester {
  @Test
  public void test() throws Exception {
    Class<?> rawClass = Class.forName(TestClass.class.getName() + "DataObject");
    Class<? extends DataObject<TestClass>> daoClass = (Class<? extends DataObject<TestClass>>) rawClass;
    DataObject<TestClass> testAppDataObject = daoClass.newInstance();

    TestClass app = testAppDataObject.newInstance();
    assertNotNull("Test object must not be null", app);
  }
}