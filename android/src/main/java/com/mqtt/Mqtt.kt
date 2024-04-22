package com.mqtt

import android.util.Base64;

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Callback
import com.facebook.react.modules.core.RCTNativeAppEventEmitter

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient

import java.util.concurrent.atomic.AtomicReference
import com.heroku.sdk.EnvKeyStore

import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.KeyStore
import java.security.SecureRandom

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

import java.io.ByteArrayInputStream
import java.io.InputStream

class Mqtt(reactContext: ReactApplicationContext, originId: String) {

  init {
    _reactContext = reactContext
    clientId = originId
  }

  fun connect(host: String?, options: ReadableMap, callback: Callback) {

    connectCallback.set(callback)
    try {
      client.set(MqttAsyncClient(host, clientId, MemoryPersistence()))
      val connOpts = MqttConnectOptions()
      connOpts.isCleanSession = !options.hasKey("cleanSession") || options.getBoolean("cleanSession")
      connOpts.setKeepAliveInterval(if (options.hasKey("keepAliveInterval")) options.getInt("keepAliveInterval") else 60)
      connOpts.setConnectionTimeout(if (options.hasKey("timeout")) options.getInt("timeout") else 10)
      connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1)
      connOpts.setMaxInflight(if (options.hasKey("maxInFlightMessages")) options.getInt("maxInFlightMessages") else 10)
      connOpts.isAutomaticReconnect = options.hasKey("autoReconnect") && options.getBoolean("autoReconnect")
      if (options.hasKey("username")) {
        connOpts.setUserName(options.getString("username"))
      }
      if (options.hasKey("password")) {
        connOpts.password = options.getString("password")!!.toCharArray()
      }
      if (options.hasKey("tls")) {
        val tlsOptions: ReadableMap? = options.getMap("tls")
        val ca: String? = if (tlsOptions!!.hasKey("caDer")) tlsOptions.getString("caDer") else null
        val cert: String? = if (tlsOptions.hasKey("cert")) tlsOptions.getString("cert") else null
        val key: String? = if (tlsOptions.hasKey("key")) tlsOptions.getString("key") else null
        var keyManagers: Array<KeyManager?>? = null
        var trustManagers: Array<TrustManager?>? = null
        if (cert != null && key != null) {
          val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("PKIX")
          val keyStore: KeyStore = EnvKeyStore.createFromPEMStrings(key, cert, "").keyStore()
          keyManagerFactory.init(keyStore, "".toCharArray())
          keyManagers = keyManagerFactory.keyManagers
        }
        if (ca != null) {
          val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
          val caInput: InputStream = ByteArrayInputStream(Base64.decode(ca, Base64.DEFAULT))
          val caCert: Certificate = cf.generateCertificate(caInput)
          caInput.close()
          val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
          keyStore.load(null, null)
          keyStore.setCertificateEntry("ca", caCert)
          val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
          tmf.init(keyStore)
          trustManagers = tmf.trustManagers
        }
        if (keyManagers != null || trustManagers != null) {
          val sslContext: SSLContext = SSLContext.getInstance("TLS")
          sslContext.init(keyManagers, trustManagers, SecureRandom())
          connOpts.socketFactory = sslContext.socketFactory
        }
      }
      client.get().setCallback(MqttEventCallback())
      client.get().connect(connOpts, _reactContext, ConnectMqttActionListener())
    } catch (ex: java.lang.Exception) {
      callback.invoke(ex.message)
      ex.printStackTrace()
    }
  }

  fun subscribe(topic: String, qos: Int) {
    try {
      client.get().subscribe(topic, qos, null, SubscribeMqttActionListener())
    } catch (ex: java.lang.Exception) {
      val params: WritableMap = Arguments.createMap()
      params.putString("message", "Error subscribing")
      params.putString("error", ex.message)
      sendEvent(ERROR_EVENT, params)
    }
  }

  fun unsubscribe(topicList: ReadableArray) {
    try {
      val topic: Array<String?> = arrayOfNulls<String>(topicList.size())
      for (x in 0 until topicList.size()) {
        topic[x] = topicList.getString(x)
      }
      client.get().unsubscribe(topic, null, UnsubscribeMqttActionListener())
    } catch (ex: java.lang.Exception) {
      val params: WritableMap = Arguments.createMap()
      params.putString("message", "Error unsubscribing")
      params.putString("error", ex.message)
      sendEvent(ERROR_EVENT, params)
    }
  }

  fun publish(topic: String?, base64Payload: String?, qos: Int, retained: Boolean) {
    val message: MqttMessage = MqttMessage(Base64.decode(base64Payload, Base64.DEFAULT))
    message.setQos(qos)
    message.setRetained(retained)
    try {
      client.get().publish(topic, message)
    } catch (ex: MqttException) {
      val params: WritableMap = Arguments.createMap()
      params.putString("message", "Error publishing message")
      params.putString("error", ex.message)
      sendEvent(ERROR_EVENT, params)
    }
  }

  fun disconnect() {
    try {
      client.get().disconnect(null, DisconnectMqttActionListener())
    } catch (ex: MqttException) {
      val params: WritableMap = Arguments.createMap()
      params.putString("message", "Error disconnecting")
      params.putString("error", ex.message)
      sendEvent(ERROR_EVENT, params)
    }
  }

  fun close() {
    try {
      client.get().close()
    } catch (ex: MqttException) {
      val params: WritableMap = Arguments.createMap()
      params.putString("message", "Error closing")
      params.putString("error", ex.message)
      sendEvent(ERROR_EVENT, params)
    }
  }


  private class MqttEventCallback() : MqttCallbackExtended {
    override fun connectionLost(cause: Throwable) {
      val params: WritableMap = Arguments.createMap()
      params.putString("cause", cause.message)
      sendEvent(DISCONNECT_EVENT, params)
    }

    override fun messageArrived(topic: String, message: MqttMessage) {
      val params: WritableMap = Arguments.createMap()
      params.putString("topic", topic)
      params.putString("message", Base64.encodeToString(message.getPayload(), Base64.DEFAULT))
      sendEvent(MESSAGE_EVENT, params)
    }

    override fun deliveryComplete(token: IMqttDeliveryToken) {}
    override fun connectComplete(reconnect: Boolean, serverURI: String) {
      val params: WritableMap = Arguments.createMap()
      params.putBoolean("reconnect", reconnect)
      sendEvent(CONNECT_EVENT, params)
    }
  }


  private class ConnectMqttActionListener() : IMqttActionListener {
    override fun onSuccess(asyncActionToken: IMqttToken) {
      connectCallback.get().invoke()
    }

    override fun onFailure(asyncActionToken: IMqttToken, ex: Throwable) {
      connectCallback.get().invoke(ex.message)
      ex.printStackTrace()
    }
  }


  private class DisconnectMqttActionListener() : IMqttActionListener {
    override fun onSuccess(asyncActionToken: IMqttToken) {
      val params: WritableMap = Arguments.createMap()
      params.putString("cause", "User disconnected")
      sendEvent(DISCONNECT_EVENT, params)
    }

    override fun onFailure(asyncActionToken: IMqttToken, ex: Throwable) {
      val params: WritableMap = Arguments.createMap()
      params.putString("message", "Error connecting")
      params.putString("error", ex.message)
      sendEvent(ERROR_EVENT, params)
    }
  }


  private class SubscribeMqttActionListener() : IMqttActionListener {
    override fun onSuccess(asyncActionToken: IMqttToken) {}
    override fun onFailure(asyncActionToken: IMqttToken, ex: Throwable) {
      val params: WritableMap = Arguments.createMap()
      params.putString("message", "Error subscribing")
      params.putString("error", ex.message)
      sendEvent(ERROR_EVENT, params)
    }
  }


  private class UnsubscribeMqttActionListener() : IMqttActionListener {
    override fun onSuccess(asyncActionToken: IMqttToken) {}
    override fun onFailure(asyncActionToken: IMqttToken, ex: Throwable) {
      val params: WritableMap = Arguments.createMap()
      params.putString("message", "Error unsubscribing")
      params.putString("error", ex.message)
      sendEvent(ERROR_EVENT, params)
    }
  }


  companion object {
    private var _reactContext: ReactApplicationContext? = null
    val client = AtomicReference<IMqttAsyncClient>();
    val connectCallback = AtomicReference<Callback>();

    var clientId = ""

    const val CONNECT_EVENT = "connect"
    const val DISCONNECT_EVENT = "disconnect"
    const val ERROR_EVENT = "error"
    const val MESSAGE_EVENT = "message"

    fun sendEvent(eventName: String, params: WritableMap) {
      params.putString("id", clientId)
      _reactContext?.getJSModule(RCTNativeAppEventEmitter::class.java)?.emit(eventName, params)
    }
  }
}
