//
//  Mqtt.swift
//  react-native-mqtt
//
//  Created by bbrosHong on 2024/05/28.
//
import Foundation
import React
import CocoaMQTT

@objc(Mqtt)
class Mqtt: RCTEventEmitter {
    
    var clients: [ String: MqttClient ] = [:]
        
    override init() {
        super.init()
        EventEmitter.sharedInstance.registerEventEmitter(eventEmitter: self)
    }
    
    @objc
    open override func supportedEvents() -> [String] {
        return EventEmitter.sharedInstance.allEvents
    }
    
    @objc(newClient:)
    func newClient(id: String) {
        clients[id] = MqttClient(withEmitter: EventEmitter.sharedInstance, id: id)
    }
    
    @objc(connect:host:options:callback:)
    func connect(id: String, host: String, options: NSDictionary, callback: @escaping RCTResponseSenderBlock) {
        clients[id]?.connect(host: host, options: options, callback: callback)
    }
    
    @objc(subscribe:topic:qos:)
    func subscribe(id: String, topic: String,qos: NSInteger) {
        clients[id]?.subscribe(topic: topic, qos: forQosInt(qos))
    }
    
    func forQosInt(_ qos: NSInteger) -> CocoaMQTTQoS {
        switch qos {
        case 1:
            return CocoaMQTTQoS.qos1
        case 2:
            return CocoaMQTTQoS.qos2
        default:
            return CocoaMQTTQoS.qos0
        }
    }
    
    @objc(unsubscribe:topicList:)
    func unsubscribe(id: String, topicList: NSArray) {
        for x in 0..<topicList.count {
            clients[id]?.unsubscribe(topic: topicList[x] as! String)
        }
    }

    @objc(publish:topic:base64Payload:qos:retained:)
    func publish(id: String, topic: String, base64Payload: String, qos: NSInteger, retained: Bool) {
        clients[id]?.publish(topic: topic, base64Payload: base64Payload, qos: forQosInt(qos), retained: retained)
    }

    @objc(disconnect:)
    func disconnect(id: String) {
        clients[id]?.disconnect()
    }

    @objc(close:)
    func close(id: String) {
        clients[id] = nil
        clients.removeValue(forKey: id)
    }
}
