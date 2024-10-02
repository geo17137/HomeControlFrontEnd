package dt.cr.com.automate;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

/*
  Les valeurs des paramètres sont utilisés sans modifications dans l'application cliente
  Ces paramètres ont été obtenus à partir d'essais sur la PAC et ne sont valables
  que pour une pac MITSUBISHI
 */
public class PowerPlagePacActivity extends AppCompatActivity implements RadioButton.OnClickListener {
  final int DEVICE = Unic.PAC;
  final int MAX_RADIO_BUTTONS = 2;
  final int MAX_IR_PARAM = 5;
  final int ON_OFF = 0;
  final int TEMP = 1;
  final int FAN = 2;
  final int MODE = 3;
  final int VANNE = 4;
  final int FAN_MIN = 0;
  final int FAN_MAX = 4;
  final int VANNE_MIN = 0;
  final int VANNE_MAX = 6;
  final int HEAT = 1;
  final int COOL = 3;

  final int MIN_TEMP = 16;
  final int MAX_TEMP = 30;
  private TextView title;
  private TextView textTemp;
  private TextView textFan;
  private TextView textVanne;

  private TimePicker timePicker;
  private RadioButton[] radioButtons;
  private Switch switchActivation, switchCmdPac;
  private String[] titleString;
  private boolean isEnabledTimePicker = true;
  private int nRadioButton;
  private Param cParam;

  private String irParam;
  private int[] tabIrParam;

  private ImageButton buttonPlus;

  private RadioButton radioButtonheat, radioButtonCold;

  @SuppressLint({"MissingInflatedId", "WrongViewCast"})
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_define_power_plage_pac);
    ActionBar actionBar = getSupportActionBar();
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);
    setTitle(R.string.AppTitle);
    title = findViewById(R.id.textView);
    TextView titleParam = findViewById(R.id.textViewTitleParam);
    irParam = Unic.getInstance().getIrParam();

    // Paramètre numéro
    // 0 : 1 on, 0 off
    // 1 : température par défaut
    // 2 : fan mode voir ci-dessous
    // 3 : mode de fonctionnement défaut HEAT
    // 4 : position de la vanne défaut kMitsubishiAcVaneHighest
    if (irParam == null) {
      irParam = "1:17:1:1:1";
      titleParam.setText(getResources().getText(R.string.IR_off_line));
    }
    tabIrParam = new int[MAX_IR_PARAM];
    for (int i=0; i < MAX_IR_PARAM; i++)
      tabIrParam[i] = Integer.parseInt(irParam.split(":")[i]);
    cParam = Unic.getInstance().getcParam();


    switchActivation = findViewById(R.id.id_switch_pac);
    switchCmdPac = findViewById(R.id.id_switch_cmd_pac);
    switchCmdPac.setChecked(tabIrParam[0] == 1);
    if (tabIrParam[0] == 1)
      switchCmdPac.setText("Pac on");
    else
      switchCmdPac.setText("Pac off");
    switchCmdPac.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
          Unic.getInstance().getMainActivity().setPacActive(true);
          switchCmdPac.setText("Pac on");
          tabIrParam[0] = 1;
        }
        else {
          Unic.getInstance().getMainActivity().setPacActive(false);
          switchCmdPac.setText("Pac off");
          tabIrParam[0] = 0;
        }
      }
    });


    titleString = new String[2];

    timePicker = findViewById(R.id.timePickerPac);
    timePicker.setIs24HourView(true);
    radioButtons = new RadioButton[MAX_RADIO_BUTTONS];
    radioButtons[0] = findViewById(R.id.radioButtonPacOn);
    radioButtons[1] = findViewById(R.id.radioButtonPacOff);
    titleString[0] = this.getString(R.string.pac_programm);
    titleString[1] = this.getString(R.string.power_off);

    ImageButton buttonPlus = findViewById(R.id.imageButtonPlus);
    buttonPlus.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (tabIrParam[TEMP] < MAX_TEMP) {
          tabIrParam[TEMP]++;
          textTemp.setText(Integer.toString(tabIrParam[TEMP]));
          Unic.getInstance().getMainActivity().setTemp(Integer.toString(tabIrParam[TEMP]));
        }
      }
    });
    ImageButton buttonMoins = findViewById(R.id.imageButtonMoins);
    buttonMoins.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (tabIrParam[TEMP] > MIN_TEMP) {
          tabIrParam[TEMP]--;
          textTemp.setText(Integer.toString(tabIrParam[TEMP]));
          Unic.getInstance().getMainActivity().setTemp(Integer.toString(tabIrParam[TEMP]));
        }
      }
    });
    textTemp = findViewById(R.id.textViewTemp);
    textTemp.setText(Integer.toString(tabIrParam[TEMP]));
    radioButtonheat = findViewById(R.id.radioButtonHeat);
    radioButtonheat.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        tabIrParam[MODE] = radioButtonheat.isChecked() ? HEAT : COOL;
        Unic.getInstance().getMainActivity().setMode(Integer.toString(tabIrParam[MODE]));
        textTemp.setTextColor(Color.RED);
      }
    });
    radioButtonCold = findViewById(R.id.radioButtonCold);
    radioButtonCold.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        tabIrParam[MODE] = radioButtonCold.isChecked() ? COOL : HEAT;
        Unic.getInstance().getMainActivity().setMode(Integer.toString(tabIrParam[MODE]));
        textTemp.setTextColor(Color.BLUE);
      }
    });
    radioButtonCold.setChecked(tabIrParam[MODE] == COOL);
    radioButtonheat.setChecked(tabIrParam[MODE] == HEAT);
    textTemp.setTextColor(tabIrParam[MODE] == HEAT ? Color.RED : Color.BLUE);
    textFan = findViewById(R.id.textViewFan);
    SeekBar seekBarFan = findViewById(R.id.seekBarFan);
    seekBarFan.setProgress(tabIrParam[FAN]);
    setTextFan(tabIrParam[FAN]);
    seekBarFan.setMax(FAN_MAX);
    seekBarFan.setMin(FAN_MIN);
    seekBarFan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        tabIrParam[FAN] = progress;
        setTextFan(progress);
        Unic.getInstance().getMainActivity().setFan(Integer.toString(tabIrParam[FAN]));
      }
      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    });

    textVanne = findViewById(R.id.textViewVanne);
    SeekBar seekBarVanne = findViewById(R.id.seekBarVanne);
    seekBarVanne.setProgress(tabIrParam[VANNE]);
    setTextVanne(tabIrParam[VANNE]);
    seekBarVanne.setMax(VANNE_MAX);
    seekBarVanne.setMin(VANNE_MIN);
    seekBarVanne.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (progress == 6)
          progress = 7;
        tabIrParam[VANNE] = progress;
        setTextVanne(progress);
        Unic.getInstance().getMainActivity().setVanne(Integer.toString(tabIrParam[VANNE]));
      }
      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    });

    for (int i = 0; i < MAX_RADIO_BUTTONS; i++)
      radioButtons[i].setOnClickListener(this);

    // Init de la première zone
    setTimePicker(0);

    switchActivation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        cParam.setEnable(isChecked, DEVICE, 0);
        // Activer la PAC si prog horaire activée
        if (isChecked) {
          tabIrParam[0] = 1;
          switchCmdPac.setChecked(true);
        };
