//
//  MqttClient.swift
//  react-native-mqtt
//
//  Created by bbrosHong on 2024/05/28.
//

import Foundation
import CocoaMQTT

class MqttClient {

    private let eventEmitter: EventEmitter
    ///MQTT 5.0
    private var clientID = "CocoaMQTT-" + String(ProcessInfo().processIdentifier)
    private var client: CocoaMQTT

    private var connectCallback: RCTResponseSenderBlock? = nil

    init(withEmitter emitter: EventEmitter, id:String) {
        self.eventEmitter = emitter
        self.clientID = id
        self.client = CocoaMQTT(clientID: self.clientID)
    }

    func connect(host: String, options: NSDictionary, callback: @escaping RCTResponseSenderBlock) {
        self.connectCallback = callback
        
        guard let url = URLComponents(string: host) else {
            callback([ "Error parsing host URL" ])
            return
        }
        let port = UInt16(url.port != nil ? url.port! : 1883)
        
        
        if (url.string?.hasPrefix("ws") == true) {
            let websocket = CocoaMQTTSocket()
            do {
                try websocket.connect(toHost: url.path, onPort: port)
                self.client = CocoaMQTT(clientID: self.clientID, socket: websocket)
            } catch {
                callback([ "Failed to CocoaMQTT websocket connected" ])
                return
            }
        }

        self.client.host = url.host!
        self.client.port = port
        self.client.delegate = self

        if let clientId = options["clientId"] as! String? {
            self.client.clientID = clientId
        }
        if let enableSsl = options["enableSsl"] as! Bool? {
            self.client.enableSSL = enableSsl
        }
        if let allowUntrustedCA = options["allowUntrustedCA"] as! Bool? {
            self.client.allowUntrustCACertificate = allowUntrustedCA
        }
        if let cleanSession = options["cleanSession"] as! Bool? {
            self.client.cleanSession = cleanSession
        }
        if let keepAlive = options["keepAliveInterval"] as! Int? {
            self.client.keepAlive = UInt16(keepAlive)
        }
        if let maxInFlightMessages = options["maxInFlightMessages"] as! Int? {
            self.client.inflightWindowSize = UInt(maxInFlightMessages)
        }
        if let autoReconnect = options["autoReconnect"] as! Bool? {
            self.client.autoReconnect = autoReconnect
        }
        if let username = options["username"] as! String? {
            self.client.username = username
        }
        if let password = options["password"] as! String? {
            self.client.password = password
        }

        if let tlsOptions = options["tls"] as! NSDictionary? {
            if let p12base64Cert = tlsOptions["p12"] as! String?, let p12Pass = tlsOptions["pass"] as! String? {
                let opts: NSDictionary = [kSecImportExportPassphrase: p12Pass]
                var items: CFArray?

                guard let p12Data = NSData(base64Encoded: p12base64Cert, options: .ignoreUnknownCharacters) else {
                    callback([ "Failed to read p12 certificate" ])
                    return
                }
                let securityError = SecPKCS12Import(p12Data, opts, &items)

                guard securityError == errSecSuccess else {
                    if securityError == errSecAuthFailed {
                        callback([ "SecPKCS12Import returned errSecAuthFailed. Incorrect password?" ])
                    } else {
                        callback([ "Failed to read p12 certificate" ])
                    }
                    return
                }

                guard let theArray = items, CFArrayGetCount(theArray) > 0 else {
                    callback([ "Failed to properly read p12 certificate" ])
                    return
                }

                let dictionary = (theArray as NSArray).object(at: 0)
                guard let identity = (dictionary as AnyObject).value(forKey: kSecImportItemIdentity as String) else {
                    callback([ "Failed to properly read p12 certificate" ])
                    return
                }

                var sslSettings: [String: NSObject] = [:]

                sslSettings["kCFStreamSSLIsServer"] = NSNumber(value: false)
                sslSettings["kCFStreamSSLCertificates"] = [identity] as CFArray
                self.client.sslSettings = sslSettings
            }
        }

        self.client.connect()
    }

