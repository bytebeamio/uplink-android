#!/system/bin/sh

set -x

MODULE_DIR="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
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