Yet another persistence library for Android. This library works as a SQLite wrapper and allows you to easily create, query and work with schemas based on objects.

###How it works?

Create a class that extends `android.app.Application` like this:

```java
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SqlPersistence database = PersistenceConfig.getDatabase("database_name.db", 1);
        database.match(Foo.class, Bar.class);
    }
}
```

And add this to your manifest:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="your.package.name"
          ...>
    <application ...
                 android:name="your.package.name.App">
     ...
```

Here `Foo` and `Bar` are [POJO][1]s that you will use through your app. Persistence library will automatically create
sqlite tables for those classes, which will allow you to insert, query, update and delete data in a easy way:

In order to interact with the database, you must use an implementation of the [SqlAdapter][2] interface. There are two
implementations; you can get an instance of those implemantion this way:

// 1. Standard implementation...
SqlAdapter adapter = Persistence.getSqliteAdapter(context);
// use the adapter and then...
adapter.close();

// 2. Quick implementation
Persistence.quick(context).someMethod();

Difference between standard and quick implementation is that quick adapter can be used only once and it will run
clean-up tasks after it is executed. Use quick implementation when you just want to do a simple thing (e.g. inserting
bean); use standard implementation if you want to do more things (e.g. inserting an object, then updating another and
finally query some other data).

### Inserting data

To insert data you can use any of these methods:

**`store(object)`**

```java
// single insertion
Foo foo = new Foo();
// add data to your object foo.setExample(...);
Persistence.quick(context).store(foo);
```

This will insert a simple object to the database. **Notice:** if you are inserting an object of type `Foo`, you must
have already registered that class in the *Application class*.

**`storeCollection(list, listener)`**

```java
List<Foo> foos = new ArrayList();
// foos.add(foo);
Persistence.quick(context).storeCollection(null, new ProgressListener() {
    @Override
    public void onProgressChange(int percentage) {
    }
});
```

This will insert a collection of objects. This is much more efficient than implementing a loop manually since this will
not insert items one-by-one but instead will create a bulk insert statement. There is another version of this method
called `storeUniqueCollection` which basically inserts and updates objects that you pass in the list, and delete from
the database those items that are not included in the list.

  [1]: http://en.wikipedia.org/wiki/Plain_Old_Java_Object
  [2]: https://github.com/casidiablo/persistence/blob/master/src/main/java/com/codeslap/persistence/SqlAdapter.java