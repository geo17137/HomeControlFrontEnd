package dt.cr.com.automate;

import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;

public class LogsActivity extends AppCompatActivity {
  static EditText editTextLogs;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_logs);
    Unic.getInstance().setLogsActivity(this);
    ActionBar actionBar = getSupportActionBar();
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);

    editTextLogs = findViewById(R.id.id_editTextMultiLine);
    Unic.getInstance().getMainActivity().readLogs();
    Unic.getInstance().getMainActivity().getIOTLogStatus();

    Button deleteButton = findViewById(R.id.id_button_clear);
    deleteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Unic.getInstance().getMainActivity().clearLogs();
        editTextLogs.setText("");
      }
    });
  }

  public boolean onOptionsItemSelected(MenuItem item){
    if (item.getItemId() == android.R.id.home) {
        finish();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void publish(String reponse) {
    String[] tReponse = reponse.split("\n");
    ArrayList<String> al = new ArrayList<>();
    for (String line : tReponse)
      al.add(line+"\n");
    Collections.reverse(al);
    reponse = al.toString();

    reponse = reponse.replace("\n", "&lt;br&gt;");
    reponse = reponse.replace(",", "");
    editTextLogs.setText((Html.fromHtml(Html.fromHtml(reponse).toString())));
  }
}