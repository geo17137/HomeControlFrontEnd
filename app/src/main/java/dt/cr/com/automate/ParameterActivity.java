package dt.cr.com.automate;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;


public class ParameterActivity extends AppCompatActivity implements  SeekBar.OnSeekBarChangeListener {
    // Paramètres temporels stockés dans dlyParam
    private final int OFFSET_TIME_WATERING      = 0;
    private final int OFFSET_EAST_VALVE_ON_TIME = 1;
    private final int OFFSET_TIME_TANK_FILLING  = 2;
    private final int OFFSET_SUPRESSOR_ERROR    = 3;
    // Paramétres non temporels (stockés dans dlyParam)
    private final int OFFSET_SUMMER_TIME_OFF    = 4;
    private final int OFFSET_LOG_STATUS         = 5;
    private final int OFFSET_SUPRESSOR_STATUS   = 6;

    private final int N_DLY_PARAM               = (OFFSET_SUPRESSOR_STATUS+1);

    static  final int POWER_COOK                = 0;
    static  final int IRRIGATION                = 1;
    static  final int VANNE_EST                 = 2;
    static  final int PAC                       = 3;
    static  final int VMC                       = 4;
    static  final int N_DEVICE                  = (VMC+1);

    private final int MaxTimeOutWaterring       = 60;
    private final int MInTimeOutWaterring       = 10;
    private final int MaxTimeTankFilling        = 200;
    private final int MInTimeTankFilling        = 100;
    private final int MaxTimeOutWaterringEV_Est = 20;
    private final int MInTimeOutWaterringEV_Est = 5;
    private final int MaxTimeOutSupressor       = 100;
    private final int MInTimeOutSupressor       = 5; // 50
    private Switch  switchSummer;
    private Switch  switchLogReport;
    private Switch switchSupressorDis;
    private SeekBar seekBarTimeOutSupressor;
    private SeekBar seekBarTimeTankFilling;
    private SeekBar seekBarTimeOutWaterringEV_Est;
    private SeekBar seekBarTimeOutWaterring;
    private TextView textViewTimeOutSupressor;
    private TextView textViewTimeTankFilling;
    private TextView textViewWateringTimeEV_EST;
    private TextView textViewWateringTime;
    private EditText editTextAdr;

    private CheckBox[] tabCheckBoxSchedledParam;

    private String[] tabDlyParam;
    private int[]    itabDlyParam;
    private boolean initialised;

    SharedPreferences.Editor editor;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parameter);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.AppTitle);
        Unic.getInstance().setParameterActivity(this);
        tabDlyParam = new String[N_DLY_PARAM];
        itabDlyParam = new int[N_DLY_PARAM];
        tabCheckBoxSchedledParam = new CheckBox[N_DEVICE];

        Unic.getInstance().getMainActivity().getDlyParam();
        SharedPreferences prefs = Unic.getInstance().getPrefs();
        editor = Unic.getInstance().getEditor();

        String adrPort = Unic.getInstance().getBrockerAdr();
        String adr = adrPort.substring(0, adrPort.lastIndexOf(":"));
        String port = adrPort.substring(adrPort.lastIndexOf(":") + 1);

        editTextAdr = findViewById(R.id.editTextServerAddr);
        editTextAdr.setText(adr + ":" + port);

        textViewTimeOutSupressor = findViewById(R.id.textViewTimeOutSupressor);
        textViewTimeTankFilling = findViewById(R.id.textViewTimeTankFilling);
        textViewWateringTimeEV_EST = findViewById(R.id.textViewWateringTimeEV_EST);
        textViewWateringTime = findViewById(R.id.textViewWateringTime);
        tabCheckBoxSchedledParam[POWER_COOK] = findViewById(R.id.checkBoxPowerCook);
        tabCheckBoxSchedledParam[IRRIGATION] = findViewById(R.id.checkBoxIrrigation);
        tabCheckBoxSchedledParam[VANNE_EST] = findViewById(R.id.checkBoxLemonWaterring);
        tabCheckBoxSchedledParam[PAC] = findViewById(R.id.checkBoxPAC);
        tabCheckBoxSchedledParam[VMC] = findViewById(R.id.checkBoxVMC);
        seekBarTimeOutWaterring = findViewById(R.id.seekBarTimeOutWaterring);
        seekBarTimeOutWaterring.setMax(MaxTimeOutWaterring);
        seekBarTimeOutWaterring.setMin(MInTimeOutWaterring);
        seekBarTimeOutWaterring.setOnSeekBarChangeListener(this);
        CheckBox checkBoxArrosage = findViewById(R.id.checkBoxArrosage);
        checkBoxArrosage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkBoxArrosage.isChecked())
                    Unic.getInstance().setArrosagePermanent(true);
                else
                    Unic.getInstance().setArrosagePermanent(false);
            }
        });
