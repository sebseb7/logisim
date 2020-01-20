#!/bin/bash

# This script is for building the final MacOS executable image. This is a
# comically complicated procedure, for many reasons:
# - Everything below Java 11 is becoming quickly obsolete and difficult to
#   support. Libraries and tools are disappearing, packages won't install, etc.
#   Solution: Use Java 11, at least for now.
# - Homebrew installs a broken version of AdoptOpenJDK-11 (as of Aug 13, 2018).
#   The jmod files included in the install contain the wrong sha256 hashes, so
#   jlink fails when attempting to build the modularized java-runtime.
#   Workaround: Install AdoptOpenJDK-11 by downloading the mac tar.gz directly
#   from github, copy into /Library/Java/JavaVirtualMachines/jdk-11.0.4+11,
#   fix the ownership and permissions, and hope for the best.
# - JDK-11 does not include javapackager, jpackager, jpackage, or any similar
#   tools. There appears to be no official way to package java applications as
#   of JDK 11, and there will not be until Java 13 at the earliest. Yes, this
#   seems incomprehensible that there could be no way to package a java
#   application for distribution any more.
#   There is an unofficial backported jpackager tool discussed here:
#   https://mail.openjdk.java.net/pipermail/openjfx-dev/2018-September/022500.html
#   It is also possible to install early access JDK 14 (or early access JDK 13),
#   which come with a new jpackage tool, which apparently can be used to package
#   files created with JDK 11. Both of these appear to be broken in various
#   ways: the backport can't deal with license files correctly, and neither sets
#   the permissions correctly on the installed files.
#   Workaround: Install early access JDK 14, and use that jpackage tool with
#   the JDK 11 runtime and build artifacts, combined with a postinstall script
#   to fix the permissions.
# - MacOS is becoming increasingly (and rapidly) more strict about running code
#   from unsigned/unofficial sources. It seems that the most recent versions of
#   MacOS may not be able to run unsigned sources at all, at least not without
#   extensive warnings and fiddling with security settings in likely unsafe
#   ways. It appears to no longer be tenable to ship unsigned code for MacOS.
#   Solution: Get developer keys and sign MacOS installers / executables.
# - Apple's University Developer Program claims to allow for obtaining
#   appropriate keys. However, the documentation for creating these keys is
#   vague, outdated, and contradictory, at best, and an extended back-and-forth
#   with our university account holder and with Apple yields no results. It
#   seems that the necessary signing keys are simply no longer possible to
#   generate within the University Developer Program.
#   Workaround: Use a paid, Individual Apple Developer account to obtain signing
#   keys.
# - My current AppleID is enrolled in our University Developer Program. Apple
#   confirms that each AppleID can be enrolled in at most one developer program.
#   Workaround: Create and use a separate AppleID for just Logisim Evolution.
#   Also work on an entirely fresh Mac hardware and account, since the AppleID
#   is engrained fairly thoroughly in MacOS.
# - jpackage (via Apple's pkgbuild/productbuild) does not set file permissions
#   (chmod) correctly on the installed application by default. jpackage does not
#   appear to have a way to pass custom arguments to pkgbuild/productbuild, and
#   it isn't clear there are any options that would help (the "preserve file
#   ownership" options might, but that doesn't say anything about preserving
#   file permissions).
#   Workaround: use a postinstall script to chmod all the files to reasonable
#   permissions.
# - On my rarely-used mac, jpackage from jdk-14-ea-16 seems to have entirely
#   disappeared, without a trace. Perhaps some auto update? The download links
#   on oracle's site for that package are dead/404. The original download
#   does not contain jpackage. The build scripts do not work without it.
#   Workaround: Download and install new jdk-14-ea-32 release, which seems to
#   have jpaackage again.
# - The new jpackage has renamed lots of options, so fix those below:
#    '--package-type' is now '--type'
#    '--output' is now '--dest'
#    '--mac-bundle-identifier' is now '--mac-package-identifier'
#    '--mac-bundle-name' is now '--mac-package-name'
#    '--identifier' isn ow '--mac-package-signing-prefix'
#    '--add-modules' and '--runtime-image' are now mutually exclusive, so
#       eliminate the former.
# - Code signing may *appear* to fail with the new jpackage with this error:
#   Running [codesign, --verify, /var/folders/kb/zswdtzg94bs52lqrts5sssdr0000gp/T/jdk.incubator.jpackage13637540241510693609/images/image-5430644754281798291/Logisim-Evolution.app/Contents/MacOS/libapplauncher.dylib]
#   /var/folders/kb/zswdtzg94bs52lqrts5sssdr0000gp/T/jdk.incubator.jpackage13637540241510693609/images/image-5430644754281798291/Logisim-Evolution.app/Contents/MacOS/libapplauncher.dylib: code object is not signed at all
#   In architecture: x86_64
#   Running [codesign, -s, Developer ID Application: Kevin Walsh (GDM3S3ULJA), --prefix, edu.holycross.cs.kwalsh.logisim, -vvvv, /var/folders/kb/zswdtzg94bs52lqrts5sssdr0000gp/T/jdk.incubator.jpackage13637540241510693609/images/image-5430644754281798291/Logisim-Evolution.app/Contents/MacOS/libapplauncher.dylib]
#   error: The specified item could not be found in the keychain.
#   java.io.IOException: Command [codesign, -s, Developer ID Application: Kevin Walsh (GDM3S3ULJA), --prefix, edu.holycross.cs.kwalsh.logisim, -vvvv, /var/folders/kb/zswdtzg94bs52lqrts5sssdr0000gp/T/jdk.incubator.jpackage13637540241510693609/images/image-5430644754281798291/Logisim-Evolution.app/Contents/MacOS/libapplauncher.dylib] exited with 1 code
#   	at jdk.incubator.jpackage/jdk.incubator.jpackage.internal.Executor.executeExpectSuccess(Executor.java:73)
#   	at jdk.incubator.jpackage/jdk.incubator.jpackage.internal.IOUtils.exec(IOUtils.java:179)
#   	at jdk.incubator.jpackage/jdk.incubator.jpackage.internal.IOUtils.exec(IOUtils.java:150)
#   	at jdk.incubator.jpackage/jdk.incubator.jpackage.internal.MacAppImageBuilder.lambda$signAppBundle$16(MacAppImageBuilder.java:804)
#   This, however, just means that the Developer ID Application and Developer ID Installer keys (not the certificates)
#   are missing fro mthe keychain. This can be fixed by going into XCode, preferences, Keys, and create a new one of each.

