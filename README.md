Yet another persistence library for Android. This library works as a SQLite wrapper and allows you to easily create, query and work with schemas based on objects.

###How it works?

Create a class that extends `android.app.Application` like this:

    public class App extends Application {
        @Override
        public void onCreate() {
            super.onCreate();
            SqlPersistence database = PersistenceConfig.getDatabase("database_name.db", 1);
            database.match(Foo.class, Bar.class);
        }
    }

And add this to your manifest:

    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
              package="your.package.name"
              ...>
        <application ...
                     android:name="your.package.name.App">
         ...

Here `Foo` and `Bar` are [POJO][1]s that you will use through your app. Persistence library will automatically create sqlite tables for those classes, which will allow you to insert, query, update and delete data in a easy way:

    // somewhere in your application...
    List<Foo> foos = Persistence.quick(context).findAll(Foo.class);

### Inserting data

To insert data you can use any of these methods:

**`store(object)`**

    // single insertion
    Foo foo = new Foo();
    // add data to your object foo.setExample(...);
    Persistence.quick(context).store(foo);

This will insert a simple object to the database. `Notice:` if you are inserting an object of type `Foo`, you must have already registered that class in the *Application class*.

**`storeCollection(list, listener)`**

```java
Persistence.quick(context).storeCollection(null, new ProgressListener() {
    @Override
    public void onProgressChange(int percentage) {
    }
});
```

  [1]: http://en.wikipedia.org/wiki/Plain_Old_Java_Object