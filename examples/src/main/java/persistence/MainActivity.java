package persistence;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.SqlAdapter;
import com.codeslap.persistence.sample.R;

public class MainActivity extends Activity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    SqlAdapter sqlAdapter = Persistence.getAdapter(this);
    Test test = new Test();
    test.setName("Moni");
    test.setAge(25);
    sqlAdapter.store(test);

    Test first = sqlAdapter.findFirst(Test.class, null, null);
    TextView result = (TextView) findViewById(R.id.result);
    result.setText(first.toString());
  }
}
