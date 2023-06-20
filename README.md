# Uplink Android SDK

SDK for setting up and communicating with [uplink](https://github.com/bytebeamio/uplink) from android.

This project consists of two parts.

## Uplink android module

It's distributed as a tarball for the 4 major android architectures (arm/x86 X 32bit/64bit). This module is a valid
magisk module so it can be just extracted to `/data/adb/modules/uplink` if you're using magisk as your root manager and
it will start uplink at boot. You can also start uplink manually like this:

```
$MODULE/bin/daemonize $MODULE/service.sh
```

This script will stop the current uplink instance if it's running and then start it.

The following are the configuration points for this module:

* `$MODULE/etc/uplink.config.toml` : uplink configuration file
* `$MODULE/services/` : If you want to run some additional services, put the boot script in this directory
* `env.sh` : Defines the environment variables for this module. One of those is `DATA_DIR` where all the uplink runtime data lives. By default it points to `/data/local/uplink`

## Android library

It's distributed as an aar file on the github releases page. It can be included in you android project like this:

```gradle
    implementation files('libs/uplink.aar')
```

The main entry point of this library is the `io.bytebeam.uplink.Uplink` class. Look at the javadoc of this class for usage notes.
