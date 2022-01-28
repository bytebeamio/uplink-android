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
3. Create an uplink object inside the appropriate `MainActivity.java` file:
```java
import io.bytebeam.uplink.Uplink;

class MainActivity extends AppCompatActivity {
    private Uplink uplink;
    ...
}
```
4. Configure and start the uplink instance where appropriate, an example is [included here](https://github.com/bytebeamio/uplink/blob/main/example/dummy.JSON):
```java
// A string containing JSON formatted uplink config.
String configFile = "{\n" +
    "   \"project_id\": \"abc\",\n" +
    "   \"broker\": \"broker.example.com\",\n" +
    "   \"port\": 8883,\n" +
    "   \"device_id\": \"123\",\n" +
    "   \"authentication\": {\n" +
    "       \"ca_certificate\": \"-----BEGIN CERTIFICATE----------END CERTIFICATE-----\\n\",\n" +
    "       \"device_certificate\": \"-----BEGIN CERTIFICATE----------END CERTIFICATE-----\\n\",\n" +
    "       \"device_private_key\": \"-----BEGIN RSA PRIVATE KEY----------END RSA PRIVATE KEY-----\\n\"\n" +
    "   }\n" +
    "}";
// Get base folder path for application
String baseFolder = getBaseContext().getExternalFilesDir("").getPath();

try {
    ConfigBuilder config = new ConfigBuilder(configFile)
                        .setOta(true, baseFolder + "/ota-file")
                        .setPersistence(baseFolder + "/uplink", 104857600, 3);
    uplink = new Uplink(config.build());
} catch (Exception e) {
    ...
}
```
5. Once configured and connected to a broker, you can send data by using the `Payload` format as [described here](https://github.com/bytebeamio/uplink/blob/main/docs/apps.md#streamed-data):
```java
try {
    JSONObject data = new JSONObject(); // JSON object that carries data to be sent to stream_name
    data.put("field", "value");
    UplinkPayload payload = new UplinkPayload("stream_name", timestamp, sequence, String.valueof(data));
    uplink.send(data);
} catch (Exception e) {
    ...
}
```
6. For your application to be able to recieve an [`Action`](https://github.com/bytebeamio/uplink/blob/main/docs/apps.md#action) through the uplink instance(received through MQTT), you should pass an object that implements the `ActionCallback` interface to the `subscribe()` method as such:
```java
class ActionRecvr implements ActionCallback {
    ...
    @Override
    public void recvdAction(UplinkAction action) {
        // action contains information that can be used by your app to execute operations. See uplink application docs for more info.
        String id = action.getId();
        String payload = action.getPayload();
    }
}

// Inside MainActivity class
ActionRecvr recv; // Considered to be properly initialized
try {
    uplink.subscribe(recv);
} catch (Exception e) {
    ...
}
```
> **NOTE**: The Foo class itself can also be written such that it implements `ActionCallback` and hence you can subsribe by passing a reference to itself by using the `this` keyword as is demonstrated within the demo app, [here in `MainActivity.java`](demo/src/main/java/io/bytebeam/demo/MainActivity.java#L119).

You could respond to `Action`s by sending [`ActionResponse`s](https://github.com/bytebeamio/uplink/blob/main/docs/apps.md#action-response) which can be created using the `ActionResponse` class:
```java
// A response to indicate action's progress
ActionResponse response = new ActionResponse(id, "Running", 10);
// A response to indicate action's successful completion
response = ActionResponse.success(id);
// A response to indicate action's failed completion, along with error
Exception e1 = new Exception("Error: 1");
response = ActionResponse.failure(id, e1.toString());
// You can carry multiple errors in a single response
Exception e2 = new Exception("Error: 2");
respose = ActionResponse.add_error(response, e2.toString());
try {
    uplink.respond(response);
} catch (Exception e) {
    ...
}
```

## How to run the demo applicaiton?
You can compile and run the demo application included in this repo, on an emulator or personal developer device of your choice, by using the build and launch mechanism provided within Android Studio.
