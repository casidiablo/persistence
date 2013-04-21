package hongo;

import android.app.Application;
import com.codeslap.hongo.DatabaseSpec;
import com.codeslap.hongo.Hongo;
import com.codeslap.hongo.HongoConfig;

public class App extends Application {
  @Override public void onCreate() {
    DatabaseSpec databaseSpec = HongoConfig.registerSpec(1);
    databaseSpec.match(Test.class);
    Hongo.getAdapter(this).truncate(Test.class);
  }
}