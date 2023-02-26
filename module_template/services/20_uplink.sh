#!/system/bin/sh

$MODULE_DIR/bin/daemonize sh -c\
 "$MODULE_DIR/bin/uplink -a $DATA_DIR/device.json -c $MODULE_DIR/etc/uplink.config.toml -vv 2>&1 | $MODULE_DIR/bin/logrotate --max-size 30000000 --backup-files-count 10 --output $DATA_DIR/out.log"