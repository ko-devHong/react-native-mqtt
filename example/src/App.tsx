import * as React from 'react';

import { StyleSheet, Text, View } from 'react-native';
import MqttClient, { Event } from 'react-native-mqtt';

export default function App() {
  const [result, setResult] = React.useState<Error>();

  React.useEffect(() => {
    const mqtt = new MqttClient('mqtt://test.mosquitto.org');
    mqtt.connect({}, (e) => {
      console.error(e);
      setResult(e);
    });
    mqtt.on(Event.Connect, (reconnect: boolean) => {
      console.log(reconnect);
    });
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {JSON.stringify(result)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
