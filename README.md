# uplink android
This is an android library, the aim of the project is to create a JNI binding on top of the `uplink` rust library to enable developers building apps for the Android platform to simply import and use it, without having to deal with the complexity of build systems, JNI, etc.

### Requirements
1. [JDK and Android SDK](https://developer.android.com/studio/install)
2. [gradle 7.0.2](https://gradle.org/install/)
3. cargo and [cross](https://crates.io/crates/cross)

### Generate `.aar`
The library can be exported as an archived package that can be easily loaded into an Android project by running the following command:
```sh
./gradlew :lib:build
```

## How to use `uplink-android` in your app?
1. Build the `uplink-release.aar` file and copy it into their app's `src/main/libs` folder.
2. One can import the library in their own app by adding the following line to their app's `build.gradle` file:
```gradle
dependencies {
    implementation files('path/to/src/main/libs/uplink-release.aar')
}
```
3. There is an example app in the `app` directory that demonstrates how to use the sdk.

## API

The `io.bytebeam.uplink.Uplink` class provides the client side api for this sdk. Creating an instance of it will
launch the uplink service. The provided `UplinkReadyCallback` will be notified when the service is ready to be
used. The application must properly dispose the `Uplink` instance when it is no longer needed using the `dispose`
method. The example app shows how to do that.

The uplink class has the following methods that can be used for communicating with the backend:

1. `Uplink::subscribe(ActionSubscriber)` - Subscribe to action targeting this device.
2. `Uplink::sendData(UplinkPayload)` - Send some data to the backend.
3. `Uplink::respondToAction(ActionResponse)` - Respond to an action that the device received from the backend.

## Running the example app

You'll need an MQTT broker to run the example app. The easiest way to get one is to use [mosquitto](https://mosquitto.org/).
On an ubuntu machine, you'll need to install `mosquitto` and `mosquitto-client` packages.

1. Start the `mosquitto` server:
```sh
mosquitto -p 8833 -v
```

2. Subscribe to battery notifications:
```sh
mosquitto_sub -p 8833 -t /tenants/demo/devices/1234/events/battery_level -v
```

3. Start the example app in an emulator. You can run it on a physical device as well, but they'll need to be on the 
same network and the firewall will have to be configured properly.

4. You'll start to see the battery notifications in the `mosquitto_sub` console.

5. Subscribe for action responses:
```sh
mosquitto_sub -p 8833 -t /tenants/demo/devices/1234/action/status -v
```

6. Send an action to the backend:
```sh
mosquitto_pub -p 8833 -t /tenants/demo/devices/1234/actions -m '{"action_id": "1", "kind": "test", "name": "test", "payload": "data"}'
```

7. You'll start to see the action responses in the `mosquitto_sub` console (10 for each action dispatched).