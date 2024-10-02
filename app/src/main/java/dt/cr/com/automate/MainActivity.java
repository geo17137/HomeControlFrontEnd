/*
Programme permettant piloter la carte ES32A08 équipée d'un ESP32
Permet :
- la mise hors/sous tension des appareils de cuisson de façon manuelle ou programmée
- le remplissage du réservoir d'irrigation de façon manuelle ou programmée
- le pilotage d'une électrovanne de façon manuelle ou programmée
- la commande d'une lance d'arrosage
- le pilotage d'une PAC de façon manuelle ou programmée (via une carte Wifi IR déportée)
- la commande d'une VMC avec marche lente/rapide de façon manuelle ou programmée (via une carte relais Wifi)
 */
package dt.cr.com.automate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.io.FileUtils;

public class MainActivity extends AppCompatActivity {
  final String TARGET_OFF = "";
  final String LOCAL_ADRESS = Secret.LOCAL_ADRESS;
  final String ADDRESS = Secret.ADDRESS;

  static final int MAX_PARAM =Unic.MAX_PARAM;
  // Indices des champs définis dans Unic
  //  public static final int CUISINE = 0;
  //  public static final int IRRIGTION_POTAGER = 1;
  //  public static final int IRRIGTION_FACADE_SUD = 2;
  //  public static final int PAC = 3;
  //  public static final int VMC = 4;

// Se référer au client mqtt (projet platformio Esp32_HomeCtrl)
// Numéro des pors GPIO relais
  private final int GPIO_ARROSAGE   = 0;
  private final int GPIO_IRRIGATION = 1;
  private final int GPIO_FOUR       = 2;
  private final int GPIO_EV_EST     = 3;
  private final int GPIO_VMC        = 4;
  private final int GPIO_PAC        = 5;

  private int testMqttCounter = 0;
  private int testClientCounter = 0;
  private long lastTouchTime = 0;
  private long currentTouchTime = 0;

  private final String TAG = "mqtt";

  private ImageButton cmd1;
  private ImageButton cmd2;
  private ImageButton cmd3;
  private ImageButton cmd4;
  private ImageButton cmd5;
  private ImageButton cmd6;
//  private ImageButton cmd7;
//  private ImageButton cmd8;

  private TextView textStatus;

  private MenuItem item_menu_reservoir;
  private MenuItem item_menu_irrigation;
  private MenuItem item_menu_cooking;
  private MenuItem item_menu_vanne_est;
  private MenuItem item_menu_reboot;
  private MenuItem item_menu_logs;
  private MenuItem itemPower;
  private MenuItem itemWatchDog;
  private MenuItem item_menu_prog_vmc;
  private MenuItem item_menu_prog_pac;
  private MenuItem item_menu_irrigation_permanent;
  private MenuItem item_menu_parametrer_client;

  private static final CharSequence SSID1 = "jeanbart";
  private static final CharSequence SSID2 = "jeanbart_5GHz";
  private static final CharSequence SSID3 = "jeanbart_plus";
  private static final CharSequence SSID4 = "AndroidWifi1";

  //-----------------------
  //    Modes de fonct. VMC
  //-----------------------
  private static final int VMC_STOP         = 0;
  private static final int VMC_PROG_OFF     = 1;
  private static final int VMC_PROG_ON      = 2;
  private static final int VMC_PROG_ON_FAST = 3;
  private static final int VMC_ON           = 5;
  private static final int VMC_ON_FAST      = 4;

// Résultat des lecture GPIO 5..7

//  private int statusArrosageNx;
  private int statusCmd_PAC;
//  private int networkTest;
  private int cmdVmc;
  private int cmdVmc_N1;

  private boolean init;
  private boolean evEstOn;
//  private boolean vmcOff;
  private boolean status_four;

  private boolean arrosageEncours;
  private boolean irrigationEnCours;
  private boolean arretEncours;
  private boolean isClientConnected;
  private boolean paramGet;
  private Runnable runnable;
  private Handler handler;
  private MqttHelper mqttHelper;
  private StringBuilder logBuffer;

