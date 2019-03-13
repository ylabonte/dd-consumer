#!/bin/sh
set -e
if [ "$1" = "start" ]; then
    APP_VERSION=$(cat /app/build.version)
    APP_JAR="/app/ddc-${APP_VERSION}.jar"
    JAVA_ARGS="${JAVA_ARGS}"
    exec java -jar "${APP_JAR}" ${JAVA_ARGS}
fi
exec "$@"
