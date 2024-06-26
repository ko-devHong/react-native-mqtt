import * as React from 'react';
import { useEffect, useState } from 'react';

import { StyleSheet, Text, View } from 'react-native';
import MqttClient, { ClientEvent } from '@ko-developerhong/react-native-mqtt';

export default function App() {
  const [mode] = useState<'mqtt' | 'mqtts'>('mqtts');
  const [result, setResult] = useState<any>();

  useEffect(() => {
    if (mode !== 'mqtts') return;
    (async () => {
      try {
        await MqttClient.connect('mqtts://test.mosquitto.org:8883', {
          autoReconnect: false,
          password: 'password',
          username: 'tablet',
        });
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
      } catch (err) {
        console.error('catch error: ', err);
      }
    })();
  }, [mode]);

  useEffect(() => {
    if (mode !== 'mqtt') return;
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
  }, [mode]);

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
