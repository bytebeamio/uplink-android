The configurator app data directory will look like this:

* $FILES/exe - uplink executable for the right architecture
* $FILES/exe_logs.db - logs of the current/last run of the uplink executable, to be consumed by the UI code
* $FILES/*.json - device auth config selected my the user
* $FILES/config.toml - uplink configuration
* $FILES/persistence - uplink persistence dir
* $FILES/ota - uplink ota dir