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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlageIrrigationVanneEstActivity extends AppCompatActivity implements View.OnClickListener {
  final int DEVICE = Unic.IRRIGTION_FACADE_SUD;
  final int MIN_SEEK_BAR_DUREE = 5;
  final int MAX_SEEK_BAR_DUREE = 20;
  final int MAX_RADIO_BUTTONS = 3;

  private TimePicker timePicker;

  private RadioButton[] radioButtons;
  private int nRadioButton;
  private int[] tempo;

  private Switch switchActivation;
  private SeekBar seekBarDuree, seekBarDebit;
  private TextView textTempo, textDebit;

  private Param cParam;
  private boolean isEnabledTimePicker = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_define_plage_irrigation_vanne_est);

    ActionBar actionBar = getSupportActionBar();
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);
    setTitle(R.string.AppTitle);
    cParam = Unic.getInstance().getcParam();
    textTempo = findViewById(R.id.textViewDuree);
    textDebit= findViewById(R.id.textViewDebit);
    int iDebit = cParam.ihMin(DEVICE, 3);
    textDebit.setText("Débit " + Integer.toString((iDebit*10)/2)  + "%");
    tempo = new int[MAX_RADIO_BUTTONS];

    timePicker = findViewById(R.id.timePickerEst);
    timePicker.setVisibility(View.VISIBLE);
    timePicker.setIs24HourView(true);
    timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
      @Override
      public void onTimeChanged(TimePicker view, int hour, int minute) {
        if (!isEnabledTimePicker) {
          return;
        }
        cParam.set_hMin(hour, DEVICE, nRadioButton);
        cParam.set_mMin(minute, DEVICE, nRadioButton);
        setTimeMax(nRadioButton);
//        Log.d("param", "hMin:"+ String.valueOf(hour));
//        Log.d("param", "mMin:"+ String.valueOf(minute));
//        Log.d("param", "radio button:"+ nRadioButton);
//        Log.d("param", ""+ paramDebug());
      }
    });

    radioButtons = new RadioButton[MAX_RADIO_BUTTONS];
    radioButtons[0] = findViewById(R.id.radioButtonEst1);
    radioButtons[1] = findViewById(R.id.radioButtonEst2);
    radioButtons[2] = findViewById(R.id.radioButtonEst3);
    for (RadioButton radioButton : radioButtons) {
      radioButton.setOnClickListener(this);
    }

    switchActivation = findViewById(R.id.id_switch_est);
    seekBarDuree = findViewById(R.id.seekBar);
    seekBarDuree.setMax(MAX_SEEK_BAR_DUREE);
    seekBarDuree.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//        Log.d("Tag1", "  " + progress);
        int tempo = progress + MIN_SEEK_BAR_DUREE;
        textTempo.setText("Durée " + tempo + " mn");
        for (int i = 0; i < radioButtons.length; i++) {
          if (radioButtons[i].isChecked()) {
            PlageIrrigationVanneEstActivity.this.tempo[i] = tempo;
            if (fromUser)
              setTimeMax(i);
          }
        }
      }
      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    });

    seekBarDebit = findViewById(R.id.seekBarDebit);
    seekBarDebit.setMax(19);
    seekBarDebit.setProgress(iDebit-1);
    seekBarDebit.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//        Log.d("Tag1", "  " + progress);
        int debit = progress + 1;
        textDebit.setText("Debit " + (debit*10)/2 + "%");
        cParam.set_hMin(debit, DEVICE, 3);
      }
      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    });


    // Initialiser le premier time picker
    setTimePickerAndProgress(0);
    seekBarDuree.setProgress(tempo[0] - MIN_SEEK_BAR_DUREE);
    textTempo.setText("Durée " + tempo[0] + " mn");


    switchActivation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switchActivation.setText(isChecked ? "Activé" : "Desactivé");
        for (int i = 0; i < radioButtons.length; i++) {
          if (radioButtons[i].isChecked()) {
            cParam.setEnable(isChecked, DEVICE, i);
          }
        }
//        Log.d("param", ""+ cParam.paramDebug());
      }
    });

    chkBoxSetGlobalSchedParam();
  }

  private void setTimePickerAndProgress(int plage) {
    String hDebut = cParam.hMin(DEVICE, plage);
    String mDebut = cParam.mMin(DEVICE, plage);
    isEnabledTimePicker = false;
    timePicker.setHour(Integer.parseInt(hDebut));
    timePicker.setMinute(Integer.parseInt(mDebut));
    isEnabledTimePicker = true;

    String start = hDebut + ":" + mDebut;
    String end = cParam.hMax(DEVICE, plage) + ":" + cParam.mMax(DEVICE, plage);
    SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.FRANCE);
    try {
      Date date1 = format.parse(start);
      Date date2 = format.parse(end);
      assert date1 != null;
      assert date2 != null;
      long difference = date2.getTime() - date1.getTime();
      if (difference == 0)
        difference = 60*5000;
      int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(difference);
      if(minutes < 0)
        minutes += 1440;
      tempo[plage] = minutes;
    } catch (ParseException e) {
      e.printStackTrace();
    }
    if (cParam.isEnable(DEVICE, plage)) {
      switchActivation.setChecked(true);
      switchActivation.setText("Activé");
    }
    else {
      switchActivation.setChecked(false);
      switchActivation.setText("Desactivé");
    }

    seekBarDuree.setProgress(tempo[plage] - MIN_SEEK_BAR_DUREE);
  }

  private void setTimeMax(int nRadioButton) {
    int progress = seekBarDuree.getProgress() + MIN_SEEK_BAR_DUREE;
    int h1 = cParam.ihMin(DEVICE, nRadioButton);
    int m1 = cParam.imMin(DEVICE, nRadioButton);
    int m2 = m1 + progress;
    if (m2 > 59) {
      h1 += 1;
      if (h1 > 23)
        h1 = 0;
      m2 = (progress) - (60 - m1);
    }
    cParam.set_hMax(h1, DEVICE, nRadioButton);
    cParam.set_mMax(m2, DEVICE, nRadioButton);
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

  @Override
  public void onClick(View view) {
    for (int i = 0; i < radioButtons.length; i++) {
      if (radioButtons[i].isChecked()) {
        nRadioButton = i;
        setTimePickerAndProgress(i);
      }
    }
  }
  private void chkBoxSetGlobalSchedParam() {
    String[] stabGlobalSchedParam = Unic.getInstance().getGlobalSchedParam();
    switchActivation.setEnabled("1".equals(stabGlobalSchedParam[ParameterActivity.VANNE_EST]));
  }
}