set -e # die on error
#set -x # debug output

# Using list-deps is recommended by one tutorial, but it seems to over-estimate
# the modules needed. Perhaps it (harmlessly)includes transitive dependencies?
# MODULES=`jdeps --list-deps logisim-evolution.jar | paste -d, -s`
# MODULES=java.base,java.datatransfer,java.desktop,java.logging,java.prefs,java.xml

# Using print-module-deps appears to be the correct way to get the dependencies.
echo "Detecting java module dependencies..."
DETECTED_MODULES=`jdeps --print-module-deps logisim-evolution.jar`
MODULES="java.base,java.desktop,java.logging,java.prefs"
echo "Detected java module dependencies: ${DETECTED_MODULES}"

JAVA_RUNTIME="java-runtime-mac"
  
if [ "${DETECTED_MODULES}" != "${MODULES}" ]; then
  echo "ERROR: This differs from expected!"
  echo "     : Module dependencies must have changed!"
  echo "     : Within build-packager.sh, set MODULES=\"${DETECTED_MODULES}\""
  echo "     : Then delete ./${JAVA_RUNTIME} and try running this again."
  exit 1
fi

if [ ! -e "${JAVA_RUNTIME}" ]; then
  echo "Building custom java runtime (using jlink)..."
  jlink --no-header-files --no-man-pages --compress=2 --strip-debug \
        --add-modules "${MODULES}" --output "${JAVA_RUNTIME}"
else
  echo "Using previously-built custom java runtime (from jlink)."
fi

INSTALLER_TYPE="pkg" # Options: dmg or pkg
OUTPUT="."
JAR="logisim-evolution.jar"
VERSION="4.0.4" # must be numerical x.y.z
FILE_ASSOCIATIONS="file-associations.properties"
APP_ICON="logisim.icns"
JAVA_APP_IDENTIFIER="edu.holycross.cs.kwalsh.logisim"

# Prepare input files
echo "Preparing input files..."
rm -rf mac-staging
mkdir -p mac-staging
cp LICENSE "${JAR}" mac-staging/

# Prepare installer customizations: background image, postinstall script
echo "Preparing installer customizations..."
rm -rf mac-resources
mkdir -p mac-resources
cp mac-installer-background.png mac-resources/Logisim-Evolution-background.png

cat <<END >> mac-resources/postinstall
#!/bin/bash
chmod -R go+rX INSTALL_LOCATION/Logisim-Evolution.app
END
chmod a+rx mac-resources/postinstall

# Build the app and installer package
echo "Building ${INSTALLER_TYPE} ..."
PACKAGER=/Library/Java/JavaVirtualMachines/jdk-14.jdk/Contents/Home/bin/jpackage
#PACKAGER=/Library/Java/JavaVirtualMachines/jdk-15-ea-6.jdk/Contents/Home/bin/jpackage
${PACKAGER} \
  --type ${INSTALLER_TYPE} \
  --input mac-staging \
  --dest "${OUTPUT}" \
  --name "Logisim-Evolution" \
  --main-class com.cburch.logisim.Main \
  --main-jar "${JAR}" \
  --app-version "${VERSION}" \
  --copyright "(c) 2019 Kevin Walsh" \
  --description "Digital logic designer and simulator." \
  --vendor "Kevin Walsh" \
  --runtime-image "${JAVA_RUNTIME}" \
  --icon "${APP_ICON}" \
  --mac-package-identifier "Logisim-Evolution-HC" \
  --mac-package-name "Logisim HC" \
  --mac-sign \
  --file-associations "${FILE_ASSOCIATIONS}" \
  --mac-package-signing-prefix "${JAVA_APP_IDENTIFIER}" \
  --resource-dir mac-resources \
  --license-file LICENSE \
  --verbose
# --add-modules "${MODULES}"

rm -rf mac-staging
rm -rf mac-resources
mv "Logisim-Evolution-${VERSION}.pkg" "Logisim-Evolution-${VERSION}-HC.pkg"

cat <<END

# To notarize, run this command:

ALTOOLPW=enter-app-specific-password-here
xcrun altool --notarize-app --primary-bundle-id "$JAVA_APP_IDENTIFIER" \
    --username "kwalsh@holycross.edu" --password "\$ALTOOLPW" \
    --file Logisim-Evolution-${VERSION}-HC.pkg

# Then later, try:
xcrun altool --notarization-history 0 -u "kwalsh@holycross.edu" -p "\$ALTOOLPW"

# And if that works, then try:
UUID=whatever-from-previous-command
xcrun altool --notarization-info \$UUID -u "kwalsh@holycross.edu" -p "\$ALTOOLPW"

# And if that works, then try:
URL=copied-from-last-output
curl "\$URL" > mac-notarize.log
cat mac-notarize.log

# Check for warnings and errors, then finally:
xcrun stapler staple Logisim-Evolution-${VERSION}-HC.pkg

