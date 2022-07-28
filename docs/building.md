# Building configurator app
* Clone `https://github.com/bytebeamio/uplink.git` on your machine. (TODO: switch to git submodules)
* Add an entry to your `local.properties` pointing to this repository:
```shell
sdk.dir=/home/user/Android/Sdk
uplink.dir=../uplink
```
* Run `./gradlew :configurator:assembleDebug` to build a debug apk

# Building a signed version of the configurator app
* Create a java key store
* Create a key named `bbkey` in this store
* Add following properties to your `local.properties`:
```shell
RELEASE_STORE_FILE=../keystore.jks
RELEASE_STORE_PASSWORD=********
RELEASE_KEY_ALIAS=bbkey
RELEASE_KEY_PASSWORD=********
```
* `./gradlew :configurator:assembleRelease`

# Building `uplink.aar`
`./gradlew :uplink:assembleRelease`