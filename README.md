# react-native-mqtt

This library is used to communicate with the message broker using the MQTT protocol. This library is available in a React Native environment and supports TypeScript.

This library uses the following native MQTT client libraries:

> iOS - https://github.com/emqx/CocoaMQTT
>
> Android - https://github.com/eclipse/paho.mqtt.android

## Installation

To use this library, you must first install it using npm or yarn.

```sh
npm install @ko-developerhong/react-native-mqtt
# OR
yarn add @ko-developerhong/react-native-mqtt
```

## IOS Setup
Add Podfile
```
pod "CocoaMQTT", :modular_headers => true
pod "MqttCocoaAsyncSocket", :modular_headers => true
```

## Import
```ts
import MqttClient, { ConnectionOptions, ClientEvent, MQTTEventHandler } from 'react-native-mqtt';
```

## Connect
To connect to the message broker, use the connect method, which takes the host address and connection options as arguments.

```tsx
import MqttClient, { ConnectionOptions } from 'react-native-mqtt';

const options: ConnectionOptions = {
  clientId: 'myClientId',
  cleanSession: true,
  keepAlive: 60,
  timeout: 60,
  maxInFlightMessages: 1,
  autoReconnect: true,
  username: 'myUsername',
  password: 'myPassword',
  tls: {
    caDer: Buffer.from('myCertificate'),
    cert: 'myCertificate',
    key: 'myKey',
    p12: Buffer.from('myP12'),
    pass: 'myPass',
  },
  allowUntrustedCA: true,
  enableSsl: true,
  protocol: 'mqtts',
};

MqttClient.connect('mqtt://broker.hivemq.com', options)
  .then(() => console.log('Connected'))
  .catch((error) => console.error('Connection failed: ', error));
```

## Subscribe
To subscribe to a specific topic, use the subscribe method.
```ts
type MQTT = {
  subscribe: (topic: string, qos?: number) => void;
};
```
```ts
MqttClient.subscribe('myTopic', 0);
```

## Publish a message
To publish a message, use the publish method.
```ts
type MQTT = {
  publish: (topic: string, message: string, qos?: number , retained?: boolean) => void,
}
```
```ts
MqttClient.publish('myTopic', 'Hello, World!', 0, false);
```

## Event Handler
This library offers a variety of events. To handle events, use the on, once, and off methods.
```ts
const onConnect: MQTTEventHandler<ClientEvent.Connect> = (reconnect) => {
  console.log('Connected', reconnect);
};

MqttClient.on(ClientEvent.Connect, onConnect);

MqttClient.once(ClientEvent.Disconnect, (cause) => {
  console.log('Disconnected', cause);
});

MqttClient.off(ClientEvent.Connect, onConnect);
```

## Disconnect
To disconnect, use the disconnect method.
```ts
MqttClient.disconnect();
```

## Close
To close the library, use the close method.
```ts
MqttClient.close();
```

## Troubleshooting
> The Swift pod `CocoaMQTT` depends upon `MqttCocoaAsyncSocket`, which does not define modules.
```
you may set `use_modular_headers!` globally in your Podfile, or specify `:modular_headers => true` for particular dependencies.
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## NOTE

- This library is available only in React Native environments.
- This library supports TypeScript.
- This library is used to communicate with the message broker using the MQTT protocol.
- This library is based on [react-native-mqtt](https://github.com/davesters/rn-native-mqtt).


## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)







