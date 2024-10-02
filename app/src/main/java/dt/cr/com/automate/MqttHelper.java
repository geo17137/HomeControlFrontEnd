package dt.cr.com.automate;

import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

class MqttHelper {

  private static final String userName = Secret.userName;
  private static final String password = Secret.password;
  private static final String TAG = "mqtt";
  private final MainActivity mainActivity;
  private final MqttAndroidClient mqttAndroidClient;

  public MqttHelper(MainActivity mainActivity, String serverUri) {
    this.mainActivity = mainActivity;
//    this.serverUri = serverUri;
    String clientId = MqttClient.generateClientId();
    mqttAndroidClient = new MqttAndroidClient(mainActivity, serverUri, clientId);
    mqttAndroidClient.setCallback(new MqttCallbackExtended() {
      @Override
      public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
          Log.d(TAG, "Reconnected to : " + serverURI);
        } else {
          Log.d(TAG, "Connected to: " + serverURI);
        }
        mainActivity.connectComplete(mqttAndroidClient);
      }

      @Override
      public void connectionLost(Throwable cause) {
        mainActivity.connectionLost();
      }

      @Override
      public void messageArrived(String topic, MqttMessage message) throws Exception {
        mainActivity.messageArrived(topic, message);
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {
        mainActivity.deliveryComplete();
      }
    });

    MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
    mqttConnectOptions.setAutomaticReconnect(true);
    mqttConnectOptions.setCleanSession(false);
    mqttConnectOptions.setUserName(userName);
    mqttConnectOptions.setPassword(password.toCharArray());

    try {
      mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
          try {
            asyncActionToken.getSessionPresent();
          } catch (Exception e) {
            String message = e.getMessage();
            Log.d(TAG, "error message is null " + String.valueOf(message == null));
          }
         // Log.d(TAG, "connected to: " + serverUri);
         // Toast.makeText(mainActivity, "connected", Toast.LENGTH_SHORT).show();
          DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
          disconnectedBufferOptions.setBufferEnabled(true);
          disconnectedBufferOptions.setBufferSize(100);
          disconnectedBufferOptions.setPersistBuffer(false);
          disconnectedBufferOptions.setDeleteOldestMessages(false);
          mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
          mainActivity.connected();
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
          mainActivity.onFailure(asyncActionToken);
          Log.d(TAG, "Failed to connect to: " + serverUri);
        }
      });
    } catch (MqttException ex) {
      ex.printStackTrace();
    }
  }

  public boolean isConnected() {
    return mqttAndroidClient.isConnected();
  }

  public void publish(String topic, byte[] payload) {
    if (mqttAndroidClient.isConnected()) {
      try {
        mqttAndroidClient.publish(topic, payload, 0, false);
      } catch (MqttPersistenceException e) {
        Log.d("mqtt", e.getLocalizedMessage());
      } catch (MqttException e) {
        Log.d("mqtt", e.getLocalizedMessage());
      }
    } else {
      Log.d("mqtt", "Could not publish. Client is not connected. Please connect first.");
    }
  }
}
