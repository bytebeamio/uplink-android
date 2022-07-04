# Uplink Android SDK

SDK for using the [uplink](https://github.com/bytebeamio/uplink) bridge from android.

## Architecture

The project has two main modules:

### Configurator app

This app provides the uplink service that can be used to launch and communicate with the backend. Other apps can
bind to this service and use it to communicate with the selected backend. This app also provides the authorization
configuration for the uplink bridge, which you have to select using the application UI.

### Client SDK

The client sdk can be used by app developers to communicate with the uplink service. It handles all the low level
messaging details and provides a simple high level interface for the app developers. The configurator app must be
installed on the device before the client sdk can be used, otherwise an exception will be throws during initialization.
There is also an `boolean Uplink.configuratorAvailable(Context)` method.

#### API

The `io.bytebeam.uplink.Uplink` class (from the `lib` module) provides the client side api for this sdk. Creating an
instance of it will connect to the uplink service. The provided `UplinkReadyCallback` will be notified when the service
is ready to be used. The application must properly dispose the `Uplink` instance when it is no longer needed using
the `dispose` method. The example app shows how to do that.

The uplink class has the following methods that can be used for communicating with the backend:

1. `Uplink.subscribe(ActionSubscriber)` - Subscribe to action targeting this device.
2. `Uplink.sendData(UplinkPayload)` - Send some data to the backend.
3. `Uplink.respondToAction(ActionResponse)` - Respond to an action that the device received from the backend.

Note: There is also an example app in the `app` directory that demonstrates how to use the client sdk.

#### Generate `.aar`

The client sdk can be exported as an aar package that can be easily loaded into an Android project by running the
following command:

```sh
./gradlew :lib:build
```

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