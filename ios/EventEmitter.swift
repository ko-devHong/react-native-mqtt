//
//  EventEmitter.swift
//  react-native-mqtt
//
//  Created by bbrosHong on 2024/05/28.
//

import Foundation

enum EventType: String {
    case Connect = "connect"
    case Disconnect = "disconnect"
    case Error = "error"
    case Message = "message"
}

class EventEmitter {

    /// Shared Instance.
    public static var sharedInstance = EventEmitter()

    // NativeMqtt is instantiated by React Native with the bridge.
    private var eventEmitter: Mqtt!

    private init() {}

    // When React Native instantiates the emitter it is registered here.
    func registerEventEmitter(eventEmitter: Mqtt) {
        self.eventEmitter = eventEmitter
    }

    func dispatch(name: String, body: Any?) {
        self.eventEmitter.sendEvent(withName: name, body: body)
    }

    // All Events which must be support by React Native.
    lazy var allEvents: [String] = {
        return [
            EventType.Connect.rawValue,
            EventType.Disconnect.rawValue,
            EventType.Error.rawValue,
            EventType.Message.rawValue,
        ]
    }()
}
