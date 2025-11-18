#!/usr/bin/env sh

##############################################################################
## Gradle start up script for UN*X
##############################################################################

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Set local java opts
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

CLASSPATH=`dirname "$0"`/gradle/wrapper/gradle-wrapper.jar

# Determine Java command to use
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/bin/java" ] ; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        JAVACMD="$JAVA_HOME/jre/bin/java"
    fi
else
    JAVACMD="java"
fi

# OS specific support
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  MINGW*) mingw=true ;;
  Darwin*) darwin=true ;;
  *) ;;
esac

# For Cygwin, switch paths to Windows format before running java
if [ "$cygwin" = true ]; then
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    JAVACMD=`cygpath --unix "$JAVACMD"`
fi

exec "$JAVACMD" ${DEFAULT_JVM_OPTS} -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
