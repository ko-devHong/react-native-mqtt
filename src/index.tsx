import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-mqtt' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const Mqtt = NativeModules['React-native-mqtt']
  ? NativeModules['React-native-mqtt']
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export type EventName =
  | 'connect'
  | 'reconnect'
  | 'error'
  | 'publish'
  | 'message';

interface IRNMqtt {
  connect: (brokerUrl: string, protocol: 'mqtt' | 'mqtts') => Promise<string>;
  on: (eventName: EventName, callBack: (_message: string) => void) => void;
}

const RNMqtt: IRNMqtt = {
  connect: async (brokerUrl, protocol) => {
    return await Mqtt.connect(brokerUrl, protocol);
  },
  on: (eventName, callBack) => {
    Mqtt.on(eventName, callBack);
  },
};

export default RNMqtt;