  private final String PREFIX = TARGET_OFF;
//----------- Publications -----------
  private final String TOPIC_GET_PARAM      =  PREFIX + "homecontrol/param_get";
  private final String TOPIC_WRITE_PARAM    =  PREFIX + "homecontrol/write_param";
  private final String TOPIC_GET_DLY_PARAM  =  PREFIX + "homecontrol/get_dly_param";
  private final String TOPIC_WRITE_DLY_PARAM=  PREFIX + "homecontrol/write_dly_param";
  private final String TOPIC_WRITE_GLOBAL_SCHED=PREFIX+ "homecontrol/global_sched_write";
  private final String TOPIC_GET_GLOBAL_SCHED = PREFIX+ "homecontrol/global_sched_get";
  private final String TOPIC_GET_GPIO       =  PREFIX + "homecontrol/get_gpio";
  private final String TOPIC_CMD_ARROSAGE   =  PREFIX + "homecontrol/arrosage";
  private final String TOPIC_CMD_IRRIGATION =  PREFIX + "homecontrol/irrigation";
  private final String TOPIC_CMD_CUISINE    =  PREFIX + "homecontrol/cuisine";
  private final String TOPIC_CMD_VMC        =  PREFIX + "homecontrol/vmc";
  private final String TOPIC_CMD_VANNE_EST  =  PREFIX + "homecontrol/vanne_est";
  private final String TOPIC_CMD_PAC        =  PREFIX + "homecontrol/pac";
  private final String TOPIC_LOGS_GET       =  PREFIX + "homecontrol/logs_get";
  private final String TOPIC_CLEAR_LOGS     =  PREFIX + "homecontrol/clear_logs";
  private final String TOPIC_REBOOT         =  PREFIX + "homecontrol/reboot";
  private final String TOPIC_GET_VERSION    =  PREFIX + "homecontrol/versions_get";
  private final String TOPIC_WATCH_DOG_OFF  =  PREFIX + "homecontrol/watch_dog_off";
  private final String TOPIC_CMD_REAMORCER  =  PREFIX + "homecontrol/rearmorcer";

  private final String VMC_BOARD_ACTION       =  PREFIX + "vmc_board/action";

//  private static final String SUB_GPIO0_ACTION  = "board1/action";
  private final String TOPIC_PAC_IR_PARAM_SET =  PREFIX + "mitsubishi/param/set";
  private final String TOPIC_PAC_IR_PARAM_GET =  PREFIX + "mitsubishi/param/get";
//  private static final String TOPIC_PAC_IR_PARAM_APPLY = "mitsubishi/param/apply";
  private final String TOPIC_PAC_IR_ON     =  PREFIX + "mitsubishi/param/on";
  private final String TOPIC_PAC_IR_OFF    =  PREFIX + "mitsubishi/param/off";
  private final String TOPIC_PAC_IR_TEMP   =  PREFIX + "mitsubishi/param/temp";
  private final String TOPIC_PAC_IR_MODE   =  PREFIX + "mitsubishi/param/mode";
  private final String TOPIC_PAC_IR_FAN    =  PREFIX + "mitsubishi/param/fan";
  private final String TOPIC_PAC_IR_VANNE  =  PREFIX + "mitsubishi/param/vanne";
  private final String TOPIC_PAC_IR_VERSION_GET  =  PREFIX + "mitsubishi/get_version";
  private final String TOPIC_CIRCUIT2_ACTION  = "circuit2/action";
  //--------------------------------- Abonnements --------------------------------------

  private final String TOPIC_READ_VERSION = PREFIX + "homecontrol/readVersion";
  private final String TOPIC_READ_LOGS    = PREFIX + "homecontrol/readLogs";
  private final String TOPIC_PARAM        = PREFIX + "homecontrol/param";
  private final String TOPIC_DLY_PARAM    = PREFIX + "homecontrol/dly_param";
  private final String TOPIC_GLOBAL_SCHED = PREFIX + "homecontrol/global_sched";
  private final String TOPIC_GPIO         = PREFIX + "homecontrol/gpio";
  private final String TOPIC_DEFAUT_SUPRESSEUR=PREFIX+"homecontrol/defaut_reservoir";
  private final String TOPIC_CIRCUIT2_STATUS  = "circuit2/status";


  private final String PUB_POWER_STATUS             =  PREFIX +  "board1/status";
  private final String TOPIC_PAC_IR_PARAM_PUB       =  PREFIX + "mitsubishi/param/pub";
  private final String TOPIC_PAC_IR_VERSION         =  PREFIX + "mitsubishi/version";

  private final String TOPIC_VMC_STATUS             =  PREFIX + "vmc_board/status";

