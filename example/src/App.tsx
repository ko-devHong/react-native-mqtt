import * as React from 'react';

import { StyleSheet, Text, View } from 'react-native';
import MqttClient, { ClientEvent } from 'react-native-mqtt';

export default function App() {
  const [result, setResult] = React.useState<Error>();

  React.useEffect(() => {
    MqttClient.connect('mqtt://test.mosquitto.org', {}, (e) => {
      console.error(e);
      setResult(e);
    });
    MqttClient.on(ClientEvent.Connect, (reconnect: boolean) => {
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
