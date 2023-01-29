#!/usr/bin/env bash

# Check JAVA_HOME
if [ "${JAVA_HOME}" = "" ]; then
  echo "Please ensure environment variable contain JAVA_HOME."
  exit 1
fi

java="${JAVA_HOME}/bin/java"

# Check java version
version=$("$java" -version 2>&1 | awk -F '"' '/version/ {print $2}')

# shellcheck disable=SC2071
if [ "$version" -lt "17" ]; then
  # shellcheck disable=SC1072
  # shellcheck disable=SC2091
  # shellcheck disable=SC2116
  echo "Java version must >=17"
  exit 1
fi

PROGRAM_NAME=nes4j

# shellcheck disable=SC2091
$(jpackage -n "$PROGRAM_NAME" -p "$1" -m cn.navclub.nes4j.app/cn.navclub.nes4j.app.Launcher --icon icon/nes4j.png --license-file ../LICENSE --linux-package-name "$PROGRAM_NAME")
