package dt.cr.com.automate;

import java.util.Arrays;

/*
  Chaine param (C++)
  const char *PARAM = "0:00:00:00:00:0:00:00:00:00:0:00:00:00:00:0:00:00:00:00:" \
                      "0:00:00:00:00:0:00:00:00:00:0:00:00:00:00:0:00:00:00:00:" \
                      "0:00:00:00:00:0:00:00:00:00:0:00:00:00:00:0:00:00:00:00:" \
                      "0:00:00:00:00:0:00:00:00:00:0:00:00:00:00:0:00:00:00:00:" \
                      "0:00:00:00:00:0:00:00:00:00:0:00:00:00:00:0:00:00:00:00";
  Format des paramètres
  "0:00:00:00:00:0:00:00:00:00:0:00:00:00:00:"
   <    PLAGE   ><    PLAGE   ><    PLAGE   >
  Une ligne = un dispositif avec trois plages de commande
  auto:h_min:m_min:h_max:m_max 4 X
  Chaque plage comporte 5 items (NB_ITEMS_PLAGE = 5)
  auto : "1" programmé, "0" non programmé
  h_min : heure de déclanchement de l'action
  m_min : minute de déclanchement de l'action
  h_max : heure d'arrèt de l'action
  m_mmx : minute d'arrèt de l'action
  Les quatres premiers groupes concernent la coupure des appareils de cuisson (seul h_max et m_max sont utilisés)
  Les quatres groupes suivant concernent le remplissage du réservoir pour l'irrigation (seul h_min et m_min sont utilisés)
  Les quatres groupes suivant concernent la commande de l'électrovanne pour l"arrosage des pots facade SUD
  Les quatres groupes suivant concernent la commande de la PAC
  Les quatres groupes suivant concernent la commande de la VMC

  La programmation des actions est faite toutes les minutes dans schedule() du client

  static final int NB_ITEMS_PLAGE = 5;
  static final int NB_PLAGES = 4;
  static final int NB_DISPOSITIFS = 5;

  Pour ajouter un dispositif:
  - ajouter une ligne dans PARAM
  - ajouter 1 à NB_DISPOSITIFS
  Pour ajouter une plage:
  - ajouter 0:00:00:00:00 à chaque lignes
  - ajouter 1 à NB_PLAGES
*/

class ItemParam {
  String enable_;
  String hMin_;
  String mMin_;
  String hMax_;
  String mMax_;

  public ItemParam() {
  }

  public ItemParam(String enable, String HMin, String MMin, String HMax, String MMax) {
    this.enable_ = enable;
    this.hMin_ = HMin;
    this.mMin_ = MMin;
    this.hMax_ = HMax;
    this.mMax_ = MMax;
  }

  public String toString() {
    return
        this.enable_ + ":" +
            this.hMin_ + ":" +
            this.mMin_ + ":" +
            this.hMax_ + ":" +
            this.mMax_;
  }
}

class DeviceParam {
  final ItemParam[] itemsParams;

  public DeviceParam() {
    itemsParams = new ItemParam[Unic.NB_PLAGES];
  }

  public DeviceParam(ItemParam[] itemsParam) {
    this.itemsParams = itemsParam;
  }

  public ItemParam get(int n) {
    return itemsParams[n];
  }

  public void set(ItemParam itemParam, int n) {
    itemsParams[n] = itemParam;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < Unic.NB_PLAGES; i++) {
      sb.append(Arrays.toString(itemsParams));
      if (i < Unic.NB_PLAGES - 1)
        sb.append(":");
    }
    return sb.toString();
  }
}

public class Param {
  private String[] tabParam;

  public void setParam(String sParam) {
    tabParam = sParam.split(":");
  }

