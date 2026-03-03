
package dt.cr.com.automate;

import static java.lang.System.*;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MqttHandler {
  private final MainActivity mainActivity;
  private Mqtt5AsyncClient client;

  // Le Handler est la seule façon native sur Android de rediriger vers le Thread UI
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public MqttHandler(MainActivity mainActivity) {
    this.mainActivity = mainActivity;
  }

  public void connect() {
    try {
      URI uri = new URI(Secret.ADDRESS);
      String host = uri.getHost();
      int port = (uri.getPort() == -1) ? 1883 : uri.getPort();

      client = Mqtt5Client.builder()
          .identifier("automate-" + UUID.randomUUID().toString())
          .serverHost(host)
          .serverPort(port)
          .simpleAuth()
          .username(Secret.userName)
          .password(Secret.password.getBytes(StandardCharsets.UTF_8))
          .applySimpleAuth()
          .addDisconnectedListener(context -> {
            // Redirection sécurisée
            mainHandler.post(() -> connectionLost(context.getCause()));
          })
          .buildAsync();

      client.connect().whenComplete((connAck, throwable) -> {
        // Redirection sécurisée
        mainHandler.post(() -> {
          if (throwable != null) {
            err.println("❌ Échec de connexion : " + throwable.getMessage());
          } else {
            mainActivity.connectComplete();
          }
        });
      });

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void subscribe(String topic, int qosLevel) {
    if (!isConnected()) {
      err.println("❌ Impossible de souscrire : non connecté.");
      return;
    }

    MqttQos qos = MqttQos.fromCode(qosLevel);
    if (qos == null) qos = MqttQos.AT_MOST_ONCE;

    client.subscribeWith()
        .topicFilter(topic)
        .qos(qos)
        .callback(this::handleIncomingMessage)
        .send();
  }

  public void connectionLost(Throwable cause) {
    err.println("⚠️ Connexion perdue ! " + (cause != null ? cause.getMessage() : ""));
  }

  private void handleIncomingMessage(Mqtt5Publish publish) {
    // C'est ICI que la magie opère : on renvoie le message réseau vers l'UI
    mainHandler.post(() -> {
      try {
        mainActivity.messageArrived(publish.getTopic().toString(),
                                    new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8));
//        messageArrived(publish.getTopic().toString(), publish.getPayloadAsBytes());
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  // Utiliser pour debug
  private void messageArrived(String topic, byte[] message) throws Exception {
    String payload = new String(message, StandardCharsets.UTF_8);
    // System.out.println("📩 Reçu sur " + topic + " : " + payload);
    // Garanti à 100% de s'exécuter sur le Main Thread pour vos animations
    mainActivity.messageArrived(topic, payload);
  }

  public void publish(String topic, byte[] payload) {
    if (isConnected()) {
      client.publishWith()
          .topic(topic)
          .payload(payload)
          .qos(MqttQos.AT_LEAST_ONCE)
          .send();
    }
  }

  public boolean isConnected() {
    return client != null && client.getState().isConnected();
  }

  public void deconnect() {
    if (client != null) {
      client.disconnect();
    }
  }
}