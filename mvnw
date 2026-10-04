#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper startup script for UN*X
# ----------------------------------------------------------------------------
set -e

DIR=$(cd "$(dirname "$0")" && pwd)
WRAPPER_JAR="$DIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
WRAPPER_URL="https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    mkdir -p "$DIR/.mvn/wrapper"
    echo "Downloading Maven Wrapper..."
    curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL"
fi

exec java -classpath "$WRAPPER_JAR" "-Dmaven.multiModuleProjectDirectory=$DIR" $WRAPPER_LAUNCHER "$@"
