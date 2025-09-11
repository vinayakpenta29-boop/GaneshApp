#!/bin/sh
# ----------------------------------------------------------------------------
# Gradle start up script for UN*X
# ----------------------------------------------------------------------------
# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Resolve links - $0 may be a link
PRG="$0"

while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Now set the runtime options
JAVA_OPTS="$DEFAULT_JVM_OPTS $JAVA_OPTS"

exec java $JAVA_OPTS -classpath "$PRGDIR/gradle-launcher.jar" org.gradle.launcher.GradleMain "$@"
