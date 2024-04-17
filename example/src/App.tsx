import * as React from 'react';

import { StyleSheet, View, Text } from 'react-native';
import mqtt from 'react-native-mqtt';

export default function App() {
  const [result, setResult] = React.useState<string>();

  React.useEffect(() => {
    mqtt.connect('test.mosquitto.org', 'mqtt').then((r) => {
      console.log(r);
      setResult(r);
    });
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
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