//        if (Unic.getInstance().isArrosagePermanent())
//            checkBoxArrosage.setChecked(true);
//        else
//            checkBoxArrosage.setChecked(false);

        seekBarTimeTankFilling = findViewById(R.id.seekBarTimeTankWatering);
        seekBarTimeTankFilling.setOnSeekBarChangeListener(this);
        seekBarTimeTankFilling.setMax(MaxTimeTankFilling);
        seekBarTimeTankFilling.setMin(MInTimeTankFilling);

        seekBarTimeOutWaterringEV_Est = findViewById(R.id.seekBarTimeOutWaterringEV_Est);
        seekBarTimeOutWaterringEV_Est.setOnSeekBarChangeListener(this);
        seekBarTimeOutWaterringEV_Est.setMax(MaxTimeOutWaterringEV_Est);
        seekBarTimeOutWaterringEV_Est.setMin(MInTimeOutWaterringEV_Est);

        seekBarTimeOutSupressor = findViewById(R.id.seekBarTimeOutSupressor);
        seekBarTimeOutSupressor.setOnSeekBarChangeListener(this);
        seekBarTimeOutSupressor.setMax(MaxTimeOutSupressor);
        seekBarTimeOutSupressor.setMin(MInTimeOutSupressor);

        switchSummer = findViewById(R.id.switchSummer);
        switchSummer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean bval) {
                if (!initialised)
                    return;
                itabDlyParam[OFFSET_SUMMER_TIME_OFF] = bval ? 2 : 1;
                switchSummer.setText(
                        bval ?  getResources().getString(R.string.heure_ete) :
                                getResources().getString(R.string.heure_hivers));
            }
        });
        switchLogReport = findViewById(R.id.switchLogReport);
        switchLogReport.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean bval) {
                if (!initialised)
                    return;
                itabDlyParam[OFFSET_LOG_STATUS] = bval ? 1 : 0;
                switchLogReport.setText(
                        bval ?  getResources().getString(R.string.desactiver_les_logs) :
                                getResources().getString(R.string.activer_les_logs));
            }
        });

        switchSupressorDis = findViewById(R.id.switchSupressorDis);
        switchSupressorDis.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean bval) {
                if (!initialised)
                    return;
                itabDlyParam[OFFSET_SUPRESSOR_STATUS] = bval ? 1 : 0;
                switchSupressorDis.setText(
                        bval ?  getResources().getString(R.string.surpressor_en):
                                getResources().getString(R.string.surpressor_dis));
            }
        });
        chkBoxSetGlobalSchedParam();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            int i = 0;
            StringBuilder sb = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            for (; i < N_DLY_PARAM - 1; i++) {
                sb.append(itabDlyParam[i]).append(":");
            }
            sb.append(itabDlyParam[i]);
            Unic.getInstance().getMainActivity().writeDlyParam(sb.toString());
