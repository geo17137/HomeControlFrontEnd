package dt.cr.com.automate;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

public class VmcActivity extends AppCompatActivity implements RadioButton.OnClickListener {
    final int MAX_ZONES = 4;
    final int VMC_ON = 0;
    final int VMC_OFF = 1;
    final int DEVICE = Unic.VMC;

    //-----------------------
    //    Modes de cmd. VMC
    //-----------------------
//    private static final String CMD_VMC_OFF     =  "0";
//    private static final String CMD_VMC_PROG    =  "1";
//    private static final String CMD_VMC_ON      =  "2";
//    private static final String CMD_VMC_ON_FAST =  "3";

    private TextView title;
    private TimePicker timePicker;
    private RadioButton   radioButtonsOn;
    private RadioButton[] radioButtonsZones;
    private Switch switchActivation;
    private Switch switchRegime;
    private String[] titleString;
    private Param cParam;
    private boolean isInit;
    private boolean isEnabledTimePicker;
    private int nRadioButton;
    private int champ;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vmc);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.AppTitle);

        title = findViewById(R.id.textViewTitleVmc);
        titleString = new String[2];
        titleString[VMC_OFF] = getResources().getString(R.string.vmc_programm_off);
        titleString[VMC_ON] =  getResources().getString(R.string.vmc_programm_on);
        radioButtonsZones = new RadioButton[MAX_ZONES];

        cParam = Unic.getInstance().getcParam();
//        Log.d("debug", cParam.paramDebug());

        timePicker = findViewById(R.id.timePickerVMC);
        timePicker.setIs24HourView(true);
        radioButtonsOn = findViewById(R.id.radioButtonVmcOn);
        radioButtonsOn.setChecked(true);
        radioButtonsOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isEnabledTimePicker = false;
                title.setText(titleString[VMC_ON]);
                nRadioButton = VMC_ON;
                setTimePicker(VMC_ON);
            }
        });
        RadioButton radioButtonsOff = findViewById(R.id.radioButtonVmcOff);
        radioButtonsOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isEnabledTimePicker = false;
                title.setText(titleString[VMC_OFF]);
                nRadioButton = VMC_OFF;
                setTimePicker(VMC_OFF);
            }
        });

        radioButtonsZones[0] =findViewById(R.id.radioButtonPlage1);
        radioButtonsZones[1] =findViewById(R.id.radioButtonPlage2);
        radioButtonsZones[2] =findViewById(R.id.radioButtonPlage3);
        radioButtonsZones[3] =findViewById(R.id.radioButtonPlage4);
        for (int i=0; i < MAX_ZONES; i++)
            radioButtonsZones[i].setOnClickListener(this);

        switchActivation = findViewById(R.id.id_switch_vmc_enable);
        switchActivation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isEnabledTimePicker)
                    return;
                if (isChecked) {
                    switchActivation.setText(getResources().getText(R.string.active));
                    switchRegime.setEnabled(true);
                    if (isInit) {
                        if (!switchRegime.isChecked()) {
                            cParam.setEnable(true, DEVICE, champ);

                        } else {
                            cParam.setCmdEnable("2", DEVICE, champ);
                        }
                    }
                } else {
                    cParam.setEnable(false, DEVICE, champ);
                    switchActivation.setText(getResources().getText(R.string.desactive));
                    switchRegime.setChecked(false);
                    switchRegime.setEnabled(false);
                }
//                Log.d("debug", cParam.paramDebug());
            }
        });

        switchRegime = findViewById(R.id.id_switch_vmc_fast);
        switchRegime.setEnabled(false);
        switchRegime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
//                Log.d("debug", cParam.paramDebug());
//                Log.d("debug", "champ=" + champ + " checked=" + isChecked);
                if (switchActivation.isChecked()) {
                    if (!isChecked) {
                        cParam.setCmdEnable("1", DEVICE, champ);
                    } else {
                        cParam.setCmdEnable("2", DEVICE, champ);
                    }
                }
            }
        });
        initIhm();
        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int hour, int minute) {
                if (!isEnabledTimePicker) {
                    return;
                }
//                Log.d("debug", "onTimeChanged, nRadioButton:" + nRadioButton + " champ:" + champ);
                if (nRadioButton == VMC_ON ) {
                    cParam.set_hMin(hour, DEVICE, champ);
                    cParam.set_mMin(minute, DEVICE, champ);
                }
                else {
                    cParam.set_hMax(hour, DEVICE, champ);
                    cParam.set_mMax(minute, DEVICE, champ);
                }
                boolean activation = switchActivation.isChecked();
                if (activation)
                    if (!switchRegime.isChecked())
                        cParam.setEnable(switchActivation.isChecked(), DEVICE, champ);
                    else
                        cParam.setCmdEnable("2", DEVICE, champ);
//                Log.d("debug", cParam.paramDebug());
            }
        });

        chkBoxSetGlobalSchedParam();
        // End onCreate
    }

    private void setTimePicker(int n) {
        isEnabledTimePicker = false;
        if (n == 0 ) {
            timePicker.setHour(cParam.ihMin(DEVICE, champ));
            timePicker.setMinute(cParam.imMin(DEVICE, champ));
        }
        else {
            timePicker.setHour(cParam.ihMax(DEVICE, champ));
            timePicker.setMinute(cParam.imMax(DEVICE, champ));
        }
        isEnabledTimePicker = true;
        switch (cParam.getCmdEnable(DEVICE, champ)) {
            case 0 :
                switchActivation.setChecked(false);
                switchRegime.setChecked(false);
                break;
            case 1 :
                switchActivation.setChecked(true);
                switchRegime.setChecked(false);
                break;
            case 2 :
                switchActivation.setChecked(true);
                switchRegime.setChecked(true);
                break;
        }
    }

    private void initIhm() {
        setTimePicker(0);
        setSwitch();
        isInit = true;
    }

    private void setSwitch() {
        int val = cParam.getCmdEnable(DEVICE, 0);
        switch (cParam.getCmdEnable(DEVICE, 0)) {
            case 0 : switchActivation.setChecked(false); break;
            case 1 : switchActivation.setChecked(true); break;
            case 2 : switchActivation.setChecked(true); switchRegime.setChecked(true);break;
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            String param = cParam.toString();
//            Log.d("debug", cParam.paramDebug());
//            Log.d("debug", param);
            Unic.getInstance().getMainActivity().writeParam(param);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (!isInit)
            return;
        boolean zButton = false;
        int zone = 0;
        for (; zone < MAX_ZONES; zone++)
           if (view.getId() == radioButtonsZones[zone].getId()) {
               zButton = true;
               break;
           }
        if (zButton) {
//            Log.d("debug", "Zone " + (zone + 1));
            champ = zone;
            radioButtonsOn.setChecked(true);
            setTimePicker(VMC_ON);
        }
    }
    private void chkBoxSetGlobalSchedParam() {
        String[] stabGlobalSchedParam = Unic.getInstance().getGlobalSchedParam();
        switchActivation.setEnabled("1".equals(stabGlobalSchedParam[ParameterActivity.VMC]));
    }
}