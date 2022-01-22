# uplink android
This is an android library, the aim of the project is to create a JNI binding on top of the `uplink` rust library to enable developers building apps for the Android platform to simply import and use it, without having to deal with the complexity of build systems, JNI, etc.

### Requirements
1. [JDK and Android SDK](https://developer.android.com/studio/install)
2. [gradle 7.0.2](https://gradle.org/install/)
3. cargo and [cross](https://crates.io/crates/cross)

### Generate `.aar`
The library can be exported as an archived package that can be easily loaded into an Android project by running the following command:
```sh
./gradlew build
```

## How to use `uplink-android` in your app?
1. Build the `uplink-release.aar` file and copy it into their app's `src/main/libs` folder.
2. One can import the library in their own app by adding the following line to their app's `build.gradle` file:
```gradle
dependencies {
    implementation file('path/to/src/main/libs/uplink-release.aar')
}
```
3. Create an uplink object inside the appropriate `*Activity.java` file:
```java
import io.bytebeam.uplink.Uplink;

class Foo {
    private Uplink uplink;
    ...
}
```
4. Configure and start the uplink instance where appropriate, an example is [included here](https://github.com/bytebeamio/uplink/blob/main/example/dummy.JSON):
```java
String config = ".."; // A string containing JSON formatted uplink config.
try {
    uplink = new Uplink(config);
} catch (Exception e) {
    ...
}
```
5. Once configured and connected to a broker, you can send data by using the `Payload` format as [described here](https://github.com/bytebeamio/uplink/blob/main/docs/apps.md#streamed-data):
```java
String data = ".."; // A string containing data that is JSON formatted Payload.
try {
    uplink.send(data);
} catch (Exception e) {
    ...
}
```
6. For your application to be able to recieve an [`Action`](https://github.com/bytebeamio/uplink/blob/main/docs/apps.md#action) through the uplink instance(received through MQTT), you should pass an object that implements the `ActionCallback` interface to the `subscribe()` method as such:
```java
class FooRecv implements ActionCallback {
    ...
    @Override
    public void recvdAction(String action) {
        ... // action contains a JSON formatted Action and can be used by your app to execute operations. See uplink application docs.
    }
}

// Inside Foo class
FooRecv recv = ...; // Considered to be properly initialized
try {
    uplink.subscribe(recv);
} catch (Exception e) {
    ...
}
```
> **NOTE**: The Foo class itself can also be written such that it implements `ActionCallback` and hence you can subsribe by passing a reference to itself by using the `this` keyword as is demonstrated within the demo app, [here in `MainActivity.java`](demo/src/main/java/io/bytebeam/demo/MainActivity.java#L116).

You could respond to `Action`s by sending [`ActionResponse`s](https://github.com/bytebeamio/uplink/blob/main/docs/apps.md#action-response) which are also to be sent as JSON formatted strings:
```java
String response = ".."; // A string containing an ActionResponse in JSON format.
try {
    uplink.respond(response);
} catch (Exception e) {
    ...
}
```

## How to run the demo applicaiton?
You can compile and run the demo application included in this repo, on an emulator or personal developer device of your choice, by using the build and launch mechanism provided within Android Studio.

### External Dependency
1. [flapigen-rs](https://github.com/Dushistov/flapigen-rs)
2. See [`Cargo.toml`](./Cargo.toml)
