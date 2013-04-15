package persistence;

import com.codeslap.persistence.PrimaryKey;
import com.codeslap.persistence.Table;

@Table("tests")
public class Test {
  @PrimaryKey(autoincrement = false)
  private long _id;
  private String name;
  private int age;

  public long getId() {
    return _id;
  }

  public void setId(long id) {
    _id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  @Override public String toString() {
    return "Test{" +
        "_id=" + _id +
        ", name='" + name + '\'' +
        ", age=" + age +
        '}';
  }
}
