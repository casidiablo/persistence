package hongo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import com.codeslap.hongo.Hongo;
import com.codeslap.hongo.SqlAdapter;
import com.codeslap.hongo.sample.R;

public class MainActivity extends Activity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    SqlAdapter sqlAdapter = Hongo.getAdapter(this);
    Test test = new Test();
    test.setName("Moni");
    test.setAge(25);
    sqlAdapter.store(test);

    Test first = sqlAdapter.findFirst(Test.class, null, null);
    TextView result = (TextView) findViewById(R.id.result);
    result.setText(first.toString());
  }
}
