package com.mqtt

import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import java.util.concurrent.ConcurrentHashMap


class MqttModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  init {
    _reactContext  = reactContext
  }

  override fun getName(): String {
    return NAME
  }

  fun isInRange(number: Int?): Boolean {
    return number != null && number in 0..999
  }

  @ReactMethod
  fun newClient(id: String) {
    clients[id] = _reactContext?.let { Mqtt(it,id) }!!
  }

  @ReactMethod
  fun connect(id: String?, host: String, options: ReadableMap, callback: Callback) {
    if (!clients.containsKey(id)) {
      return
    }
    clients[id]?.connect(host, options, callback)
  }

  @ReactMethod
  fun subscribe(id: String?, topic: String, qos: Int) {
    if (!clients.containsKey(id)) {
      return
    }
    clients[id]?.subscribe(topic, qos)
  }

  @ReactMethod
  fun unsubscribe(id: String?, topicList: ReadableArray) {
    if (!clients.containsKey(id)) {
      return
    }
    clients[id]?.unsubscribe(topicList)
  }

  @ReactMethod
  fun publish(id: String?, topic: String?, base64Payload: String?, qos: Int, retained: Boolean) {
    if (!clients.containsKey(id)) {
      return
    }
    clients[id]?.publish(topic, base64Payload, qos, retained)
  }

  @ReactMethod
  fun disconnect(id: String?) {
    if (!clients.containsKey(id)) {
      return
    }
    clients[id]?.disconnect()
  }

  @ReactMethod
  fun close(id: String?) {
    if (!clients.containsKey(id)) {
      return
    }
    clients[id]?.close()
    clients.remove(id)
  }


  @ReactMethod
  fun addListener(type: String?) {
    // Keep: Required for RN built in Event Emitter Calls.
  }

  @ReactMethod
  fun removeListeners(type: Int?) {
    // Keep: Required for RN built in Event Emitter Calls.
  }

  companion object {
    const val NAME = "Mqtt"
    val clients =  ConcurrentHashMap<String, Mqtt>();
    private var _reactContext: ReactApplicationContext? = null
  }
}
