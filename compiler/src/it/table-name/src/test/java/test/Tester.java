package test;

import com.codeslap.hongo.DataObject;
import com.codeslap.hongo.ReflectDataObject;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class Tester {
  @Test
  public void test() throws Exception {
    Class<?> rawClass = Class.forName(Foo.class.getName() + "DataObject");
    Class<? extends DataObject<Foo>> daoClass = (Class<? extends DataObject<Foo>>) rawClass;
    DataObject<Foo> genDao = daoClass.newInstance();

    Foo app = genDao.newInstance();
    assertNotNull("Test object must not be null", app);

    ReflectDataObject reflectDao = new ReflectDataObject(Foo.class);
    assertEquals(reflectDao.getCreateTableSentence(), genDao.getCreateTableSentence());
    assertEquals(reflectDao.getObjectType(), genDao.getObjectType());
  }
}