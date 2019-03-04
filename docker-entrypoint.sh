#!/bin/sh
set -e
if [ "$1" = "start" ]; then
    APP_VERSION=$(cat /root/build.version)
    APP_JAR="/root/ddc-${APP_VERSION}.jar"
    exec java -jar "${APP_JAR}"
fi
exec "$@"