  public void set(int device, int champ, ItemParam itemParam) {
    tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + Unic.NB_ITEMS_PLAGE * champ] = itemParam.enable_;
    tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 1)] = itemParam.hMin_;
    tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 2)] = itemParam.mMin_;
    tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 3)] = itemParam.hMax_;
    tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 4)] = itemParam.mMax_;
  }

  public ItemParam get(int device, int champ) {
    ItemParam itemParam = new ItemParam();
    itemParam.enable_ = tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + Unic.NB_ITEMS_PLAGE * champ];
    itemParam.hMin_ = tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 1)];
    itemParam.mMin_ = tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 2)];
    itemParam.hMax_ = tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 3)];
    itemParam.mMax_ = tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 4)];
    return itemParam;
  }

  public ItemParam[] get(int device) {
    ItemParam[] itemsParam = new ItemParam[Unic.NB_PLAGES];
    for (int i = 0; i < Unic.NB_PLAGES; i++) {
      itemsParam[i] = new ItemParam();
      itemsParam[i].enable_ = tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + Unic.NB_ITEMS_PLAGE * i];
      itemsParam[i].hMin_ = tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * i + 1)];
      itemsParam[i].mMin_ = tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * i + 2)];
      itemsParam[i].hMax_ = tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * i + 3)];
      itemsParam[i].mMax_ = tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * i + 4)];
    }
    return itemsParam;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    for (; i < tabParam.length - 1; i++) {
      sb.append(tabParam[i] + ":");
    }
    sb.append(tabParam[i]);
    return sb.toString();
  }

  public String[] getTabParam() {
    return tabParam;
  }

  public String hMin(int device, int champ) {
    return tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 1)];
  }

  public String mMin(int device, int champ) {
    return tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 2)];
  }

  public String hMax(int device, int champ) {
    return tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 3)];
  }

  public String mMax(int device, int champ) {
    return tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 4)];
  }

  public void set_hMin(String hMin, int device, int champ) {
    tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 1)] = hMin;
  }

  public void set_hMin(int ihMin, int device, int champ) {
    set_hMin(String.format("%02d", ihMin), device, champ);
  }

  public void set_mMin(String mMin, int device, int champ) {
    tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 2)] = mMin;
  }

  public void set_mMin(int imMin, int device, int champ) {
    set_mMin(String.format("%02d", imMin), device, champ);
  }

  public void set_hMax(String hMax, int device, int champ) {
    tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 3)] = hMax;
  }

  public void set_hMax(int ihMax, int device, int champ) {
    set_hMax(String.format("%02d", ihMax), device, champ);
  }

  public void set_mMax(String mMax, int device, int champ) {
    tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + (Unic.NB_ITEMS_PLAGE * champ + 4)] = mMax;
  }

  public void set_mMax(int imMax, int device, int champ) {
    set_mMax(String.format("%02d", imMax), device, champ);
  }

  public int ihMin(int device, int champ) {
    return Integer.parseInt(hMin(device, champ));
  }

  public int imMin(int device, int champ) {
    return Integer.parseInt(mMin(device, champ));
  }

  public int ihMax(int device, int champ) {
    return Integer.parseInt(hMax(device, champ));
  }

  public int imMax(int device, int champ) {
    return Integer.parseInt(mMax(device, champ));
  }

  public String sEnable(int device, int champ) {
    return tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + Unic.NB_ITEMS_PLAGE * champ];
  }

  public void setEnable(boolean enable, int device, int champ) {
    tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + Unic.NB_ITEMS_PLAGE * champ] = (enable ? "1" : "0");
  }

  public void setCmdEnable(String cmd, int device, int champ) {
    tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + Unic.NB_ITEMS_PLAGE * champ] = cmd;
  }

  public boolean isEnable(int device, int champ) {
    return "1".equals(tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + Unic.NB_ITEMS_PLAGE * champ]);
  }

  public int getCmdEnable(int device, int champ) {
    return Integer.parseInt(tabParam[(Unic.NB_ITEMS_PLAGE * Unic.NB_PLAGES) * device + Unic.NB_ITEMS_PLAGE * champ]);
  }


  public String paramDebug() {
    StringBuilder dataParam = new StringBuilder("-");
    int i = Unic.PARAM_START;
    for (; i < Unic.MAX_PARAM - 1; i++) {
      if (i % 5 == 0)
        dataParam.append("  ");
      if (i % 15 == 0)
        dataParam.append("\n");
      dataParam.append(String.format("%2s:", tabParam[i]));
    }
    dataParam.append(tabParam[i]);
    return dataParam.toString();
  }
}
