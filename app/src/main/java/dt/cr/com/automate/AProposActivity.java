package dt.cr.com.automate;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class AProposActivity extends AppCompatActivity {
  private TextView textViewVersSrvLog;
  private TextView textViewVersSrvIP;
  private TextView textViewIPClientIR;
  private TextView textViewVers_IR;
  private TextView textViewDateClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_apropos);
    Unic.getInstance().setaProposActivity(this);
    ActionBar actionBar = getSupportActionBar();
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);
    TextView  textViewVersionLog = findViewById(R.id.id_VersionLog);
    textViewVersionLog.setText(Unic.getInstance().version);
    textViewVersSrvLog = findViewById(R.id.id_VersionServeurLog);
    textViewVersSrvIP = findViewById(R.id.id_IpClientLog);
    textViewIPClientIR = findViewById(R.id.id_textViewIpClientIR);
    textViewVers_IR = findViewById(R.id.id_textViewClientIR);
    textViewDateClient = findViewById(R.id.id_TextDateClient);
    Unic.getInstance().getMainActivity().readVersion();
  }

  public boolean onOptionsItemSelected(MenuItem item){
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void publishClientVersion(String reponse) {
    String[] info = reponse.split("\n");
    textViewVersSrvLog.setText(info[0]);
    textViewVersSrvIP.setText(info[1]);
    textViewDateClient.setText(info[2]);
  }

  public void publishIrVersion(String reponse) {
    String[] info = reponse.split("\n");
    textViewIPClientIR.setText(info[0]);
    textViewVers_IR.setText(info[1]);
  }
}