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

  @ReactMethod
  fun newClient(id: String) {
    clients[id] = _reactContext?.let { Mqtt(it,id) }!!
  }

  @ReactMethod
  fun connect(id: String?, host: String?, options: ReadableMap, callback: Callback) {
    if (!clients.containsKey(id)) {
      return
    }
    clients[id]?.connect(host, options, callback)
  }

  @ReactMethod
  fun subscribe(id: String?, topicList: ReadableArray, qosList: ReadableArray) {
    if (!clients.containsKey(id)) {
      return
    }
    clients[id]?.subscribe(topicList, qosList)
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

  companion object {
    const val NAME = "Mqtt"
    val clients =  ConcurrentHashMap<String, Mqtt>();
    private var _reactContext: ReactApplicationContext? = null
  }
}
