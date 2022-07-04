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

The `io.bytebeam.uplink.Uplink` class (from the `lib` module) provides the client side api for this sdk. These are the
steps to use this library:

1. Use `Uplink.configuratorAvailable(Context)` to check if the configurator app is installed on the device.
2. Instantiate the `Uplink` class, providing an implementation of `UplinkStateCallback`. This will be used to
   notify the app about the state of the uplink bridge. This interface has two methods
    1. `onServiceReady()`: invoked when the service is ready to be used
    2. `onServiceNotConfigured()`: invoked if the service hasn't been configured yet. The user needs to select the
       authorization configuration using the configurator app.
3. Once the service is ready, you can use the methods on the `Uplink` instance to communicate with the backend:
    1. `Uplink.subscribe(ActionSubscriber)` - Subscribe to action targeting this device.
    2. `Uplink.sendData(UplinkPayload)` - Send some data to the backend.
    3. `Uplink.respondToAction(ActionResponse)` - Respond to an action that the device received from the backend.
       Each of these methods can throw an `UplinkTerminatedException` if the uplink service is terminated for some
       reason (the user stops it or the device configuration is changed).
       If that happens the clients need to wait for some time, go back to step 2, and attempt reconnecting to the
       service. The example app shows how to do that.
4. The application must properly dispose the `Uplink` instance when it is no longer needed using the `dispose` method.

#### Generate `.aar`

The client sdk can be exported as an aar package that can be easily loaded into an Android project by running the
following command:

```sh
./gradlew :lib:build
```
