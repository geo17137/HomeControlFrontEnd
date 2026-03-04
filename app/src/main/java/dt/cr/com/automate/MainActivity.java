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

import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.io.FileUtils;

/*
public class Secret {
    static final String userName = "xxxxx";
    static final String password = "xxxxx";

    static final String LOCAL_ADRESS = "http://xxx.xxx.xxx.xxx:yyyy";
    static final String ADDRESS = "http://zzzzz:yyyy";

    static final CharSequence SSID1 = "XXXXX";
    static final CharSequence SSID2 = "XXXXX";
    static final CharSequence SSID3 = "XXXXX";
    static final CharSequence SSID4 = "XXXXX";
}
 */
public class MainActivity extends AppCompatActivity {
  final String TARGET_OFF = "";
  final String LOCAL_ADRESS = Secret.LOCAL_ADRESS;
  final String ADDRESS = Secret.ADDRESS;

  static final int MAX_PARAM = Unic.MAX_PARAM;
  // Indices des champs définis dans Unic
  //  public static final int CUISINE = 0;
  //  public static final int IRRIGTION_POTAGER = 1;
  //  public static final int IRRIGTION_FACADE_SUD = 2;
  //  public static final int PAC = 3;
  //  public static final int VMC = 4;

  // Se référer au client mqtt (projet platformio Esp32_HomeCtrl)
  // Numéro des ports GPIO relais
  private final int GPIO_ARROSAGE = 0;
  private final int GPIO_IRRIGATION = 1;
  private final int GPIO_FOUR = 2;
  private final int GPIO_EV_EST = 3;
  private final int GPIO_VMC = 4;
  private final int GPIO_PAC = 5;

  private long lastTouchTime = 0;
  private long currentTouchTime = 0;

  private final String TAG = "mqtt";

  private ImageButton cmd1;
  private ImageButton cmd2;
  private ImageButton cmd3;
  private ImageButton cmd4;
  private ImageButton cmd5;
  private ImageButton cmd6;

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


  //-----------------------
  //    Modes de fonct. VMC
  //-----------------------
  private static final int VMC_STOP = 0;
  private static final int VMC_PROG_OFF = 1;
  private static final int VMC_PROG_ON = 2;
  private static final int VMC_PROG_ON_FAST = 3;
  private static final int VMC_ON = 5;
  private static final int VMC_ON_FAST = 4;

// Résultat des lecture GPIO 5..7

  private int statusCmd_PAC;
  //  private int networkTest;
  private int cmdVmc;
  private boolean init;
  private boolean evEstOn;
  private boolean status_four;

  private boolean arrosageEncours;
  private boolean irrigationEnCours;
  private boolean arretEncours;
  private boolean isClientConnected;
  private boolean paramGet;
  private Runnable runnable;
  private Handler handler;
  private MqttHandler mqttHandler;
  private StringBuilder logBuffer;

  private final String PREFIX = TARGET_OFF;
  //----------- Publications -----------
  private final String TOPIC_GET_PARAM = PREFIX + "homecontrol/param_get";
  private final String TOPIC_WRITE_PARAM = PREFIX + "homecontrol/write_param";
  private final String TOPIC_GET_DLY_PARAM = PREFIX + "homecontrol/get_dly_param";
  private final String TOPIC_WRITE_DLY_PARAM = PREFIX + "homecontrol/write_dly_param";
  private final String TOPIC_WRITE_GLOBAL_SCHED = PREFIX + "homecontrol/global_sched_write";
  private final String TOPIC_GET_GLOBAL_SCHED = PREFIX + "homecontrol/global_sched_get";
  private final String TOPIC_GET_GPIO = PREFIX + "homecontrol/get_gpio";
  private final String TOPIC_CMD_ARROSAGE = PREFIX + "homecontrol/arrosage";
  private final String TOPIC_CMD_IRRIGATION = PREFIX + "homecontrol/irrigation";
  private final String TOPIC_CMD_CUISINE = PREFIX + "homecontrol/cuisine";
  private final String TOPIC_CMD_VMC = PREFIX + "homecontrol/vmc";
  private final String TOPIC_CMD_VANNE_EST = PREFIX + "homecontrol/vanne_est";
  private final String TOPIC_CMD_PAC = PREFIX + "homecontrol/pac";
  private final String TOPIC_LOGS_GET = PREFIX + "homecontrol/logs_get";
  private final String TOPIC_CLEAR_LOGS = PREFIX + "homecontrol/clear_logs";
  private final String TOPIC_REBOOT = PREFIX + "homecontrol/reboot";
  private final String TOPIC_GET_VERSION = PREFIX + "homecontrol/versions_get";
  private final String TOPIC_WATCH_DOG_OFF = PREFIX + "homecontrol/watch_dog_off";
  private final String TOPIC_CMD_REAMORCER = PREFIX + "homecontrol/rearmorcer";
  private final String TOPIC_APP_CONNECT = "homecontrol/app_connect";
  private final String VMC_BOARD_ACTION = PREFIX + "vmc_board/action";

