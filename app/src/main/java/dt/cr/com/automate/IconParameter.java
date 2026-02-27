package dt.cr.com.automate;

import static dt.cr.com.automate.R.*;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

public class IconParameter extends AppCompatActivity
    implements CompoundButton.OnCheckedChangeListener {

  static final int MAX_BUTTONS = 6;
  private String[] actions = null;
  private Switch[] switches = null;

  private SharedPreferences.Editor editor = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(layout.activity_icons_parameter);
    setTitle(R.string.AppTitle);

    SharedPreferences prefs = Unic.getInstance().getPrefs();
    editor = Unic.getInstance().getEditor();

    actions = new String[MAX_BUTTONS];
    switches = new Switch[MAX_BUTTONS];

    ActionBar actionBar = getSupportActionBar();
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);

    switches[0] = findViewById(id.switch1);
    switches[0].setOnCheckedChangeListener(this);
    switches[1] = findViewById(id.switch2);
    switches[1].setOnCheckedChangeListener(this);
    switches[2] = findViewById(id.switch3);
    switches[2].setOnCheckedChangeListener(this);
    switches[3] = findViewById(id.switch4);
    switches[3].setOnCheckedChangeListener(this);
    switches[4] = findViewById(id.switch5);
    switches[4].setOnCheckedChangeListener(this);
    switches[5] = findViewById(id.switch6);
    switches[5].setOnCheckedChangeListener(this);


    String paramProtect = prefs.getString("sdat", null);
    assert paramProtect != null;
    actions = paramProtect.split(":");
    for (int i = 0; i < MAX_BUTTONS; i++) {
      switches[i].setText("1".equals(actions[i]) ? "Activé" : "Désactivé");
      switches[i].setChecked("1".equals(actions[i]));
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
      if (buttonView.equals(switches[i])) {
        Unic.getInstance().getImageButtons()[i].setEnabled(isChecked);
        buttonView.setText(isChecked ? "Activé" : "Desactivé");
        actions[i] = isChecked ? "1" : "0";
      }
    }
  }
}