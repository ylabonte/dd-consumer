#!/bin/sh
set -e
. /etc/environment
if [ "$1" = "start" ]; then
    APP_JAR="/app/ddc-${BUILD_VERSION}.jar"
    JAVA_ARGS="${JAVA_ARGS}"
    exec java -jar "${APP_JAR}" ${JAVA_ARGS}
fi
exec "$@"
