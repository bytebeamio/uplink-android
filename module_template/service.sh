#!/system/bin/sh

MODULE_DIR=$(dirname "$0")
DATA_DIR=/data/local/uplink
export MODULE_DIR
export DATA_DIR
mkdir -p $DATA_DIR

cd $DATA_DIR || exit
for service in $MODULE_DIR/services/*; do
  if [ -x "$service" ]; then
    "$service"
  fi
done