  // Commandes
  private static final String ON  = "on";
  private static final String OFF  = "off";
  // Commandes spécific VMC
    private final String GET_STATUS =        "get_status";
  private final String   PUBLISH_STATE_ON =   "pub_on";
  private final String   PUBLISH_STATE_OFF =  "pub_off";
  private int timout;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    init_();
  }

  /**
   * Appelée lorsque que l’activité est suspendue.
   * Stoppez les actions qui consomment des ressources.
   * L’activité va passer en arrière-plan.
   */
  @Override
  public void onPause() {
    if (init) {
      handler.removeCallbacks(runnable);
    }
    super.onPause();
  }

  /**
   * Appelée lorsque que l’activité est arrêtée.
   * Vider  le cache de l'application
   */
  @Override
  public void onDestroy() {
    FileUtils.deleteQuietly(getApplicationContext().getCacheDir());
    super.onDestroy();
  }

  /**
   * Appelée après le démarrage ou une pause.
   * Relancez les opérations arrêtées (threads). Mettez à
   * jour votre application et vérifiez vos écouteurs.
   */
  @Override
  public void onResume() {
    super.onResume();
    if (init)
      runnable.run();
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  private void init_() {
//    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
////      || ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
//    {
//      // Ces requètes se font en arrière plan, il est important lancer l'appli (startApp) effective dans l'écouteur onRequestPermissionsResult
//      ActivityCompat.requestPermissions(this, new String[]{
//          Manifest.permission.WRITE_EXTERNAL_STORAGE
////          Manifest.permission.READ_EXTERNAL_STORAGE
////          Manifest.permission.MANAGE_EXTERNAL_STORAGE
//      }, 1); // code permettant de différentier les différents blocs if (checkSelfPermission...)
//    } else
    {
      logBuffer = new StringBuilder();
      Unic.getInstance().setcParam(new Param());
      startApp();
      init = true;
    }
  }

  public boolean getWifiInfo(Context context) {
    // Attention il faut que le GPS soit activé !!!
    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    String ssid = wm.getConnectionInfo().getSSID();
    return ssid.contains(SSID1) || ssid.contains(SSID2) || ssid.contains(SSID3) || ssid.contains(SSID4);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    boolean permissionOK = true;
//    switch (requestCode) {
//      case 1:
    for (int i = 0; i < permissions.length; i++) {
      if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
        permissionOK = false;
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
          startActivity(intent);
          permissionOK = true;
        }
      }
    }
    if (permissionOK) {
      startApp();
      init = true;
    } else {
       System.exit(0);
    }
  }

  private void startApp() {
    String serverMqtt;
    setMenuEnabled(false);
//    powerOn_N_1 = !powerOn;
    setContentView(R.layout.activity_main);
    setTitle(R.string.AppTitle);
    textStatus = findViewById(R.id.textStatus);

    Unic.getInstance().setImageButtons(new ImageButton[IconParameter.MAX_BUTTONS]);
    Unic.getInstance().setImageButton(cmd1 = findViewById(R.id.imageButton1), 0);
    Unic.getInstance().setImageButton(cmd2 = findViewById(R.id.imageButton2), 1);
    Unic.getInstance().setImageButton(cmd3 = findViewById(R.id.imageButton3), 2);
    Unic.getInstance().setImageButton(cmd4 = findViewById(R.id.imageButton4), 3);
    Unic.getInstance().setImageButton(cmd5 = findViewById(R.id.imageButton5), 4);
    Unic.getInstance().setImageButton(cmd6 = findViewById(R.id.imageButton6), 5);

    Unic.getInstance().set_this(this);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    setProtect();
    if (getWifiInfo(this)) {
      serverMqtt = LOCAL_ADRESS;
    }
    else {
      serverMqtt = getBrocker();
    }
    Unic.getInstance().setBrockerAdr(serverMqtt);
    mqttHelper = new MqttHelper(this, serverMqtt);

    cmd1.setOnClickListener(View -> {
      if (arrosageEncours) {
        mqttHelper.publish(TOPIC_CMD_ARROSAGE, "0".getBytes());
        arrosageEncours = false;
        return;
      }
      lastTouchTime = currentTouchTime;
      currentTouchTime = System.currentTimeMillis();
      if (currentTouchTime - lastTouchTime < 450) {
        lastTouchTime = 0;
        currentTouchTime = 0;
        if (Unic.getInstance().isArrosagePermanent()) {
          mqttHelper.publish(TOPIC_CMD_ARROSAGE, "2".getBytes());
        }
        else {
          mqttHelper.publish(TOPIC_CMD_ARROSAGE, "1".getBytes());
        }
      }
    });

    cmd2.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (irrigationEnCours) {
          mqttHelper.publish(TOPIC_CMD_IRRIGATION, "0".getBytes());
          irrigationEnCours = false;
          return;
        }
        lastTouchTime = currentTouchTime;
        currentTouchTime = System.currentTimeMillis();
        if (currentTouchTime - lastTouchTime < 450) {
          lastTouchTime = 0;
          currentTouchTime = 0;
          mqttHelper.publish(TOPIC_CMD_IRRIGATION, "1".getBytes());
        }
      }
    });

    cmd3.setOnClickListener(view -> {
      if (status_four) {
        mqttHelper.publish(TOPIC_CMD_CUISINE, "0".getBytes());
      } else {
        mqttHelper.publish(TOPIC_CMD_CUISINE, "1".getBytes());
      }

    });

    // Commande VMC
    cmd4.setOnClickListener(view -> {
      cmdVmc = cmdVmc % 4;
      switch (cmdVmc) {
        case 0 :
          // Mode off (pas de vcm active, même ne mode programmée)
          mqttHelper.publish(TOPIC_CMD_VMC, "0".getBytes());
          cmdVmc++;
          break;
        case 1 :
          // Mode programmée, lent ou rapide suivant programme
          mqttHelper.publish(TOPIC_CMD_VMC, "1".getBytes());
          cmdVmc++;
          break;
        case 2 :
          // Mode forcé rapide (programmation off)
          mqttHelper.publish(TOPIC_CMD_VMC, "2".getBytes());
          mqttHelper.publish(VMC_BOARD_ACTION, PUBLISH_STATE_ON.getBytes());
          cmdVmc++;
          break;
        case 3 :
          // Mode forcé lent (programmation off)
          mqttHelper.publish(TOPIC_CMD_VMC, "3".getBytes());
          mqttHelper.publish(VMC_BOARD_ACTION, PUBLISH_STATE_OFF.getBytes());
          mqttHelper.publish(VMC_BOARD_ACTION, OFF.getBytes());
          cmdVmc++;
      }
    });

    cmd5.setOnClickListener(view -> {
      if (evEstOn) {
        mqttHelper.publish(TOPIC_CMD_VANNE_EST, "0".getBytes());
        cmd5.setBackgroundResource(R.mipmap.ic_arrosage_est_off);
      } else {
        mqttHelper.publish(TOPIC_CMD_VANNE_EST, "1".getBytes());
        cmd5.setBackgroundResource(R.mipmap.ic_arrosage_est_on_prog);
      }
    });

    // envoi de commande de type bascule
    // PAC
    cmd6.setOnClickListener(view -> {
      lastTouchTime = currentTouchTime;
      currentTouchTime = System.currentTimeMillis();
      if (currentTouchTime - lastTouchTime < 450) {
        lastTouchTime = 0;
        currentTouchTime = 0;
        // Le relai de puissance est actif au repos
        if (statusCmd_PAC == 1) {
          // Coupure PAC
          if (!arretEncours) {
            mqttHelper.publish(TOPIC_CMD_PAC, "1".getBytes());
          }
          else {
            mqttHelper.publish(TOPIC_CMD_PAC, "0".getBytes());
          }
        } else {
          mqttHelper.publish(TOPIC_CMD_PAC, "0".getBytes());
        }
      }
    });


    /*
      Tâche de surveillance des entrées GPIO bistables
      Désactivé en arrière plan
     */
    handler = new Handler();
    handler.postDelayed(runnable = new Runnable() {
      @Override
      public void run() {
        if (!mqttHelper.isConnected()) {
          textStatus.setText(getString(R.string.mqtt_nok));
          textStatus.setTextColor(Color.RED);
          paramGet = false;
          if (item_menu_irrigation != null) {
            item_menu_irrigation.setEnabled(false);
            setMenuEnabled(false);
          }
        } else {
          textStatus.setText(getString(R.string.mqtt_ok));
          textStatus.setTextColor(Color.GREEN);
          mqttHelper.publish(TOPIC_GET_PARAM, "".getBytes());
        }
        if (!paramGet) {
          mqttHelper.publish(TOPIC_GET_PARAM, "".getBytes());
        }
        if (!isClientConnected) {
          paramGet = false;
          if (item_menu_irrigation != null) {
            item_menu_irrigation.setEnabled(false);
            setMenuEnabled(false);
          }
        } else if (timout++ % 4 < 3) {
          setMenuEnabled(true);
          textStatus.setText(R.string.cnx_ok);
          textStatus.setTextColor(Color.GREEN);
          mqttHelper.publish(TOPIC_GET_GPIO, "".getBytes());
          mqttHelper.publish(TOPIC_PAC_IR_PARAM_GET, "".getBytes());
          //          Log.d("debug", "activé ? " +  cmd6.isEnabled());
        } else {
          textStatus.setText(R.string.cnx_nok);
          textStatus.setTextColor(Color.RED);
        }
        // Ne pas descendre en dessous de 1s
        handler.postDelayed(this, 1500);
      }
    }, 500);
  }

  /**
   * Mémoriser les protections des boutons contre les actions accidentelles*
   */
  public void setProtect() {
    SharedPreferences prefs;
    Unic.getInstance().setPrefs(prefs = getPreferences(Context.MODE_PRIVATE));
    SharedPreferences.Editor editor;
    Unic.getInstance().setEditor(editor = prefs.edit());
    String paramProtect = prefs.getString("sdat", null);
    String[] actions;
    if (paramProtect == null) {
      editor.putString("sdat", "1:1:1:1:1:1:1:1:1");
//    actions = "1:1:1:1:1:1:1:1".split(":");
      editor.commit();
    } else {
      actions = paramProtect.split(":");
      // Il n'y a que 6 boutons (MAX_BUTTONS=7 à cause du switch heure E/H)
      for (int i = 0; i < IconParameter.MAX_BUTTONS - 1; i++) {
        Unic.getInstance().getImageButtons()[i].setEnabled("1".equals(actions[i]));
//        Log.d("debug", "cmd" + (i+1) + ":" + Unic.getInstance().getImageButtons()[i].isEnabled());
      }
    }
  }

  public String getBrocker() {
    SharedPreferences prefs = Unic.getInstance().getPrefs();
    SharedPreferences.Editor editor;
    Unic.getInstance().setEditor(editor = prefs.edit());
    String brocker = prefs.getString("brocker", null);
    if (brocker == null) {
      editor.putString("brocker", ADDRESS);
      editor.commit();
      return ADDRESS;
    }
    return brocker;
  }

