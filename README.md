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

> **Demo**: To demonstrate the capabilities of the Android library, we have included an example program in the `demo` branch of this git repository.

### External Dependency
1. [flapigen-rs](https://github.com/Dushistov/flapigen-rs)
2. See [`Cargo.toml`](./Cargo.toml)
