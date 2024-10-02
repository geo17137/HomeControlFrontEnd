package dt.cr.com.automate;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class WarningActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_warning);
    TextView textWarning = findViewById(R.id.textWarning);
    textWarning.setVisibility(View.VISIBLE);
    Button buttonReturn = findViewById(R.id.buttonReturn);
    buttonReturn.setOnClickListener(view -> {
      Unic.getInstance().setSignalDefautPompe(false);
//      Unic.getInstance().setTimeOutExit(MainActivity.TIMOUT_EXIT);
      finish();
    });
  }
}