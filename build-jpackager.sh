#!/bin/bash

set -e

# MODULES=`jdeps --list-deps logisim-evolution.jar | paste -d, -s`

MODULES=java.base,java.datatransfer,java.desktop,java.logging,java.prefs,java.xml
JAVA_RUNTIME=java-runtime

if [ ! -e "${JAVA_RUNTIME}" ]; then
  echo "Building custom java runtime (using jlink)..."
  jlink --no-header-files --no-man-pages --compress=2 --strip-debug \
        --add-modules "${MODULES}" --output "${JAVA_RUNTIME}"
else
  echo "Using previously-built custom java runtime (from jlink)."
fi

PACKAGER="/Library/Java/JPackager/jpackager"
INSTALLER_TYPE="dmg" # msi, rpm, deb, dmg, pkg
MODULE_PATH="libs" # ?
INPUT="." # ?
OUTPUT="bin/bundle" # ?
JAR="logisim-evolution.jar"
VERSION="4.0.0"
FILE_ASSOCIATIONS="file-associations.properties"
APP_ICON="logisim.icns"
EXTRA_BUNDLER_ARGUMENTS="--mac-sign"

echo "Building ${INSTALLER_TYPE} package..."
${PACKAGER} \
  create-installer ${INSTALLER_TYPE} \
  --verbose \
  --echo-mode \
  --module-path ${MODULE_PATH} \
  --add-modules "${MODULES}" \
  --input "${INPUT}" \
  --output "${OUTPUT}" \
  --name Santulator \
  --main-jar ${JAR} \
  --version ${VERSION} \
  --file-associations "${FILE_ASSOCIATIONS}" \
  --runtime-image "${JAVA_RUNTIME}" \
  --icon $APP_ICON \
  $EXTRA_BUNDLER_ARGUMENTS \
  --class com.cburch.logisim.Main