  //  private static final String SUB_GPIO0_ACTION  = "board1/action";
  private final String TOPIC_PAC_IR_PARAM_SET = PREFIX + "mitsubishi/param/set";
  private final String TOPIC_PAC_IR_PARAM_GET = PREFIX + "mitsubishi/param/get";
  //  private static final String TOPIC_PAC_IR_PARAM_APPLY = "mitsubishi/param/apply";
  private final String TOPIC_PAC_IR_ON = PREFIX + "mitsubishi/param/on";
  private final String TOPIC_PAC_IR_OFF = PREFIX + "mitsubishi/param/off";
  private final String TOPIC_PAC_IR_TEMP = PREFIX + "mitsubishi/param/temp";
  private final String TOPIC_PAC_IR_MODE = PREFIX + "mitsubishi/param/mode";
  private final String TOPIC_PAC_IR_FAN = PREFIX + "mitsubishi/param/fan";
  private final String TOPIC_PAC_IR_VANNE = PREFIX + "mitsubishi/param/vanne";
  private final String TOPIC_PAC_IR_VERSION_GET = PREFIX + "mitsubishi/get_version";
  private final String TOPIC_CIRCUIT2_ACTION = "circuit2/action";

  //--------------------------------- Abonnements --------------------------------------

  private final String TOPIC_READ_VERSION = PREFIX + "homecontrol/readVersion";
  private final String TOPIC_READ_LOGS = PREFIX + "homecontrol/readLogs";
  private final String TOPIC_PARAM = PREFIX + "homecontrol/param";
  private final String TOPIC_DLY_PARAM = PREFIX + "homecontrol/dly_param";
  private final String TOPIC_GLOBAL_SCHED = PREFIX + "homecontrol/global_sched";
  private final String TOPIC_GPIO = PREFIX + "homecontrol/gpio";
  private final String TOPIC_DEFAUT_SUPRESSEUR = PREFIX + "homecontrol/default_surpressor";
  private final String TOPIC_CIRCUIT2_STATUS = "circuit2/status";


  private final String PUB_POWER_STATUS = PREFIX + "board1/status";
  private final String TOPIC_PAC_IR_PARAM_PUB = PREFIX + "mitsubishi/param/pub";
  private final String TOPIC_PAC_IR_VERSION = PREFIX + "mitsubishi/version";

  private final String TOPIC_VMC_STATUS = PREFIX + "vmc_board/status";

  // Commandes
  private static final String ON = "on";
  private static final String OFF = "off";
  private final String PUBLISH_STATE_ON = "pub_on";
  private final String PUBLISH_STATE_OFF = "pub_off";

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

