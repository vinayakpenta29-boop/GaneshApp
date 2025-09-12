#!/usr/bin/env sh
##############################################################################
#
#  Gradle start up script for UN*X
#
##############################################################################

# Determine the location of the gradle-wrapper.jar
WRAPPER_JAR="$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar"

# Check if gradle-wrapper.jar exists
if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Gradle wrapper JAR not found at $WRAPPER_JAR"
  exit 1
fi

# Execute the Gradle wrapper
exec java -jar "$WRAPPER_JAR" "$@"
