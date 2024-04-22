import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import { TinyEmitter } from 'tiny-emitter';
import uniqid from 'uniqid';
import { Buffer } from 'buffer';

const LINKING_ERROR =
  `The package 'react-native-mqtt' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const NativeMqtt = NativeModules.Mqtt
  ? NativeModules.Mqtt
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const mqttEventEmitter = new NativeEventEmitter(NativeMqtt);

export interface TlsOptions {
  caDer?: Buffer;
  cert?: string;
  key?: string;
  p12?: Buffer;
  pass?: string;
}

export interface ConnectionOptions {
  clientId?: string;
  cleanSession?: boolean;
  keepAlive?: number;
  timeout?: number;
  maxInFlightMessages?: number;
  autoReconnect?: boolean;
  username?: string;
  password?: string;
  tls?: TlsOptions;
  allowUntrustedCA?: boolean;
  enableSsl?: boolean;
}

export interface PublishOptions {
  retained?: boolean;
  qos?: number;
}

export enum Event {
  Connect = 'connect',
  Disconnect = 'disconnect',
  Message = 'message',
  Error = 'error',
}

enum NativeEvent {
  CONNECT_EVENT = 'connect',
  DISCONNECT_EVENT = 'disconnect',
  ERROR_EVENT = 'error',
  MESSAGE_EVENT = 'message',
}

export type ConnectEventHandler = (reconnect: boolean) => void;
export type MessageEventHandler = (topic: string, message: Buffer) => void;
export type DisconnectEventHandler = (cause: string) => void;
export type ErrorEventHandler = (error: string) => void;

export type MQTTEventHandler =
  | ConnectEventHandler
  | MessageEventHandler
  | DisconnectEventHandler
  | ErrorEventHandler;

class MqttClient {
  private readonly id: string;
  private emitter: TinyEmitter | null;

  private readonly url: string;
  private connected: boolean = false;
  private closed: boolean = false;

  constructor(url: string) {
    this.emitter = new TinyEmitter();
    this.id = uniqid();
    this.url = url;

    NativeMqtt.newClient(this.id);

    mqttEventEmitter.addListener(
      NativeEvent.CONNECT_EVENT,
      (event: { id: string; reconnect: boolean }) => {
        if (event.id !== this.id) {
          return;
        }

        this.connected = true;
        this.emitter?.emit(Event.Connect, event.reconnect);
      }
    );

    mqttEventEmitter.addListener(
      NativeEvent.MESSAGE_EVENT,
      (event: { id: string; topic: string; message: string }) => {
        if (event.id !== this.id) {
          return;
        }

        this.emitter?.emit(
          Event.Message,
          event.topic,
          Buffer.from(event.message, 'base64')
        );
      }
    );

    mqttEventEmitter.addListener(
      NativeEvent.DISCONNECT_EVENT,
      (event: { id: string; cause: string }) => {
        if (event.id !== this.id) {
          return;
        }

        this.connected = false;
        this.emitter?.emit(Event.Disconnect, event.cause);
      }
    );

    mqttEventEmitter.addListener(
      'rn-native-mqtt_error',
      (event: { id: string; error: string }) => {
        if (event.id !== this.id) {
          return;
        }

        this.emitter?.emit(Event.Error, event.error);
      }
    );
  }

  public connect(
    options: ConnectionOptions,
    callback: (error?: Error) => void
  ) {
    if (this.closed) {
      throw new Error('client already closed');
    }

    if (this.connected) {
      throw new Error('client already connected');
    }

    const opts: ConnectionOptions = Object.assign(
      {},
      { ...options, clientId: 'clientId' }
    );
    if (opts.tls && opts.tls.p12) {
      opts.tls = Object.assign({}, opts.tls);
      opts.tls.p12 = opts.tls.p12?.toString('base64') as any;
    }

    if (opts.tls && opts.tls.caDer) {
      opts.tls = Object.assign({}, opts.tls);
      opts.tls.caDer = opts.tls.caDer?.toString('base64') as any;
    }

    NativeMqtt.connect(this.id, this.url, opts, (err: string) => {
      if (err) {
        callback(new Error(err));
        return;
      }

      this.connected = true;
      callback();
    });
  }

  public subscribe(topics: string[], qos: number[]) {
    if (this.closed) {
      throw new Error('client already closed');
    }

    if (!this.connected) {
      throw new Error('client not connected');
    }

    NativeMqtt.subscribe(this.id, topics, qos);
  }

  public unsubscribe(topics: string[]) {
    if (this.closed) {
      throw new Error('client already closed');
    }

    if (!this.connected) {
      throw new Error('client not connected');
    }

    NativeMqtt.unsubscribe(this.id, topics);
  }

  public publish(
    topic: string,
    message: Buffer,
    qos: number = 0,
    retained: boolean = false
  ) {
    if (this.closed) {
      throw new Error('client already closed');
    }

    if (!this.connected) {
      throw new Error('client not connected');
    }

    NativeMqtt.publish(
      this.id,
      topic,
      message.toString('base64'),
      qos,
      retained
    );
  }

  public disconnect() {
    if (this.closed) {
      throw new Error('client already closed');
    }

    NativeMqtt.disconnect(this.id);
  }

  public close() {
    if (this.connected) {
      throw new Error('client not disconnected');
    }

    NativeMqtt.close(this.id);
    this.closed = true;
    this.emitter = null;
  }

  public on(name: Event, handler: MQTTEventHandler, context?: any): void {
    if (this.closed) {
      throw new Error('client already closed');
    }

    this.emitter?.on(name, handler, context);
  }

  public once(name: Event, handler: MQTTEventHandler, context?: any): void {
    if (this.closed) {
      throw new Error('client already closed');
    }

    this.emitter?.once(name, handler, context);
  }

  public off(name: Event, handler?: MQTTEventHandler): void {
    if (this.closed) {
      throw new Error('client already closed');
    }

    this.emitter?.off(name, handler);
  }
}

export default MqttClient;