    func subscribe(topic: String, qos: CocoaMQTTQoS) {
        do {
            self.client.subscribe(topic, qos: qos)
        } catch {
            sendEvent(name: EventType.Error.rawValue, body: [
                "id": self.clientID,
                "error": error.localizedDescription
            ])
        }
    }

    func unsubscribe(topic: String) {
        do {
            self.client.unsubscribe(topic)
        } catch {
            sendEvent(name: EventType.Error.rawValue, body: [
                "id": self.clientID,
                "error": error.localizedDescription
            ])
        }
    }

    func publish(topic: String, base64Payload: String, qos: CocoaMQTTQoS, retained: Bool) {
        guard let payload = Data(base64Encoded: base64Payload) else {
            return
        }

        do {
            let message = CocoaMQTTMessage(topic: topic, payload: [UInt8](payload))
            message.qos = qos
            message.retained = retained
            
            self.client.publish(message)
        } catch {
            sendEvent(name: EventType.Error.rawValue, body: [
                "id": self.clientID,
                "error": error.localizedDescription
            ])
        }
    }

    func disconnect() {
        do {
            self.client.disconnect()
        } catch {
            sendEvent(name: EventType.Error.rawValue, body: [
                "id": self.clientID,
                "error": error.localizedDescription
            ])
        }
    }
}

extension MqttClient: CocoaMQTTDelegate {
    func mqtt(_ mqtt: CocoaMQTT, didSubscribeTopics success: NSDictionary, failed: [String]) {}
    
    func mqtt(_ mqtt: CocoaMQTT, didUnsubscribeTopics topics: [String]) {}
    
    func mqtt(_ mqtt: CocoaMQTT, didConnectAck ack: CocoaMQTTConnAck) {
        if (ack == .accept) {
            self.connectCallback?(nil)
            return
        }
        
        sendEvent(name: EventType.Error.rawValue, body: [
            "id": self.clientID,
            "error": "Error connecting: \(ack.description)"
        ])
    }

    func mqtt(_ mqtt: CocoaMQTT, didStateChangeTo state: CocoaMQTTConnState) {
        if (state == .connected) {
            sendEvent(name: EventType.Connect.rawValue, body: [
                "id": self.clientID,
            ])
        }
        if (state == .disconnected) {
            sendEvent(name: EventType.Disconnect.rawValue, body: [
                "id": self.clientID,
            ])
        }
    }

    func mqtt(_ mqtt: CocoaMQTT, didPublishMessage message: CocoaMQTTMessage, id: UInt16) {}

    func mqtt(_ mqtt: CocoaMQTT, didPublishAck id: UInt16) {}

    func mqtt(_ mqtt: CocoaMQTT, didReceiveMessage message: CocoaMQTTMessage, id: UInt16) {
        sendEvent(name: EventType.Message.rawValue, body: [
            "id": self.clientID,
            "topic": message.topic,
            "message": message.payload
        ])
    }

    func mqttDidPing(_ mqtt: CocoaMQTT) {}

    func mqttDidReceivePong(_ mqtt: CocoaMQTT) {}

    func mqttDidDisconnect(_ mqtt: CocoaMQTT, withError err: Error?) {
        if let error = err {
            sendEvent(name: EventType.Disconnect.rawValue, body: [
                "id": self.clientID,
                "cause": error.localizedDescription
            ])
        } else {
            sendEvent(name: EventType.Disconnect.rawValue, body: [
                "id": self.clientID
            ])
        }
    }

    func mqtt(_ mqtt: CocoaMQTT, didReceive trust: SecTrust, completionHandler: @escaping (Bool) -> Void) {
        var accept = true
        let isServerTrusted = SecTrustEvaluateWithError(trust, nil)

        if (!isServerTrusted) {
            accept = false
        }

        completionHandler(accept)
    }

    func sendEvent(name: String, body: Any) {
        eventEmitter.dispatch(name: name, body: body)
    }
}