//  public void writeParam() {
//    StringBuilder dataParam = new StringBuilder();
//    int i = PARAM_START;
//
//    for (; i < MAX_PARAM - 1; i++)
//      dataParam.append(Unic.getInstance().getcParam().getTabParam()[i]).append(":");
////      dataParam.append(Unic.getInstance().getParamAutomate()[i]).append(":");
//    dataParam.append(Unic.getInstance().getcParam().getTabParam()[i]);
//    mqttHelper.publish(TOPIC_WRITE_PARAM,  dataParam.toString().getBytes());
//  }

  public void writeParam(String param) {
//    Log.d("debug", Unic.getInstance().getcParam().paramDebug());
//    Log.d("debug", param);
    mqttHelper.publish(TOPIC_WRITE_PARAM,  param.getBytes());
  }

  private void signalerDefautPompe() {
    if (!Unic.getInstance().isSignalDefautPompe()) {
      Unic.getInstance().setSignalDefautPompe(true);
      Intent intent = new Intent(MainActivity.this, WarningActivity.class);
      startActivity(intent);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_main, menu);
    MenuItem item_menu = menu.findItem(R.id.id_action_garage);
    item_menu.setTooltipText("Porte du garage");
    item_menu_reservoir = menu.findItem(R.id.id_action_reservoir);
    item_menu_irrigation = menu.findItem(R.id.id_action_irrigation);
    item_menu_cooking = menu.findItem(R.id.id_action_cooking);
    item_menu_cooking.setEnabled(false);
    MenuItem item_menu_protect = menu.findItem(R.id.id_action_protect);
    item_menu_vanne_est = menu.findItem(R.id.id_action_irrigationEst);
    item_menu_vanne_est.setEnabled(false);
    item_menu_reboot = menu.findItem(R.id.id_action_reboot);
    item_menu_logs = menu.findItem(R.id.id_action_logs);
//    itemPower = menu.findItem(R.id.id_power_off);
    itemWatchDog = menu.findItem(R.id.id_watch_dog_off);
    MenuItem item_menu_parametrer = menu.findItem(R.id.id_action_parametrer_client);
    item_menu_prog_vmc = menu.findItem(R.id.id_action_prog_vmc);
    item_menu_prog_pac =  menu.findItem(R.id.id_action_pac);
    item_menu_parametrer_client = menu.findItem(R.id.id_action_parametrer_client);
//    item_menu_DuréeArrosage.setEnabled(true);
//    itemPower.setEnabled(true);
    return true;
  }

  private void setMenuEnabled(boolean enable) {
    if (item_menu_irrigation!=null) {
      item_menu_irrigation.setEnabled(enable);
      item_menu_cooking.setEnabled(enable);
      item_menu_vanne_est.setEnabled(enable);
      item_menu_reboot.setEnabled(enable);
      item_menu_logs.setEnabled(enable);
      itemWatchDog.setEnabled(enable);
      item_menu_prog_vmc.setEnabled(enable);
      item_menu_prog_pac.setEnabled(enable);
//      item_menu_DureeArrosage.setEnabled(enable);
//      item_menu_parametrer_client.setEnabled(enable);
//      item_menu_reservoir.setEnabled(true);
    }
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    Intent intent;
//    Log.d("debug", "id menu =" + item.getItemId());
    switch (item.getItemId()) {
      case R.id.id_action_portail:
        try {
          Intent i = null;
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.CUPCAKE) {
            i = getPackageManager().getLaunchIntentForPackage("dt.cr.com.portailmqtt");
          }
          if (i == null) throw new PackageManager.NameNotFoundException();
          i.addCategory(Intent.CATEGORY_LAUNCHER);
          startActivity(i);
        } catch (PackageManager.NameNotFoundException e) {
        }
        break;
      case R.id.id_action_garage:
        try {
          Intent i = null;
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.CUPCAKE) {
            i = getPackageManager().getLaunchIntentForPackage("dt.cr.com.garage");
          }
          if (i == null) throw new PackageManager.NameNotFoundException();
          i.addCategory(Intent.CATEGORY_LAUNCHER);
          startActivity(i);
        } catch (PackageManager.NameNotFoundException e) {
        }
        break;
      case R.id.id_action_cooking:
        intent = new Intent(MainActivity.this, PowerPlageCookingActivity.class);
        startActivity(intent);
        break;
      case R.id.id_action_pac:
        intent = new Intent(MainActivity.this, PowerPlagePacActivity.class);
        startActivity(intent);
        break;
      case R.id.id_action_irrigation:
        intent = new Intent(MainActivity.this, PlageIrrigationActivity.class);
        startActivity(intent);
        break;
      case R.id.id_action_parametrer_client:
        intent = new Intent(MainActivity.this, ParameterActivity.class);
        startActivity(intent);
        break;
      case R.id.id_action_reservoir:
        mqttHelper.publish(TOPIC_CMD_REAMORCER, "".getBytes());
        item_menu_reservoir.setEnabled(false);
        break;
      case R.id.id_action_irrigationEst:
        intent = new Intent(MainActivity.this, PlageIrrigationVanneEstActivity.class);
        startActivity(intent);
        break;
      case R.id.id_action_protect:
        intent = new Intent(MainActivity.this, IconParameter.class);
        startActivity(intent);
        break;
      case R.id.id_action_exit:
        System.exit(0);
        return true;
      case R.id.id_action_reboot:
        mqttHelper.publish(TOPIC_REBOOT, "".getBytes());
        break;
      case R.id.id_a_propos:
        intent = new Intent(MainActivity.this, AProposActivity.class);
        startActivity(intent);
        break;
      case R.id.id_action_logs:
        intent = new Intent(MainActivity.this, LogsActivity.class);
        startActivity(intent);
        break;
