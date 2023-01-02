#!/system/bin/sh

MODULE_DIR=/data/local/uplinkmodule
DATA_DIR=/data/local/uplink

cd $DATA_DIR|| exit
("$MODULE_DIR"/bin/uplink -a $DATA_DIR/device.json -c "$MODULE_DIR"/etc/uplink.config.toml -v 2>&1 | "$MODULE_DIR"/bin/logrotate $DATA_DIR/out.log) &
