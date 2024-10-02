package dt.cr.com.automate;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

public class PlageIrrigationActivity extends AppCompatActivity
    implements RadioButton.OnClickListener {

  final int DEVICE = Unic.IRRIGTION_POTAGER;
  private TimePicker timePicker;
  private RadioButton[] radioButtons;
  private Switch switchActivation;
  private TextView texViewXjour;
  private SeekBar seekBarJours;
  private Param cParam;

  private String[] param;
  private boolean isEnabledTimePicker = true;
  private int nRadioButton;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_define_plage_irrigation);
    ActionBar actionBar = getSupportActionBar();
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);
    setTitle(R.string.AppTitle);
    Unic.getInstance().setPlageIrrigationActivity(this);
    Unic.getInstance().getMainActivity().mqttGetGlobalScheduledParam();
    cParam = Unic.getInstance().getcParam();

    timePicker = findViewById(R.id.timePickerIrrigation);
    timePicker.setIs24HourView(true);
    timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
      @Override
      public void onTimeChanged(TimePicker timePicker, int hour, int minute) {
        if (!isEnabledTimePicker) {
          return;
        }
        cParam.set_hMin(hour, DEVICE, nRadioButton);
        cParam.set_mMin(minute, DEVICE, nRadioButton);
      }
    });
    texViewXjour = findViewById(R.id.textViewXjours);
    seekBarJours = findViewById(R.id.seekBarJours);
    seekBarJours.setMin(1);
    seekBarJours.setMax(14);
    seekBarJours.setProgress(cParam.ihMax(DEVICE, 0));
    Switch switchCircuit2 = findViewById(R.id.switchCircuit2);
    switchCircuit2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Unic.getInstance().getMainActivity().setVanneCircuit2(isChecked);
      }
    });
    switchCircuit2.setChecked("on".equals(Unic.getInstance().getCircuit2Status()));

    texViewXjour.setText("Tout les " + cParam.ihMax(DEVICE, 0) + " jours. Jour courant : " + (cParam.imMax(DEVICE, 0)));

    seekBarJours.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

        if (b) {
//          Log.d("debug", "seekbar user" + ":" + i);
          texViewXjour.setText("Tout les " + i +" jours. Jour courant : " + (cParam.imMax(DEVICE, 0)));
          cParam.set_hMax(i, DEVICE, 0);
        }
//        else {
//          Log.d("debug", "seekbar " + ":" + i);
//        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });


    radioButtons = new RadioButton[Unic.MAX_RADIO_BUTTONS];
    radioButtons[0] = findViewById(R.id.radioButtonIrrigation1);
    radioButtons[1] = findViewById(R.id.radioButtonIrrigation2);
    radioButtons[2] = findViewById(R.id.radioButtonIrrigation3);
    for (int i = 0; i < Unic.MAX_RADIO_BUTTONS; i++)
      radioButtons[i].setOnClickListener(this);

    switchActivation = findViewById(R.id.id_switch_z);
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
    setTimePicker(0);
    chkBoxSetGlobalSchedParam();
    // End Oncreate
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
    isEnabledTimePicker = true;
  }

  private void setTimePicker(int plage) {
    isEnabledTimePicker = false;
    timePicker.setHour(cParam.ihMin(DEVICE, plage));
    timePicker.setMinute(cParam.imMin(DEVICE, plage));
    isEnabledTimePicker = true;
    boolean enabled = cParam.isEnable(DEVICE, plage);
    switchActivation.setChecked(enabled);
    switchActivation.setText(enabled ? "Activé" : "Desactivé");
  }

  private void chkBoxSetGlobalSchedParam() {
    String[] stabGlobalSchedParam = Unic.getInstance().getGlobalSchedParam();
    switchActivation.setEnabled("1".equals(stabGlobalSchedParam[ParameterActivity.IRRIGATION]));
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      Unic.getInstance().getMainActivity().writeParam(cParam.toString());
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}