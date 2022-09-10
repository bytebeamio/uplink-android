# Uplink Android SDK

SDK for communicating with [uplink](https://github.com/bytebeamio/uplink) from android.

## Client SDK

The client sdk can be used by app developers to communicate with an uplink instance. It provides a type safe java api
for the app developers.

#### API

The `io.bytebeam.uplink.Uplink` class (from the `lib` module) provides the client side api for this sdk. Please
see the java docs and example app source code to understand how to use it.

#### Generate `.aar`

The client sdk can be exported as an aar package that can be easily loaded into an Android project by running the
following command:

```sh
./gradlew :lib:build
```
