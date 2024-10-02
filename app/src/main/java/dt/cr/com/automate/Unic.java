package dt.cr.com.automate;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.widget.ImageButton;

class Unic {

  @SuppressLint("StaticFieldLeak")
  final String version = "2024.9.24";

  private static final Unic ourInstance = new Unic();

  // Position des entités dans la structure des paramètres
  public static final int CUISINE = 0;
  public static final int IRRIGTION_POTAGER = 1;
  public static final int IRRIGTION_FACADE_SUD = 2;
  public static final int PAC = 3;
  public static final int VMC = 4;

  // Paramétrage des tables de paramètres
  static final int NB_PLAGES = 4;
  static final int NB_ITEMS_PLAGE = 5;
  static final int NB_DISPOSITIFS = 5;

  static final int PARAM_START = 0;
  static final int MAX_PARAM = NB_ITEMS_PLAGE * NB_PLAGES * NB_DISPOSITIFS;

  static final int MAX_RADIO_BUTTONS = 3;
  //  static final int MAX_BUTTONS = 7;
//
  private MainActivity mainActivity;
  private ImageButton[] imageButtons;

  private String[] stabGlobalSchedParam;
  private boolean signalDefautPompe;
  private PlageIrrigationActivity plageIrrigationActivity;

  public String getCircuit2Status() {
    return circuit2Status;
  }

  public void setCircuit2Status(String circuit2Status) {
    this.circuit2Status = circuit2Status;
  }

  private String circuit2Status;

  public boolean isArrosagePermanent() {
    return arrosagePermanent;
  }

  public void setArrosagePermanent(boolean arrosagePermanent) {
    this.arrosagePermanent = arrosagePermanent;
  }

  private boolean arrosagePermanent;
//  private String[] paramAutomate;
  private String irParam;

  private SharedPreferences.Editor editor;
  private SharedPreferences prefs;
  private AProposActivity aProposActivity;
  private LogsActivity logsActivity;
  private PowerPlagePacActivity powerPlagePacActivity;
  private ParameterActivity parameterActivity;
  private String serverMqtt;

  public Param getcParam() {
    return cParam;
  }

  public void setcParam(Param cParam) {
    this.cParam = cParam;
  }

  private Param cParam;

  public AProposActivity getaProposActivity() { return aProposActivity; }
  public void setaProposActivity(AProposActivity aProposActivity) { this.aProposActivity = aProposActivity; }
  public LogsActivity getLogsActivity() { return logsActivity; }
  public void setLogsActivity(LogsActivity logsActivity) { this.logsActivity = logsActivity; }
//  public String[] getParamAutomate() {
//    return paramAutomate;
//  }
//  public void setParamAutomate(String[] paramAutomate) {
//    this.paramAutomate = paramAutomate;
//  }


  public boolean isSignalDefautPompe() {
    return signalDefautPompe;
  }
  public void setSignalDefautPompe(boolean signalDefautPompe) {
    this.signalDefautPompe = signalDefautPompe;
  }
  public MainActivity getMainActivity() {
    return mainActivity;
  }
  public void set_this(MainActivity _this) {
    this.mainActivity = _this;
  }
  public ImageButton[] getImageButtons() { return imageButtons; }
  public void setImageButton(ImageButton imageButton, int nButton) {
    this.imageButtons[nButton] = imageButton;
  }
  public void setImageButtons(ImageButton[] imageButton) {
    this.imageButtons = imageButton;
  }
  public SharedPreferences.Editor getEditor() { return editor; }
  public void setEditor(SharedPreferences.Editor editor) { this.editor = editor; }
  public SharedPreferences getPrefs() { return prefs; }
  public void setPrefs(SharedPreferences prefs) { this.prefs = prefs;  }

  public String getIrParam() {
    return irParam;
  }
  public void  setIrParam(String irParam) {
    this.irParam = irParam;
  }

  static Unic getInstance() {
    return ourInstance;
  }


  public PowerPlagePacActivity getPowerPacActivity() {
    return this.powerPlagePacActivity;
  }

  public void setParameterActivity(ParameterActivity parameterActivity) {
    this.parameterActivity = parameterActivity;
  }
  public ParameterActivity getParameterActivity() {
    return this.parameterActivity;
  }

  public void setBrockerAdr(String serverMqtt) {
    this.serverMqtt = serverMqtt;
  }
  public String getBrockerAdr() {
    return this.serverMqtt;
  }

  public void setPlageIrrigationActivity(PlageIrrigationActivity plageIrrigationActivity) {
    this.plageIrrigationActivity = plageIrrigationActivity;
  }
  public PlageIrrigationActivity getPlageIrrigationActivity() {
    return this.plageIrrigationActivity;
  }

  public void setGlobalSchedParam(String reponse) {
    stabGlobalSchedParam = reponse.split(":");
  }

  public  String[] getGlobalSchedParam() {
    return stabGlobalSchedParam;
  }

}
