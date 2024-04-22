import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import { TinyEmitter } from 'tiny-emitter';
import uuid from 'react-native-uuid';
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
  protocol?: 'mqtt' | 'mqtts' | 'local' | 'ws' | 'wss';
}

export interface PublishOptions {
  retained?: boolean;
  qos?: number;
}

export enum ClientEvent {
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

export type MQTTEventHandler<T extends ClientEvent> =
  T extends ClientEvent.Connect
    ? ConnectEventHandler
    : T extends ClientEvent.Message
      ? MessageEventHandler
      : T extends ClientEvent.Disconnect
        ? DisconnectEventHandler
        : ErrorEventHandler;

class MqttClient {
  private readonly id: string;
  private emitter: TinyEmitter | null;

  private url: string = '';
  private connected: boolean = false;
  private closed: boolean = false;

  constructor() {
    this.emitter = new TinyEmitter();
    this.id = uuid.v4().toString();
    NativeMqtt.newClient(this.id);

    mqttEventEmitter.addListener(
      NativeEvent.CONNECT_EVENT,
      (event: { id: string; reconnect: boolean }) => {
        if (event.id !== this.id) {
          return;
        }

        this.connected = true;
        this.emitter?.emit(ClientEvent.Connect, event.reconnect);
      }
    );

    mqttEventEmitter.addListener(
      NativeEvent.MESSAGE_EVENT,
      (event: { id: string; topic: string; message: string }) => {
        if (event.id !== this.id) {
          return;
        }

        this.emitter?.emit(
          ClientEvent.Message,
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
        this.emitter?.emit(ClientEvent.Disconnect, event.cause);
      }
    );

    mqttEventEmitter.addListener(
      NativeEvent.ERROR_EVENT,
      (event: { id: string; error: string }) => {
        if (event.id !== this.id) {
          return;
        }

        this.emitter?.emit(ClientEvent.Error, event.error);
      }
    );
  }

  public connect(host: string, options: ConnectionOptions) {
    const urlMatch = host.split('://');
    let protocol = urlMatch?.[0];
    if (protocol === 'mqtt') {
      protocol = 'tcp';
    }
    if (protocol === 'mqtts') {
      protocol = 'ssl';
    }
    if (!protocol) {
      protocol = options.protocol ?? 'tcp';
    }
    const hostname = urlMatch?.[1] || host;
    const url = `${protocol}://${hostname}`;

    return new Promise((resolve, reject) => {
      this.url = url;
      if (this.closed) {
        reject(new Error('client already closed'));
      }

      if (this.connected) {
        console.error('client already connected');
        return;
        // reject(new Error('client already connected'))
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
          reject(new Error(err));
          return;
        }

        this.connected = true;
        resolve(true);
      });
    });
  }

  public subscribe(topic: string, qos: number = 0) {
    if (this.closed) {
      throw new Error('client already closed');
    }

    if (!this.connected) {
      throw new Error('client not connected');
    }
    NativeMqtt.subscribe(this.id, topic, qos);
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
    message: string,
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
      Buffer.from(message, 'utf-8').toString('base64'),
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

  public on<T extends ClientEvent>(
    name: T,
    handler: MQTTEventHandler<T>,
    context?: any
  ): void {
    if (this.closed) {
      throw new Error('client already closed');
    }

    this.emitter?.on(name, handler, context);
  }

  public once<T extends ClientEvent>(
    name: T,
    handler: MQTTEventHandler<T>,
    context?: any
  ): void {
    if (this.closed) {
      throw new Error('client already closed');
    }

    this.emitter?.once(name, handler, context);
  }

  public off<T extends ClientEvent>(
    name: T,
    handler?: MQTTEventHandler<T>
  ): void {
    if (this.closed) {
      throw new Error('client already closed');
    }

    this.emitter?.off(name, handler);
  }
}

export default new MqttClient();