//      case R.id.id_power_off:
//        mqttHelper.publish(SUB_GPIO0_ACTION,
//              powerOn ? ON.getBytes() : OFF.getBytes());
//        if (powerOn) {
//          itemPower.setTitle(R.string.action_power_off);
//        }
//        else {
//          itemPower.setTitle(R.string.action_power_on);
//        }
//        break;
      case R.id.id_watch_dog_off:
        mqttHelper.publish(TOPIC_WATCH_DOG_OFF, "".getBytes());
        break;

      case R.id.id_action_prog_vmc:
        intent = new Intent(MainActivity.this, VmcActivity.class);
        startActivity(intent);
    }
    return super.onOptionsItemSelected(item);
  }

  public void connectComplete(MqttAndroidClient mqttAndroidClient) {
    try {
      //Successful connection requires all client subscription relationships to be uploaded
      mqttAndroidClient.subscribe(TOPIC_READ_VERSION, 0);
      mqttAndroidClient.subscribe(TOPIC_READ_LOGS, 0);
      mqttAndroidClient.subscribe(TOPIC_PARAM, 0);
      mqttAndroidClient.subscribe(TOPIC_DLY_PARAM, 0);
      mqttAndroidClient.subscribe(TOPIC_GLOBAL_SCHED, 0);
      mqttAndroidClient.subscribe(TOPIC_GPIO, 0);
      mqttAndroidClient.subscribe(TOPIC_DEFAUT_SUPRESSEUR, 0);
//      mqttAndroidClient.subscribe(PUB_POWER_STATUS, 0);
      mqttAndroidClient.subscribe(TOPIC_PAC_IR_PARAM_PUB,  0);
      mqttAndroidClient.subscribe(TOPIC_PAC_IR_VERSION, 0);
//      mqttAndroidClient.subscribe(TOPIC_VMC_STATUS, 0);
      mqttAndroidClient.subscribe(TOPIC_CIRCUIT2_STATUS, 0);

    }
    catch (MqttException e) {
//      Log.d(TAG, "subscribe ex" );
    }
  }

  public void connectionLost(Throwable cause) {
    // mqttConnected = false;
  }

  public void messageArrived(String topic, MqttMessage message) {
//    Log.d("debug", "" +cmd6.isEnabled());
    String reponse = message.toString();
//    Log.d("debug", topic + ":" + reponse);
    isClientConnected = true;
    switch (topic) {
      case PUB_POWER_STATUS:
        // L'alimentation est branchée sur le contact repos
        boolean powerOn = OFF.equals(reponse);
        // Faire les maj que sir changement d'état
        itemPower.setTitle(powerOn ?
                  R.string.action_power_off : R.string.action_power_on);
        if (!powerOn) {
          textStatus.setText(R.string.command_power_off);
        }
        return;
      case TOPIC_READ_VERSION:
        String version = reponse.trim();
        Unic.getInstance().getaProposActivity().publishClientVersion(version);
        return;
      case TOPIC_PAC_IR_VERSION:
        String irVersion = reponse.trim();
        Unic.getInstance().getaProposActivity().publishIrVersion(irVersion);
        return;

      case TOPIC_PARAM:
//        isClientConnected = true;
        paramGet = true;
        timout = 0;
        Unic.getInstance().getcParam().setParam(reponse.trim());
        //        String debug = Unic.getInstance().getcParam().paramDebug();
        //        Log.d("debug", debug);
        // Une fois les paramètres obtenus, acquerir les paramètres globaux ScheduledParam
        mqttGetGlobalScheduledParam();
        return;

      case TOPIC_DLY_PARAM:
        Unic.getInstance().getParameterActivity().setDlyParam(reponse);
        return;

      case TOPIC_GLOBAL_SCHED:
        Unic.getInstance().setGlobalSchedParam(reponse);
        break;
      case TOPIC_PAC_IR_PARAM_PUB:
        Unic.getInstance().setIrParam(reponse);
        return;

      // Met à jour les bouton à l'aide des info reçu du client
      // Dans  réponse une chaine a;b;c;d;e;f;g;h;i;j
      // a..j = "0" ou "1" suivant l'état des ports GPIO
      //        en général "0" correspond à une commande active (commande par niveao 0)
      // Avec :
      //  a GPIO_ARROSAGE
      //  b GPIO_IRRIGATION
      //  c GPIO_FOUR
      //  d GPIO_EV_EST
      //  e VMC
      //  f GPIO_PAC

      case TOPIC_GPIO:
        timout = 0;
//        isClientConnected = true;
//        Log.d("debug", reponse);
        String[] gpioPorts = reponse.split(";");
        if ("1".equals(gpioPorts[GPIO_ARROSAGE])) {
          cmd1.setBackgroundResource(R.mipmap.ic_arrosage_off);
          if (arrosageEncours) {
            arrosageEncours = false;
          }
        } else if ("0".equals(gpioPorts[GPIO_ARROSAGE])) {
          arrosageEncours = true;
          cmd1.setBackgroundResource(R.mipmap.ic_arrosage_on);
        }
        else  if ("2".equals(gpioPorts[GPIO_ARROSAGE])) {
          arrosageEncours = true;
          cmd1.setBackgroundResource(R.mipmap.ic_arrosage_perma);
        }

        if ("1".equals(gpioPorts[GPIO_IRRIGATION])) {
          irrigationEnCours = false;
          cmd2.setBackgroundResource(R.mipmap.ic_reservoir_off);
        } else {
          irrigationEnCours = true;
          cmd2.setBackgroundResource(R.mipmap.ic_reservoir_on);
        }

        if ("1".equals((gpioPorts[GPIO_FOUR]))) {
          status_four = false;
          cmd3.setBackgroundResource(R.mipmap.ic_four_off);
        } else {
          status_four = true;
          cmd3.setBackgroundResource(R.mipmap.ic_four_on);
        }

        int vmcStatus = Integer.parseInt(gpioPorts[GPIO_VMC]);
//        if (vmcFast)
//          return;
//
//        if (cmdVmc != cmdVmc_N1) {
//          this.cmdVmc = cmdVmc + 1;
//          cmdVmc_N1 = cmdVmc + 1;
//        }
//        Log.d("debug", "cmdVmc " +  cmdVmc);
//        Log.d("debug", "vmcStatus " +  vmcStatus);
        switch (vmcStatus) {
          case VMC_STOP :
            cmd4.setBackgroundResource(R.mipmap.ic_vmc_off);
            setCmdVmc(0);
            break;
          case VMC_ON :
            cmd4.setBackgroundResource(R.mipmap.ic_vmc_on);
            setCmdVmc(3);
            break;
          case VMC_ON_FAST :
            cmd4.setBackgroundResource(R.mipmap.ic_vmc_fast);
            setCmdVmc(2);
            break;
          case VMC_PROG_OFF :
            cmd4.setBackgroundResource(R.mipmap.ic_vmc_prog);
            setCmdVmc(1);
            break;
          case VMC_PROG_ON :
            cmd4.setBackgroundResource(R.mipmap.ic_vmc_prog_on);
            setCmdVmc(1);
            break;
          case VMC_PROG_ON_FAST :
            cmd4.setBackgroundResource(R.mipmap.ic_vmc_prog_fast);
            setCmdVmc(1);
            break;

        }

        if ("1".equals(gpioPorts[GPIO_EV_EST])) {
          cmd5.setBackgroundResource(R.mipmap.ic_arrosage_est_off);
          evEstOn = false;
        } else {
          cmd5.setBackgroundResource(R.mipmap.ic_arrosage_est_on_prog);
          evEstOn = true;
        }
        if ("1".equals(gpioPorts[GPIO_PAC])) {
          statusCmd_PAC = 1;
          arretEncours = false;
          cmd6.setBackgroundResource(R.mipmap.ic_pac_on);
        }
        else
          if ("2".equals(gpioPorts[GPIO_PAC])) {
            cmd6.setBackgroundResource(R.mipmap.ic_pac_onstop);
            arretEncours = true;
          }
        else {
            statusCmd_PAC = 0;
            arretEncours = false;
            cmd6.setBackgroundResource(R.mipmap.ic_pac_off);
        }
        return;

      case TOPIC_DEFAUT_SUPRESSEUR:
//        Log.d("debug", "TOPIC_DEFAUT_SUPRESSEUR=" + reponse);
        if ("off".equals(reponse))
          item_menu_reservoir.setEnabled(false);
        if ("on".equals(reponse))
          item_menu_reservoir.setEnabled(true);
        if ("on2".equals(reponse))
            signalerDefautPompe();
        return;

      case TOPIC_READ_LOGS:
//        String msg = reponse;
//        if (!"#####".equals(msg)) {
//          logBuffer.append(msg);
//          return;
//        }
//        String[] tReponse = logBuffer.toString().split("\n");
//        ArrayList<String> al = new ArrayList<>();
//        for (String line : tReponse)
//          al.add(line+"\n");
//        Collections.reverse(al);
//        reponse = al.toString().substring(1);
//        reponse = reponse.replace("\n", "&lt;br&gt;");
//        reponse = reponse.replace(",", "");
//        LogsActivity.editTextLogs.setText((Html.fromHtml(Html.fromHtml(reponse).toString())));
//        logBuffer.delete(0, logBuffer.length()-1);
//        return;

        if (!"#####".equals(reponse)) {
          logBuffer.append(reponse);
          return;
        }
        String[] tReponse = logBuffer.toString().split("\n");
        ArrayList<String> al = new ArrayList<>();
        for (String line : tReponse)
          al.add(line+"\n");
        Collections.reverse(al);
        String msg = al.toString().substring(1);
        msg = msg.replace("\n", "&lt;br&gt;");
        msg = msg.replace(",", "");
        LogsActivity.editTextLogs.setText((Html.fromHtml(Html.fromHtml(msg).toString())));
        logBuffer.delete(0, logBuffer.length()-1);
      case TOPIC_CIRCUIT2_STATUS :
        Unic.getInstance().setCircuit2Status(reponse);
        return;
    }
  }

  private void setCmdVmc(int i) {
    if (i != cmdVmc_N1) {
      this.cmdVmc = i + 1;
      cmdVmc_N1 = i + 1;
    }
  }

  public void deliveryComplete(IMqttDeliveryToken token) {
  }

  public void connected() {
  }

  public void onFailure(IMqttToken asyncActionToken) {
  }

  public void readVersion() {
    mqttHelper.publish(TOPIC_GET_VERSION, "".getBytes());
    mqttHelper.publish(TOPIC_PAC_IR_VERSION_GET, "".getBytes());
  }

  public void readLogs() {
    mqttHelper.publish(TOPIC_LOGS_GET, "".getBytes());
  }

  public void clearLogs() {
    mqttHelper.publish(TOPIC_CLEAR_LOGS, "".getBytes());
  }

  public void setSummerTime(boolean b) {
//    mqttHelper.publish(TOPIC_TIME_SUMMER_OFF, (b ? "2" : "1").getBytes());
  }
  public void setDisableLog(String status) {
//    mqttHelper.publish(TOPIC_SET_LOG_STATUS, status.getBytes());
  }

  public void getIOTLogStatus() {
//    mqttHelper.publish(TOPIC_GET_LOG_STATUS, "". getBytes());
  }

  public void writeIrParam(String param) {
    mqttHelper.publish(TOPIC_PAC_IR_PARAM_SET, param.getBytes());
    //mqttHelper.publish(TOPIC_PAC_IR_PARAM_APPLY, "".getBytes());
  }

  public void setPacActive(boolean b) {
    if (b)
      mqttHelper.publish(TOPIC_PAC_IR_ON, "".getBytes());
    else
      mqttHelper.publish(TOPIC_PAC_IR_OFF, "".getBytes());
  }

  public void setTemp(String temp) {
    mqttHelper.publish(TOPIC_PAC_IR_TEMP, temp.getBytes());
  }

  public void setMode(String mode) {
    mqttHelper.publish(TOPIC_PAC_IR_MODE, mode.getBytes());
  }

  public void setFan(String fan) {
    mqttHelper.publish(TOPIC_PAC_IR_FAN, fan.getBytes());
  }

  public void setVanne(String vanne) {
    mqttHelper.publish(TOPIC_PAC_IR_VANNE, vanne.getBytes());
  }

  public void getDlyParam() {
    mqttHelper.publish(TOPIC_GET_DLY_PARAM, "".getBytes());
  }

  public void writeDlyParam(String dlyParam) {
    mqttHelper.publish(TOPIC_WRITE_DLY_PARAM, dlyParam.getBytes());
  }
  public void mqttGetGlobalScheduledParam() {
    mqttHelper.publish(TOPIC_GET_GLOBAL_SCHED, "".getBytes());
  }
  public void writeScheduledParam(String scheduledParam) {
//    Log.d("debug", scheduledParam);
    mqttHelper.publish(TOPIC_WRITE_GLOBAL_SCHED, scheduledParam.getBytes());
  }

  public void setVanneCircuit2(boolean isChecked) {
    mqttHelper.publish(TOPIC_CIRCUIT2_ACTION, isChecked ? "on".getBytes() : "off".getBytes());
  }

  public void connectionLost() {
//      Log.d("debug", "connectionLost");
  }

  public void deliveryComplete() {
//    Log.d("debug", "deliveryComplete");
  }
}