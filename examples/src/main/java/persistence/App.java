package persistence;

import android.app.Application;
import com.codeslap.persistence.DatabaseSpec;
import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.PersistenceConfig;

public class App extends Application {
  @Override public void onCreate() {
    DatabaseSpec databaseSpec = PersistenceConfig.registerSpec(1);
    databaseSpec.match(Test.class);
    Persistence.getAdapter(this).truncate(Test.class);
  }
}