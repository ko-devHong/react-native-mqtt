# react-native-mqtt

use mqtt

## Installation

```sh
npm install @ko-developerhong/react-native-mqtt
```

## Usage

```tsx
import * as React from 'react';
import { useEffect, useState } from 'react';
import MqttClient, { ClientEvent } from '@ko-developerhong/react-native-mqtt';

  useEffect(() => {
  (async () => {
    try {
      await MqttClient.connect('mqtt://test.mosquitto.org', {});
      MqttClient.on(ClientEvent.Connect, (reconnect) => {
        console.log('reconnect : ', reconnect);
      });
      MqttClient.on(ClientEvent.Error, (error) => {
        console.log('error : ', error);
      });
      MqttClient.on(ClientEvent.Disconnect, (cause) => {
        console.log('Disconnect cause : ', cause);
      });
      MqttClient.on(ClientEvent.Message, (topic, message) => {
        console.log('topic : ', topic);
        console.log('message : ', message.toString());
        setResult(message.toString());
      });
      MqttClient.subscribe('presence');
      MqttClient.publish('presence', 'Hello mqtt');
    } catch (err) {
      console.error('catch error: ', err);
    }
  })();
}, []);
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
