package dt.cr.com.automate;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TimePicker;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class PowerPlageCookingActivity extends AppCompatActivity implements RadioButton.OnClickListener {
  final int DEVICE = Unic.CUISINE;

  private TimePicker timePicker;
  private RadioButton[] radioButtons;
  private Switch switchActivation;
  private boolean isEnabledTimePicker = true;
  private int nRadioButton;
  private Param cParam;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_define_power_plage_cooking);

    ActionBar actionBar = getSupportActionBar();
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);
    setTitle(R.string.AppTitle);
    cParam = Unic.getInstance().getcParam();

    timePicker = findViewById(R.id.timePickerPac);
    timePicker.setIs24HourView(true);
    radioButtons = new RadioButton[Unic.MAX_RADIO_BUTTONS];
    radioButtons[0] = findViewById(R.id.radioButtonPacOn);
    radioButtons[1] = findViewById(R.id.radioButtonPacOff);
    radioButtons[2] = findViewById(R.id.radioButtonCooking3);
    for (int i = 0; i < Unic.MAX_RADIO_BUTTONS; i++)
      radioButtons[i].setOnClickListener(this);

    switchActivation = findViewById(R.id.id_switch_pac);
    // Init de la première zone
    setTimePicker(0);

    switchActivation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switchActivation.setText(isChecked ? "Activé" : "Desactivé");
        for (int i = 0; i < radioButtons.length; i++) {
          if (radioButtons[i].isChecked())
            cParam.setEnable(isChecked, DEVICE, i);
        }
      }
    });

    timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
      @Override
      public void onTimeChanged(TimePicker timePicker, int hour, int minute) {
        if (!isEnabledTimePicker) {
          return;
        }
        cParam.set_hMax(hour, DEVICE, nRadioButton);
        cParam.set_mMax(minute, DEVICE, nRadioButton);
      }
    });

    chkBoxSetGlobalSchedParam();
  }

  @Override
  public void onClick(View v) {
    isEnabledTimePicker = false;
    for (int i = 0; i < Unic.MAX_RADIO_BUTTONS; i++) {
      if (radioButtons[i].isChecked()) {
        nRadioButton = i;
        setTimePicker(i);
      }
    }
  }

  private void setTimePicker(int plage) {
    isEnabledTimePicker = false;
    timePicker.setHour(cParam.ihMax(DEVICE, plage));
    timePicker.setMinute(cParam.imMax(DEVICE, plage));
    isEnabledTimePicker = true;
    boolean enabled = cParam.isEnable(DEVICE, plage);
    switchActivation.setChecked(enabled);
    switchActivation.setText(enabled ? "Activé" : "Desactivé");
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      String param = cParam.toString();
      Unic.getInstance().getMainActivity().writeParam(param);
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void chkBoxSetGlobalSchedParam() {
    String[] stabGlobalSchedParam = Unic.getInstance().getGlobalSchedParam();
    switchActivation.setEnabled("1".equals(stabGlobalSchedParam[ParameterActivity.POWER_COOK]));
  }
}