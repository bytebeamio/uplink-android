#!/system/bin/sh

MODULE_DIR=$(dirname "$0")
DATA_DIR=/data/local/uplink

pkill -x uplink_watchdog
pkill -x uplink
mkdir -p $DATA_DIR

cd $DATA_DIR|| exit
("$MODULE_DIR"/bin/uplink -a $DATA_DIR/device.json -c "$MODULE_DIR"/etc/uplink.config.toml -vv 2>&1 | "$MODULE_DIR"/bin/logrotate --output $DATA_DIR/out.log) &
"$MODULE_DIR"/bin/uplink_watchdog >> $DATA_DIR/restart.log 2>&1