//        tabIrParam[0] = isChecked ? 1 : 0;
        switchActivation.setText(isChecked ?
                getResources().getText(R.string.active) :
                getResources().getText(R.string.desactive));
      }
    });

    timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
      @Override
      public void onTimeChanged(TimePicker timePicker, int hour, int minute) {
        if (!isEnabledTimePicker) {
          return;
        }
        if (nRadioButton == 0 ) {
          cParam.set_hMin(hour, DEVICE, 0);
          cParam.set_mMin(minute, DEVICE, 0);
        }
        else {
          cParam.set_hMax(hour, DEVICE, 0);
          cParam.set_mMax(minute, DEVICE, 0);
        }
      }
    });

    chkBoxSetGlobalSchedParam();
    // end onCreate
  }

  @Override
  public void onClick(View v) {
    isEnabledTimePicker = false;
    for (int i = 0; i < MAX_RADIO_BUTTONS; i++) {
      if (radioButtons[i].isChecked()) {
        title.setText(titleString[i]);
        nRadioButton = i;
        setTimePicker(i);
      }
    }
  }

  private void setTimePicker(int zone) {
    isEnabledTimePicker = false;
    if (zone == 0 ) {
      timePicker.setHour(cParam.ihMin(DEVICE, 0));
      timePicker.setMinute(cParam.imMin(DEVICE, 0));
    }
    else {
      timePicker.setHour(cParam.ihMax(DEVICE, 0));
      timePicker.setMinute(cParam.imMax(DEVICE, 0));
    }
    isEnabledTimePicker = true;
    switchActivation.setChecked(cParam.isEnable(DEVICE, 0));
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      String param = cParam.toString();
      Unic.getInstance().getMainActivity().writeParam(param);
      StringBuilder sb = new StringBuilder();
      int i = 0;
      for (; i < MAX_IR_PARAM -1; i++)
        sb.append(tabIrParam[i] + ":");
      sb.append(tabIrParam[i]);
      Unic.getInstance().setIrParam(sb.toString());
      Unic.getInstance().getMainActivity().writeIrParam(sb.toString());
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void setTextVanne(int progress) {
    String sProgress ="";
    switch (progress) {
      case 0: sProgress = "auto"; break;
      case 1: sProgress = "max"; break;
      case 2: sProgress = "haute"; break;
      case 3: sProgress = "moyenne"; break;
      case 4: sProgress = "basse"; break;
      case 5: sProgress = "min"; break;
      case 7: sProgress = "swing"; break;
    }
    textVanne.setText("Position vanne " + sProgress);
  }

  private void setTextFan(int progress) {
    String sProgress ="";
    switch (progress) {
      case 0: sProgress = "auto"; break;
      case 1: sProgress = "1"; break;
      case 2: sProgress = "2"; break;
      case 3: sProgress = "3"; break;
      case 4: sProgress = "4"; break;
    }
    textFan.setText("Ventilation " + sProgress);
  }

  private void chkBoxSetGlobalSchedParam() {
    String[] stabGlobalSchedParam = Unic.getInstance().getGlobalSchedParam();
    switchActivation.setEnabled("1".equals(stabGlobalSchedParam[ParameterActivity.PAC]));
  }

//  public void setIrParam( String irParam) {
//    this.irParam = irParam;
//  }
}