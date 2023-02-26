#!/system/bin/sh

$MODULE_DIR/bin/daemonize sh -c "$MODULE_DIR/bin/uplink_watchdog >> $DATA_DIR/restart.log 2>&1"
