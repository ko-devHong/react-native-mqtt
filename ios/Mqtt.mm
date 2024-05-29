#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(Mqtt, NSObject)

RCT_EXTERN_METHOD(supportedEvents)

RCT_EXTERN_METHOD(newClient:(NSString *)id)

RCT_EXTERN_METHOD(connect:(NSString *)id host:(NSString *)host options:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback)

RCT_EXTERN_METHOD(subscribe:(NSString *)id topic:(NSString *)topic qos:(nonnull NSNumber *)qos)

RCT_EXTERN_METHOD(publish:(NSString *)id topic:(NSString *)topic base64Payload:(NSString *)base64Payload qos:(nonnull NSNumber *)qos retained:(BOOL *)retained)

RCT_EXTERN_METHOD(unsubscribe:(NSString *)id topicList:(NSArray)topicList)

RCT_EXTERN_METHOD(disconnect:(NSString *)id)

RCT_EXTERN_METHOD(close:(NSString *)id)

// https://facebook.github.io/react-native/docs/native-modules-ios.html#threading
// We'll run all methods (provisionally) in their own queue to prevent blocking.
- (dispatch_queue_t)methodQueue
{
    return dispatch_queue_create("com.taehong.rn.mqtt", DISPATCH_QUEUE_SERIAL);
}

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