//          // Log.d("PARAM", sb.toString());
            i = POWER_COOK;
            for (; i < N_DEVICE-1; i++) {
                sb2.append(tabCheckBoxSchedledParam[i].isChecked() ? "1:" : "0:");
            }
            sb2.append(tabCheckBoxSchedledParam[i].isChecked() ? "1" : "0");
            Unic.getInstance().getMainActivity().writeScheduledParam(sb2.toString());
            Unic.getInstance().setGlobalSchedParam(sb2.toString());
            editor.putString("brocker", editTextAdr.getText().toString());
            editor.commit();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (!initialised)
            return;

        int id = seekBar.getId();
        if (id == seekBarTimeOutWaterring.getId()) {
            itabDlyParam[OFFSET_TIME_WATERING] = i * 60;
            textViewWateringTime.setText(getResources().getString(R.string.duree_arrosage) +
                    " " + i + " mn");
        }
        else if (id == seekBarTimeOutWaterringEV_Est.getId()) {
            itabDlyParam[OFFSET_EAST_VALVE_ON_TIME] = i * 60;
            textViewWateringTimeEV_EST.setText(getResources().getString(R.string.temps_arrosage_EV_EST) +
                    " " + i + " mn");
        }
        else if (id == seekBarTimeTankFilling.getId()) {
            itabDlyParam[OFFSET_TIME_TANK_FILLING] = i;
            textViewTimeTankFilling.setText(getResources().getString(R.string.duree_remplissage_reservoir) +
                    " " + i + " s");
        }
        else if (id == seekBarTimeOutSupressor.getId()) {
            itabDlyParam[OFFSET_SUPRESSOR_ERROR] = i;
            textViewTimeOutSupressor.setText(getResources().getString(R.string.temps_avant_mise_en_securite) +
                    " " + i + " s");
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void setDlyParam(String reponse) {
//        Log.d("PARAM", reponse);
        tabDlyParam = reponse.split(":");
        int time = Integer.parseInt(tabDlyParam[OFFSET_TIME_WATERING]);
        itabDlyParam[OFFSET_TIME_WATERING] = time;
        time = time/60;
        seekBarTimeOutWaterring.setProgress(time);
        textViewWateringTime.setText(getResources().getString(R.string.duree_arrosage) +
                " " + time + " mn");

        time = Integer.parseInt(tabDlyParam[OFFSET_EAST_VALVE_ON_TIME]);
        itabDlyParam[OFFSET_EAST_VALVE_ON_TIME] = time;
        time = time/60;
        seekBarTimeOutWaterringEV_Est.setProgress(time);
        textViewWateringTimeEV_EST.setText(getResources().getString(R.string.temps_arrosage_EV_EST) +
                " " + time + " mn");

        time = Integer.parseInt(tabDlyParam[OFFSET_TIME_TANK_FILLING]);
        itabDlyParam[OFFSET_TIME_TANK_FILLING] = time;
        seekBarTimeTankFilling.setProgress(time);
        textViewTimeTankFilling.setText(getResources().getString(R.string.duree_remplissage_reservoir) +
                " " + time + " s");

        time = Integer.parseInt(tabDlyParam[OFFSET_SUPRESSOR_ERROR]);
        seekBarTimeOutSupressor.setProgress(time);
        itabDlyParam[OFFSET_SUPRESSOR_ERROR] = time;
        textViewTimeOutSupressor.setText(getResources().getString(R.string.temps_avant_mise_en_securite) +
                " " + time + " s");

        boolean bval = "2".equals(tabDlyParam[OFFSET_SUMMER_TIME_OFF]);
        switchSummer.setChecked(bval);
        itabDlyParam[OFFSET_SUMMER_TIME_OFF] = bval ? 2 : 1;
        switchSummer.setText(
                bval ?  getResources().getString(R.string.heure_ete) :
                        getResources().getString(R.string.heure_hivers));

        bval = "1".equals(tabDlyParam[OFFSET_LOG_STATUS]);
        switchLogReport.setChecked(bval);
        itabDlyParam[OFFSET_LOG_STATUS] = bval ? 1 : 0;
        switchLogReport.setText(
                bval ?  getResources().getString(R.string.desactiver_les_logs) :
                        getResources().getString(R.string.activer_les_logs));
        bval = "1".equals(tabDlyParam[OFFSET_SUPRESSOR_STATUS]);
        switchSupressorDis.setChecked(bval);
        itabDlyParam[OFFSET_SUPRESSOR_STATUS] = bval ? 1 : 0;
        switchSupressorDis.setText(
                bval ?  getResources().getString(R.string.surpressor_en):
                        getResources().getString(R.string.surpressor_dis));
        initialised = true;
    }

    private void chkBoxSetGlobalSchedParam() {
        String[] stabGlobalSchedParam = Unic.getInstance().getGlobalSchedParam();
        for (int i= POWER_COOK; i < VMC + 1; i++) {
          tabCheckBoxSchedledParam[i].setChecked("1".equals(stabGlobalSchedParam[i]));
        }
    }
}