  public boolean getWifiInfo() {
    // Attention il faut que le GPS soit activé !!!
    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    String ssid = wm.getConnectionInfo().getSSID();
    return ssid.contains(Secret.SSID1) || ssid.contains(Secret.SSID2) || ssid.contains(Secret.SSID3) || ssid.contains(Secret.SSID4);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    boolean permissionOK = true;
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
    if (getWifiInfo()) {
      serverMqtt = LOCAL_ADRESS;
    } else {
      serverMqtt = getBroker();
    }
    Unic.getInstance().setBrockerAdr(serverMqtt);
    mqttHandler = new MqttHandler(this);
    mqttHandler.connect();

    cmd1.setOnClickListener(View -> {
      if (arrosageEncours) {
        mqttHandler.publish(TOPIC_CMD_ARROSAGE, "0".getBytes());
        arrosageEncours = false;
        return;
      }
      lastTouchTime = currentTouchTime;
      currentTouchTime = System.currentTimeMillis();
      if (currentTouchTime - lastTouchTime < 450) {
        lastTouchTime = 0;
        currentTouchTime = 0;
        if (Unic.getInstance().isArrosagePermanent()) {
          mqttHandler.publish(TOPIC_CMD_ARROSAGE, "2".getBytes());
        } else {
          mqttHandler.publish(TOPIC_CMD_ARROSAGE, "1".getBytes());
        }
      }
    });

    cmd2.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (irrigationEnCours) {
          mqttHandler.publish(TOPIC_CMD_IRRIGATION, "0".getBytes());
          irrigationEnCours = false;
          return;
        }
        lastTouchTime = currentTouchTime;
        currentTouchTime = System.currentTimeMillis();
        if (currentTouchTime - lastTouchTime < 450) {
          lastTouchTime = 0;
          currentTouchTime = 0;
          mqttHandler.publish(TOPIC_CMD_IRRIGATION, "1".getBytes());
        }
      }
    });

    cmd3.setOnClickListener(view -> {
      if (status_four) {
        mqttHandler.publish(TOPIC_CMD_CUISINE, "0".getBytes());
      } else {
        mqttHandler.publish(TOPIC_CMD_CUISINE, "1".getBytes());
      }

    });

    // Commande VMC
    cmd4.setOnClickListener(view -> {
      switch (cmdVmc) {
        case 0:
          // Mode off (pas de vcm active, même ne mode programmée)
          mqttHandler.publish(TOPIC_CMD_VMC, "0".getBytes());
          break;
        case 1:
          // Mode programmé, lent ou rapide suivant programme
          mqttHandler.publish(TOPIC_CMD_VMC, "1".getBytes());
          break;
        case 2:
          // Mode forcé rapide (programmation off)
          // Carte déportée pilotée par l'automate
          mqttHandler.publish(TOPIC_CMD_VMC, "2".getBytes());
          break;
        case 3:
          // Mode forcé lent (programmation off)
          // Carte déportée pilotée par l'automate
          mqttHandler.publish(TOPIC_CMD_VMC, "3".getBytes());
      }
    });

    cmd5.setOnClickListener(view -> {
      if (evEstOn) {
        mqttHandler.publish(TOPIC_CMD_VANNE_EST, "0".getBytes());
        cmd5.setBackgroundResource(R.mipmap.ic_arrosage_est_off);
      } else {
        mqttHandler.publish(TOPIC_CMD_VANNE_EST, "1".getBytes());
        cmd5.setBackgroundResource(R.mipmap.ic_arrosage_est_on_prog);
      }
    });

    // PAC: envoi de commande de type bascule
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
            mqttHandler.publish(TOPIC_CMD_PAC, "1".getBytes());
          } else {
            mqttHandler.publish(TOPIC_CMD_PAC, "0".getBytes());
          }
        } else {
          mqttHandler.publish(TOPIC_CMD_PAC, "0".getBytes());
        }
      }
    });

    /*
      Tâche de surveillance des entrées GPIO bistables
      Désactivé en arrière plan
     */

    handler = new Handler();
    handler.postDelayed(runnable = new Runnable() {
      private boolean first = true;

      @Override
      public void run() {
        if (!mqttHandler.isConnected()) {
          textStatus.setText(R.string.mqtt_nok);
          textStatus.setTextColor(Color.RED);
          setMenuEnabled(false);
          Log.d("isConnected","" + "1");
        } else {
          textStatus.setText(R.string.mqtt_ok);
          textStatus.setTextColor(Color.GREEN);
          if (isClientConnected) {
            textStatus.setText(R.string.cnx_ok);
            textStatus.setTextColor(Color.GREEN);
            mqttHandler.publish(TOPIC_APP_CONNECT, "1".getBytes());
            mqttHandler.publish(TOPIC_PAC_IR_PARAM_GET, "".getBytes());
            mqttHandler.publish(TOPIC_GET_GPIO, "".getBytes());
            // Log.d("isConnected","" + "3");
            setMenuEnabled(true);
          } else {
            if (first) {
              textStatus.setText(R.string.cnx_nok);
              textStatus.setTextColor(Color.RED);
              setMenuEnabled(false);
              first = false;
              // mqttHandler.publish(TOPIC_GET_PARAM, "".getBytes());
            }
            // Log.d("isConnected","" + "2");
            handler.postDelayed(this, 2000);
          }
        }
      }
    }, 1000);
    init = true;
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
      editor.apply();
    } else {
      actions = paramProtect.split(":");
      // Il n'y a que 6 boutons (MAX_BUTTONS=7 à cause du switch heure E/H)
      for (int i = 0; i < IconParameter.MAX_BUTTONS - 1; i++) {
        Unic.getInstance().getImageButtons()[i].setEnabled("1".equals(actions[i]));
//        Log.d("debug", "cmd" + (i+1) + ":" + Unic.getInstance().getImageButtons()[i].isEnabled());
      }
    }
  }

  public String getBroker() {
    SharedPreferences prefs = Unic.getInstance().getPrefs();
    SharedPreferences.Editor editor;
    Unic.getInstance().setEditor(editor = prefs.edit());
    String broker = prefs.getString("broker", null);
    if (broker == null) {
      editor.putString("broker", ADDRESS);
      editor.apply();
      return ADDRESS;
    }
    return broker;
  }

  public void writeParam(String param) {
    mqttHandler.publish(TOPIC_WRITE_PARAM, param.getBytes());
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
    item_menu_prog_pac = menu.findItem(R.id.id_action_pac);
    item_menu_parametrer_client = menu.findItem(R.id.id_action_parametrer_client);
//    item_menu_DuréeArrosage.setEnabled(true);
//    itemPower.setEnabled(true);
    return true;
  }

  private void setMenuEnabled(boolean enable) {
    if (item_menu_irrigation != null) {
      item_menu_irrigation.setEnabled(enable);
      item_menu_cooking.setEnabled(enable);
      item_menu_vanne_est.setEnabled(enable);
      item_menu_reboot.setEnabled(enable);
      item_menu_logs.setEnabled(enable);
      itemWatchDog.setEnabled(enable);
      item_menu_prog_vmc.setEnabled(enable);
      item_menu_prog_pac.setEnabled(enable);
      item_menu_parametrer_client.setEnabled(enable);
//      item_menu_DureeArrosage.setEnabled(enable);
//      item_menu_parametrer_client.setEnabled(enable);
//      item_menu_reservoir.setEnabled(true);
    }
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    Intent intent;
    int id = item.getItemId();
    if (id == R.id.id_action_portail) {
      try {
        Intent i;
        i = getPackageManager().getLaunchIntentForPackage("dt.cr.com.portailmqtt");
        if (i == null) throw new PackageManager.NameNotFoundException();
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        startActivity(i);
      } catch (PackageManager.NameNotFoundException e) {
      }
    } else if (id == R.id.id_action_garage) {
      try {
        Intent i;
        i = getPackageManager().getLaunchIntentForPackage("dt.cr.com.garage");
        if (i == null) throw new PackageManager.NameNotFoundException();
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        startActivity(i);
      } catch (PackageManager.NameNotFoundException e) {
      }
    } else if (id == R.id.id_action_cooking) {
      intent = new Intent(MainActivity.this, PowerPlageCookingActivity.class);
      startActivity(intent);
    } else if (id == R.id.id_action_pac) {
      mqttHandler.publish(TOPIC_PAC_IR_PARAM_GET, "".getBytes());
      intent = new Intent(MainActivity.this, PowerPlagePacActivity.class);
      startActivity(intent);
    } else if (id == R.id.id_action_irrigation) {
      intent = new Intent(MainActivity.this, PlageIrrigationActivity.class);
      startActivity(intent);
    } else if (id == R.id.id_action_parametrer_client) {
      intent = new Intent(MainActivity.this, ParameterActivity.class);
      startActivity(intent);
    } else if (id == R.id.id_action_reservoir) {
      mqttHandler.publish(TOPIC_CMD_REAMORCER, "".getBytes());
      item_menu_reservoir.setEnabled(false);
    } else if (id == R.id.id_action_irrigationEst) {
      intent = new Intent(MainActivity.this, PlageIrrigationVanneEstActivity.class);
      startActivity(intent);
    } else if (id == R.id.id_action_protect) {
      intent = new Intent(MainActivity.this, IconParameter.class);
      startActivity(intent);
    } else if (id == R.id.id_action_exit) {
      mqttHandler.publish(TOPIC_APP_CONNECT, "0".getBytes());
      System.exit(0);
      return true;
    } else if (id == R.id.id_action_reboot) {
      mqttHandler.publish(TOPIC_REBOOT, "".getBytes());
    } else if (id == R.id.id_a_propos) {
      intent = new Intent(MainActivity.this, AProposActivity.class);
      startActivity(intent);
    } else if (id == R.id.id_action_logs) {
      intent = new Intent(MainActivity.this, LogsActivity.class);
      startActivity(intent);
    } else if (id == R.id.id_watch_dog_off) {
      mqttHandler.publish(TOPIC_WATCH_DOG_OFF, "".getBytes());
    } else if (id == R.id.id_action_prog_vmc) {
      intent = new Intent(MainActivity.this, VmcActivity.class);
      startActivity(intent);
    }
    return super.onOptionsItemSelected(item);
  }

  public void connectComplete() {
      //Successful connection requires all client subscription relationships to be uploaded
      mqttHandler.subscribe(TOPIC_READ_VERSION, 0);
      mqttHandler.subscribe(TOPIC_READ_LOGS, 0);
      mqttHandler.subscribe(TOPIC_PARAM, 0);
      mqttHandler.subscribe(TOPIC_DLY_PARAM, 0);
      mqttHandler.subscribe(TOPIC_GLOBAL_SCHED, 0);
      mqttHandler.subscribe(TOPIC_GPIO, 0);
      mqttHandler.subscribe(TOPIC_DEFAUT_SUPRESSEUR, 0);
//    mqttHandler.subscribe(PUB_POWER_STATUS, 0);
      mqttHandler.subscribe(TOPIC_PAC_IR_PARAM_PUB, 0);
      mqttHandler.subscribe(TOPIC_PAC_IR_VERSION, 0);
//    mqttHandler.subscribe(TOPIC_VMC_STATUS, 0);
      mqttHandler.subscribe(TOPIC_CIRCUIT2_STATUS, 0);

      mqttHandler.publish(TOPIC_GET_PARAM, "".getBytes());
//    mqttHandler.publish(TOPIC_GET_GPIO, "".getBytes());
//    mqttHandler.publish(TOPIC_PAC_IR_PARAM_GET, "".getBytes());
//    mqttHandler.publish(TOPIC_APP_CONNECT, "1".getBytes());
  }
  public void messageArrived(String topic, String reponse) {
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
        isClientConnected = true;
        Unic.getInstance().getcParam().setParam(reponse.trim());
        // String debug = Unic.getInstance().getcParam().paramDebug();
        //  Log.d("debug", debug);
        // Une fois les paramètres obtenus, acquerir les paramètres globaux ScheduledParam
        if (!paramGet) {
          mqttGetGlobalScheduledParam();
          paramGet = true;
        }
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
        String[] gpioPorts = reponse.split(";");
        if ("1".equals(gpioPorts[GPIO_ARROSAGE])) {
          cmd1.setBackgroundResource(R.mipmap.ic_arrosage_off);
          if (arrosageEncours) {
            arrosageEncours = false;
          }
        } else if ("0".equals(gpioPorts[GPIO_ARROSAGE])) {
          arrosageEncours = true;
          cmd1.setBackgroundResource(R.mipmap.ic_arrosage_on);
        } else if ("2".equals(gpioPorts[GPIO_ARROSAGE])) {
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
        switch (vmcStatus) {
          case VMC_STOP:
            cmd4.setBackgroundResource(R.mipmap.ic_vmc_off);
            setCmdVmc(0);
            break;
          case VMC_ON:
            cmd4.setBackgroundResource(R.mipmap.ic_vmc_on);
            setCmdVmc(3);
            break;
          case VMC_ON_FAST:
            cmd4.setBackgroundResource(R.mipmap.ic_vmc_fast);
            setCmdVmc(2);
            break;
          case VMC_PROG_OFF:
            cmd4.setBackgroundResource(R.mipmap.ic_vmc_prog);
            setCmdVmc(1);
            break;
          case VMC_PROG_ON:
            cmd4.setBackgroundResource(R.mipmap.ic_vmc_prog_on);
            setCmdVmc(1);
            break;
          case VMC_PROG_ON_FAST:
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
        } else if ("2".equals(gpioPorts[GPIO_PAC])) {
          cmd6.setBackgroundResource(R.mipmap.ic_pac_onstop);
          arretEncours = true;
        } else {
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
        if (!"#####".equals(reponse)) {
          logBuffer.append(reponse);
          return;
        }
        String[] tReponse = logBuffer.toString().split("\n");
        ArrayList<String> al = new ArrayList<>();
        for (String line : tReponse)
          al.add(line + "\n");
        Collections.reverse(al);
        String msg = al.toString().substring(1);
        msg = msg.replace("\n", "&lt;br&gt;");
        msg = msg.replace(",", "");
        LogsActivity.editTextLogs.setText((Html.fromHtml(Html.fromHtml(msg).toString())));
        logBuffer.delete(0, logBuffer.length() - 1);
        break;
      case TOPIC_CIRCUIT2_STATUS:
        Unic.getInstance().setCircuit2Status(reponse);
        break;
    }
  }

  private void setCmdVmc(int i) {
    this.cmdVmc = (i + 1) % 4;
  }

  public void readVersion() {
    mqttHandler.publish(TOPIC_GET_VERSION, "".getBytes());
    mqttHandler.publish(TOPIC_PAC_IR_VERSION_GET, "".getBytes());
  }

  public void readLogs() {
    mqttHandler.publish(TOPIC_LOGS_GET, "".getBytes());
  }

  public void clearLogs() {
    mqttHandler.publish(TOPIC_CLEAR_LOGS, "".getBytes());
  }

  public void writeIrParam(String param) {
    mqttHandler.publish(TOPIC_PAC_IR_PARAM_SET, param.getBytes());
  }

  public void setPacActive(boolean b) {
    if (b)
      mqttHandler.publish(TOPIC_PAC_IR_ON, "".getBytes());
    else
      mqttHandler.publish(TOPIC_PAC_IR_OFF, "".getBytes());
  }

  public void setTemp(String temp) {
    mqttHandler.publish(TOPIC_PAC_IR_TEMP, temp.getBytes());
  }

  public void setMode(String mode) {
    mqttHandler.publish(TOPIC_PAC_IR_MODE, mode.getBytes());
  }

  public void setFan(String fan) {
    mqttHandler.publish(TOPIC_PAC_IR_FAN, fan.getBytes());
  }

  public void setVanne(String vanne) {
    mqttHandler.publish(TOPIC_PAC_IR_VANNE, vanne.getBytes());
  }

  public void getDlyParam() {
    mqttHandler.publish(TOPIC_GET_DLY_PARAM, "".getBytes());
  }

  public void writeDlyParam(String dlyParam) {
    mqttHandler.publish(TOPIC_WRITE_DLY_PARAM, dlyParam.getBytes());
  }

  public void mqttGetGlobalScheduledParam() {
    mqttHandler.publish(TOPIC_GET_GLOBAL_SCHED, "".getBytes());
  }

  public void writeScheduledParam(String scheduledParam) {
//    Log.d("debug", scheduledParam);
    mqttHandler.publish(TOPIC_WRITE_GLOBAL_SCHED, scheduledParam.getBytes());
  }

  public void setVanneCircuit2(boolean isChecked) {
    mqttHandler.publish(TOPIC_CIRCUIT2_ACTION, isChecked ? "on".getBytes() : "off".getBytes());
  }
}