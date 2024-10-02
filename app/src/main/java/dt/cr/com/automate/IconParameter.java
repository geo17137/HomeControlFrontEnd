package dt.cr.com.automate;

import static dt.cr.com.automate.R.*;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

public class IconParameter extends AppCompatActivity
    implements CompoundButton.OnCheckedChangeListener {

  static final int MAX_BUTTONS = 6;
  private String[] actions = null;
  private Switch[] switchs = null;

  private SharedPreferences.Editor editor = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(layout.activity_icons_parameter);
    setTitle(R.string.AppTitle);

    SharedPreferences prefs = Unic.getInstance().getPrefs();
    editor = Unic.getInstance().getEditor();

    actions = new String[MAX_BUTTONS];
    switchs = new Switch[MAX_BUTTONS];

    ActionBar actionBar = getSupportActionBar();
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);

    switchs[0] = findViewById(id.switch1);
    switchs[0].setOnCheckedChangeListener(this);
    switchs[1] = findViewById(id.switch2);
    switchs[1].setOnCheckedChangeListener(this);
    switchs[2] = findViewById(id.switch3);
    switchs[2].setOnCheckedChangeListener(this);
    switchs[3] = findViewById(id.switch4);
    switchs[3].setOnCheckedChangeListener(this);
    switchs[4] = findViewById(id.switch5);
    switchs[4].setOnCheckedChangeListener(this);
    switchs[5] = findViewById(id.switch6);
    switchs[5].setOnCheckedChangeListener(this);


    String paramProtect = prefs.getString("sdat", null);
    actions = paramProtect.split(":");
    for (int i = 0; i < MAX_BUTTONS; i++) {
      switchs[i].setText("1".equals(actions[i]) ? "Activé" : "Désactivé");
      switchs[i].setChecked("1".equals(actions[i]));
    }
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      StringBuilder dataParam = new StringBuilder();
      int i = 0;
      for (; i < MAX_BUTTONS; i++)
        dataParam.append(actions[i]).append(':');
      dataParam.append(actions[i]);
      editor.putString("sdat", dataParam.toString());
      editor.commit();
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int i = 0;
    for (; i < MAX_BUTTONS; i++) {
      if (buttonView.equals(switchs[i])) {
        Unic.getInstance().getImageButtons()[i].setEnabled(isChecked);
        buttonView.setText(isChecked ? "Activé" : "Desactivé");
        actions[i] = isChecked ? "1" : "0";
      }
    }
  }
}