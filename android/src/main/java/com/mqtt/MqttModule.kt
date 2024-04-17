package com.mqtt

import android.util.Log
import androidx.annotation.Nullable
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


class MqttModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  fun connect(url: String, promise: Promise) {
    mqttAndroidClient = MqttAndroidClient(reactApplicationContext, url, clientId)
    mqttAndroidClient!!.setCallback(object : MqttCallbackExtended {
      override fun connectComplete(reconnect: Boolean, serverURI: String) {
        if (reconnect) {
          print("Reconnected to : $serverURI")
          promise.resolve("Reconnected to: $serverURI")
        } else {
          print("Connected to: $serverURI")
          promise.resolve("Connected to: $serverURI")
        }
      }

      override fun connectionLost(cause: Throwable) {
        print("The Connection was lost.")
        promise.reject("The Connection was lost.",cause)
      }

      @Throws(Exception::class)
      override fun messageArrived(topic: String, message: MqttMessage) {
        print("Incoming message: " + String(message.payload))
      }

      override fun deliveryComplete(token: IMqttDeliveryToken) {}
    })
  }



  private fun sendEvent(eventName: String,  params: WritableMap?) {
    try {
      reactApplicationContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit(eventName, params)
    } catch (e: RuntimeException) {
      Log.e("ERROR", "java.lang.RuntimeException: Trying to invoke JS before CatalystInstance has been set!")
    }
  }

  @ReactMethod
  fun on(eventName: String,callback: Callback) {
    mqttAndroidClient!!.setCallback(object : MqttCallbackExtended {
      override fun connectComplete(reconnect: Boolean, serverURI: String) {
        if (reconnect) {
          print("Reconnected to : $serverURI")
          callback.invoke("Reconnected to: $serverURI")
        } else {
          print("Connected to: $serverURI")
          callback.invoke("Connected to: $serverURI")
        }
      }

      override fun connectionLost(cause: Throwable) {
        print("The Connection was lost.")
        callback.invoke("The Connection was lost.",cause)
      }

      @Throws(Exception::class)
      override fun messageArrived(topic: String, message: MqttMessage) {
        callback.invoke("Incoming message: " + String(message.payload))
      }

      override fun deliveryComplete(token: IMqttDeliveryToken) {}
    })
  }

  @ReactMethod
  fun subscribeToTopic(subscriptionTopic: String, promise: Promise) {
    try {
      mqttAndroidClient!!.subscribe(subscriptionTopic, 0, null, object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken) {
          print("Subscribed!")
          promise.resolve("Subscribed")
        }

        override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
          print("Failed to subscribe")
          promise.reject("Failed to subscribe",exception)
        }
      })

      // THIS DOES NOT WORK!
      mqttAndroidClient!!.subscribe(subscriptionTopic, 0) { topic, message -> // message Arrived!
        print("Message: " + topic + " : " + String(message.payload))
      }
    } catch (ex: MqttException) {
      System.err.println("Exception whilst subscribing")
      ex.printStackTrace()
    }
  }

  @ReactMethod
  fun publishMessage(publishTopic: String, publishMessage: String,promise: Promise) {
    try {
      val message = MqttMessage()
      message.setPayload(publishMessage.toByteArray())
      mqttAndroidClient!!.publish(publishTopic, message)
      print("Message Published")
      promise.resolve("Message Published")
      if (!mqttAndroidClient!!.isConnected) {
        print("${mqttAndroidClient!!.bufferedMessageCount} messages in buffer.")
        promise.resolve("${mqttAndroidClient!!.bufferedMessageCount} messages in buffer.")
      }
    } catch (e: MqttException) {
      System.err.println("Error Publishing: " + e.message)
      e.printStackTrace()
      promise.reject("Error Publishing: " + e.message,e)
    }
  }

  companion object {
    const val NAME = "React-native-mqtt"
    var mqttAndroidClient: MqttAndroidClient? = null

    const  val serverUri = "mqtt://test.mosquitto.org"

    const val clientId = "ExampleAndroidClient"
    const val _subscriptionTopic = "presence"
    const val _publishTopic = "presence"
    const val _publishMessage = "Hello mqtt"
    const val CONNECT_EVENT = "connect"
    const val RECONNECT_EVENT = "reconnect"
    const val ERROR_EVENT = "error"
    const val PUBLISH_EVENT = "publish"
    const val MESSAGE_EVENT = "message"
  }
}
